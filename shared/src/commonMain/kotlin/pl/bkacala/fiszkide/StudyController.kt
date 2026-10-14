package pl.bkacala.fiszkide

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.bkacala.fiszkide.data.Flashcard
import pl.bkacala.fiszkide.data.FlashcardRepository
import pl.bkacala.fiszkide.data.Progress
import pl.bkacala.fiszkide.resources.Res

data class Session(
    val cards: List<Flashcard>,
    val index: Int = 0,
    val revealed: Boolean = false,
    val correct: Int = 0,
) {
    val current: Flashcard get() = cards[index]
}

data class StudyState(
    val progress: Progress = Progress(),
    val ready: Boolean = false,
    val busy: Boolean = false,
    val session: Session? = null,
    val message: String? = null,
    val error: String? = null,
)

class StudyController(private val repository: FlashcardRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(StudyState())
    val state = mutableState.asStateFlow()

    init { initialize() }

    fun initialize() = perform {
        val progress = repository.initialize()
        mutableState.value = StudyState(progress = progress, ready = true)
    }

    fun startSession() {
        if (!state.value.ready || state.value.progress.complete || state.value.session != null) return
        perform {
            val cards = repository.nextSession()
            mutableState.value = state.value.copy(
                session = cards.takeIf { it.isNotEmpty() }?.let { Session(it) },
                progress = repository.progress(), message = null,
            )
        }
    }

    fun flip() {
        if (state.value.busy) return
        val session = state.value.session ?: return
        mutableState.value = state.value.copy(session = session.copy(revealed = !session.revealed))
    }

    fun answer(cardId: Long, remembered: Boolean) {
        val session = state.value.session ?: return
        // Ignore stale clicks from the previous card as well as clicks during a write.
        if (session.current.id != cardId || state.value.busy) return
        perform {
            val progress = repository.answer(cardId, remembered)
            val correct = session.correct + if (remembered) 1 else 0
            val finished = session.index == session.cards.lastIndex
            mutableState.value = state.value.copy(
                progress = progress,
                session = if (finished) null else session.copy(
                    index = session.index + 1, revealed = false, correct = correct,
                ),
                message = if (finished) "Sesja ukończona · poprawne odpowiedzi: $correct z ${session.cards.size}" else null,
            )
        }
    }

    fun endSession() {
        if (state.value.busy) return
        mutableState.value = state.value.copy(session = null, error = null, message = "Dotychczasowe odpowiedzi zostały zapisane.")
    }

    fun dismissError() { mutableState.value = state.value.copy(error = null) }
    fun close() { scope.cancel() }

    private fun perform(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true, error = null)
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = state.value.copy(error =
                    if (!state.value.ready) "Nie udało się przygotować słówek. ${error.message.orEmpty()}"
                    else "Nie udało się zapisać lub odczytać postępu. Spróbuj ponownie.")
            } finally {
                mutableState.value = state.value.copy(busy = false)
            }
        }
    }
}

@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
fun createStudyController(databasePath: String) = StudyController(
    FlashcardRepository(databasePath) { Res.readBytes("files/slowka.csv").decodeToString() },
)
