package pl.bkacala.fiszkide.data

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlashcardRepositoryTest {
    private fun withRepository(count: Int = 25, test: suspend (FlashcardRepository, String) -> Unit) = runBlocking {
        val file = Files.createTempFile("fiszki-test-", ".db")
        try {
            val repo = FlashcardRepository(file.toString()) {
                "deutsch;polski\n" + (1..count).joinToString("\n") { "de$it;pl$it" }
            }
            repo.initialize()
            test(repo, file.toString())
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun initialSessionUsesFirstTwentyAndPrioritizesHighestStreak() = withRepository { repo, _ ->
        assertEquals((1L..20L).toList(), repo.nextSession().map { it.id })
        repeat(3) { repo.answer(1, true) }
        repeat(2) { repo.answer(2, true) }
        repeat(4) { repo.answer(3, true) }
        assertEquals(listOf(3L, 1L, 2L, 4L, 5L), repo.nextSession().take(5).map { it.id })
        repo.answer(3, false)
        assertEquals(listOf(1L, 2L, 3L), repo.nextSession().take(3).map { it.id })
        assertEquals(0, repo.nextSession().first { it.id == 3L }.streak)
    }

    @Test
    fun learnedCardsDisappearAndAreReplacedNextSession() = withRepository { repo, _ ->
        repeat(5) { repo.answer(1, true) }
        assertEquals((2L..21L).toList(), repo.nextSession().map { it.id })
        assertEquals(1, repo.progress().learned)
        // A stale answer cannot undo mastery.
        repo.answer(1, false)
        assertEquals(1, repo.progress().learned)
    }

    @Test
    fun reopeningDoesNotImportAgainAndPreservesAnswers() = withRepository { repo, file ->
        repo.answer(2, true)
        val reopened = FlashcardRepository(file) { error("CSV should not be read twice") }
        assertEquals(25, reopened.initialize().total)
        assertEquals(2L, reopened.nextSession().first().id)
        assertEquals(1, reopened.nextSession().first().streak)
    }

    @Test
    fun shortFinalSessionAndCompletion() = withRepository(3) { repo, _ ->
        assertEquals(3, repo.nextSession().size)
        assertFalse(repo.progress().complete)
        for (id in 1L..3L) repeat(5) { repo.answer(id, true) }
        assertTrue(repo.nextSession().isEmpty())
        assertTrue(repo.progress().complete)
        assertEquals(1f, repo.progress().fraction)
    }

    @Test
    fun malformedImportCanBeRetriedWithoutPartialData() = runBlocking {
        val file = Files.createTempFile("fiszki-import-", ".db")
        try {
            val broken = FlashcardRepository(file.toString()) { "deutsch;polski\nHallo;Cześć\nkaputt;" }
            assertFailsWith<IllegalArgumentException> { broken.initialize() }
            val repaired = FlashcardRepository(file.toString()) { "deutsch;polski\nHallo;Cześć" }
            assertEquals(1, repaired.initialize().total)
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
