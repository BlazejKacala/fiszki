package pl.bkacala.fiszkide

import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pl.bkacala.fiszkide.data.FlashcardRepository
import pl.bkacala.fiszkide.data.VocabularyCsv
import pl.bkacala.fiszkide.resources.Res

@OptIn(ExperimentalCoroutinesApi::class)
class StudyControllerTest {
    @Test
    fun sessionFlipsResetsAndReturnsHomeAfterTwentyAnswers() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val file = Files.createTempFile("fiszki-session-", ".db")
        val repo = FlashcardRepository(file.toString()) {
            "deutsch;polski\n" + (1..25).joinToString("\n") { "de$it;pl$it" }
        }
        val controller = StudyController(repo)
        try {
            controller.state.first { it.ready && !it.busy }
            controller.startSession()
            controller.state.first { it.session != null && !it.busy }
            controller.flip()
            assertTrue(controller.state.value.session!!.revealed)
            for (id in 1L..20L) {
                controller.answer(id, remembered = id != 2L)
                // A second tap for the old card must not record another answer.
                controller.answer(id, remembered = true)
                controller.state.first { !it.busy }
                if (id < 20) {
                    assertEquals(id + 1, controller.state.value.session!!.current.id)
                    assertFalse(controller.state.value.session!!.revealed)
                }
            }
            assertNull(controller.state.value.session)
            assertEquals(19, controller.state.value.progress.learning)
            assertEquals(0, controller.state.value.progress.learned)
            assertTrue(controller.state.value.message!!.contains("19 z 20"))
            val next = repo.nextSession()
            assertEquals(1, next.first().streak)
            assertEquals(0, next.first { it.id == 2L }.streak)
            controller.startSession()
            controller.state.first { it.session != null && !it.busy }
            controller.endSession()
            assertNull(controller.state.value.session)
            assertEquals(19, repo.progress().learning)
        } finally {
            controller.close()
            Dispatchers.resetMain()
            Files.deleteIfExists(file)
        }
    }

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @Test
    fun bundledVocabularyIsReadableAndComplete() = runTest {
        val entries = VocabularyCsv.parse(Res.readBytes("files/slowka.csv").decodeToString())
        assertEquals(3390, entries.size)
        assertEquals("der Zoll", entries.first().german)
        assertEquals("cło", entries.first().polish)
    }
}
