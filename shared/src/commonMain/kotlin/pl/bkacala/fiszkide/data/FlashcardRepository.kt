package pl.bkacala.fiszkide.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

const val MASTERY_STREAK = 5
const val SESSION_SIZE = 20

data class Flashcard(val id: Long, val german: String, val polish: String, val streak: Int)
data class Progress(val total: Int = 0, val learned: Int = 0, val learning: Int = 0) {
    val remaining: Int get() = total - learned
    val complete: Boolean get() = total > 0 && learned == total
    val fraction: Float get() = if (total == 0) 0f else learned.toFloat() / total
}

class FlashcardRepository(
    private val databasePath: String,
    private val readVocabulary: suspend () -> String,
) {
    private val mutex = Mutex()

    suspend fun initialize(): Progress = withContext(Dispatchers.Default) {
        mutex.withLock {
            withDatabase { db ->
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS flashcards (
                        id INTEGER PRIMARY KEY,
                        german TEXT NOT NULL,
                        polish TEXT NOT NULL,
                        correct_streak INTEGER NOT NULL DEFAULT 0 CHECK(correct_streak BETWEEN 0 AND 5)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS session_order ON flashcards(correct_streak DESC, id ASC)")
                val existing = db.progress()
                if (existing.total == 0) {
                    // Validate the whole input before inserting anything. Import is all-or-nothing.
                    val entries = VocabularyCsv.parse(readVocabulary())
                    db.transaction {
                        db.prepare("INSERT INTO flashcards(id, german, polish) VALUES (?, ?, ?)").use { stmt ->
                            entries.forEachIndexed { index, entry ->
                                stmt.bindLong(1, index.toLong() + 1)
                                stmt.bindText(2, entry.german)
                                stmt.bindText(3, entry.polish)
                                stmt.step()
                                stmt.reset()
                            }
                        }
                    }
                }
                db.progress()
            }
        }
    }

    suspend fun nextSession(): List<Flashcard> = access { db ->
        db.prepare("""
            SELECT id, german, polish, correct_streak FROM flashcards
            WHERE correct_streak < 5 ORDER BY correct_streak DESC, id ASC LIMIT 20
        """.trimIndent()).use { stmt ->
            buildList {
                while (stmt.step()) add(Flashcard(stmt.getLong(0), stmt.getText(1), stmt.getText(2), stmt.getInt(3)))
            }
        }
    }

    suspend fun progress(): Progress = access { it.progress() }

    suspend fun answer(id: Long, remembered: Boolean): Progress = access { db ->
        db.transaction {
            db.prepare("""
                UPDATE flashcards SET correct_streak =
                CASE WHEN ? = 1 THEN MIN(correct_streak + 1, 5) ELSE 0 END
                WHERE id = ? AND correct_streak < 5
            """.trimIndent()).use { stmt ->
                stmt.bindInt(1, if (remembered) 1 else 0)
                stmt.bindLong(2, id)
                stmt.step()
            }
            db.progress()
        }
    }

    private suspend fun <T> access(block: (SQLiteConnection) -> T): T = withContext(Dispatchers.Default) {
        mutex.withLock { withDatabase(block) }
    }

    private inline fun <T> withDatabase(block: (SQLiteConnection) -> T): T {
        val db = BundledSQLiteDriver().open(databasePath)
        return try {
            db.execSQL("PRAGMA busy_timeout = 5000")
            block(db)
        } finally {
            db.close()
        }
    }

    private fun SQLiteConnection.progress(): Progress = prepare("""
        SELECT COUNT(*), COALESCE(SUM(correct_streak >= 5), 0),
        COALESCE(SUM(correct_streak > 0 AND correct_streak < 5), 0) FROM flashcards
    """.trimIndent()).use { stmt ->
        check(stmt.step())
        Progress(stmt.getInt(0), stmt.getInt(1), stmt.getInt(2))
    }

    private inline fun <T> SQLiteConnection.transaction(block: () -> T): T {
        execSQL("BEGIN IMMEDIATE TRANSACTION")
        try {
            val result = block()
            execSQL("COMMIT")
            return result
        } catch (error: Throwable) {
            runCatching { execSQL("ROLLBACK") }
            throw error
        }
    }
}
