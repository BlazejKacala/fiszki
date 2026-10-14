package pl.bkacala.fiszkide.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.bkacala.fiszkide.Session
import pl.bkacala.fiszkide.StudyController
import pl.bkacala.fiszkide.StudyState
import pl.bkacala.fiszkide.data.MASTERY_STREAK
import pl.bkacala.fiszkide.data.SESSION_SIZE

private val Ink = Color(0xFF203B33)
private val Green = Color(0xFF27644D)
private val Muted = Color(0xFF69766E)
private val Paper = Color(0xFFF7F7F0)
private val Mint = Color(0xFFE5EEDF)
private val Line = Color(0xFFE2E7DC)
private val Rust = Color(0xFFA34838)
private val Shape = RoundedCornerShape(24.dp)

@Composable
fun FiszkiApp(controller: StudyController) {
    val state by controller.state.collectAsState()
    var confirmExit by remember { mutableStateOf(false) }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green, onPrimary = Color.White, primaryContainer = Mint,
            onPrimaryContainer = Ink, background = Paper, onBackground = Ink,
            surface = Color.White, onSurface = Ink, onSurfaceVariant = Muted,
            outlineVariant = Line, error = Rust,
        ),
    ) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.safeDrawingPadding().fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                when {
                    !state.ready -> LoadingScreen(state, controller::initialize)
                    state.session != null -> SessionScreen(
                        session = state.session!!, busy = state.busy,
                        onFlip = controller::flip, onAnswer = controller::answer,
                        onExit = { confirmExit = true },
                    )
                    else -> HomeScreen(state, controller::startSession)
                }
            }
        }
        if (state.ready && state.error != null) {
            AlertDialog(
                onDismissRequest = controller::dismissError,
                title = { Text("Spróbuj jeszcze raz") },
                text = { Text(state.error!!) },
                confirmButton = { TextButton(onClick = controller::dismissError) { Text("Rozumiem") } },
            )
        }
        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text("Zakończyć tę sesję?") },
                text = { Text("Twoje dotychczasowe odpowiedzi są już zapisane. Możesz wrócić do nauki w dowolnej chwili.") },
                confirmButton = {
                    TextButton(enabled = !state.busy, onClick = {
                        confirmExit = false
                        controller.endSession()
                    }) { Text("Zakończ sesję") }
                },
                dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Uczę się dalej") } },
            )
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).background(Green, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text("f.", color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
        }
        Column {
            Text("Fiszki", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("MAŁY KROK. NOWE SŁOWA.", fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 1.2.sp, color = Muted)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = Mint, shape = CircleShape) {
            Text("DE / PL", Modifier.padding(horizontal = 13.dp, vertical = 9.dp), fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = Green)
        }
    }
}

@Composable
private fun HomeScreen(state: StudyState, onStart: () -> Unit) {
    val progress = state.progress
    Column(
        Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 24.dp),
    ) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Brand()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (progress.complete) "WUNDERBAR!" else "TWÓJ NIEMIECKI, CODZIENNIE", color = Green, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Text(
                if (progress.complete) "Wszystkie słowa\nsą już Twoje." else "Słowo po słowie.\nCoraz bliżej celu.",
                fontSize = 34.sp, lineHeight = 39.sp, fontFamily = FontFamily.Serif, color = Ink,
            )
            Text(
                if (progress.complete) "Gratulacje! Każdą frazę udało Ci się przypomnieć 5 razy z rzędu. To kawał dobrej pracy."
                else "Kilka minut skupienia. Jedna krótka sesja.\nZbuduj pewność w nowym języku.",
                color = Muted, lineHeight = 23.sp, fontSize = 15.sp,
            )
        }
        Surface(shape = Shape, color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (progress.complete) "CEL OSIĄGNIĘTY" else "TWÓJ POSTĘP", color = Muted, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                ProgressRing(progress.fraction, progress.learned, progress.total)
                Spacer(Modifier.height(20.dp))
                Text(if (progress.complete) "Brawo, znasz już cały zestaw!" else "Każde zapamiętane słowo zostaje z Tobą.", textAlign = TextAlign.Center, color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Line)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${progress.learning}", "w nauce")
                    Stat("${progress.remaining}", "do opanowania")
                    Stat("5", "trafień do celu")
                }
            }
        }
        state.message?.let {
            Surface(color = Mint, shape = RoundedCornerShape(16.dp)) {
                Text(it, Modifier.fillMaxWidth().padding(16.dp), color = Green, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
        if (!progress.complete) {
            Surface(shape = RoundedCornerShape(20.dp), color = Mint.copy(alpha = 0.6f)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Jak uczymy się słówek?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Przypomnij sobie niemiecką frazę i dotknij fiszki, aby sprawdzić. Pięć poprawnych odpowiedzi z rzędu oznacza opanowane słowo.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
      }
      if (!progress.complete) {
            Column(Modifier.padding(top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart, enabled = !state.busy && progress.remaining > 0,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(18.dp),
                ) {
                    Text(if (state.busy) "Przygotowuję fiszki…" else "Rozpocznij sesję", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("→", fontSize = 24.sp)
                }
                Text("${minOf(SESSION_SIZE, progress.remaining)} fraz w sesji · w swoim tempie", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp, color = Muted)
            }
      }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text(label, fontSize = 10.sp, lineHeight = 14.sp, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProgressRing(fraction: Float, learned: Int, total: Int) {
    val animated by animateFloatAsState(fraction, tween(700))
    Box(
        Modifier.size(166.dp).semantics(mergeDescendants = true) {
            contentDescription = "Opanowane frazy: $learned z $total"
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }, contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Line, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (animated > 0f) drawArc(Green, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$learned", fontSize = 43.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("z $total fraz", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(4.dp))
            Text("OPANOWANE", fontSize = 9.sp, lineHeight = 13.sp, letterSpacing = 1.5.sp, color = Green, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionScreen(session: Session, busy: Boolean, onFlip: () -> Unit, onAnswer: (Long, Boolean) -> Unit, onExit: () -> Unit) {
    BoxWithConstraints(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
        val compact = maxHeight < 580.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 22.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CHWILA NA NIEMIECKI", fontSize = 10.sp, color = Muted, letterSpacing = 1.3.sp)
                    Text("Twoja sesja", fontSize = 24.sp, color = Ink, fontFamily = FontFamily.Serif)
                }
                TextButton(onClick = onExit, enabled = !busy) { Text("Zakończ", color = Muted) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fiszka ${session.index + 1} z ${session.cards.size}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Krok po kroku", color = Muted, fontSize = 12.sp)
                }
                LinearProgressIndicator(progress = { session.index.toFloat() / session.cards.size }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = Green, trackColor = Line)
            }
            key(session.current.id) {
                FlashcardFace(session, busy, onFlip, Modifier.weight(1f).fillMaxWidth(), compact)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!compact) Text("Udało Ci się przypomnieć?", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onAnswer(session.current.id, false) }, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5E8E2), contentColor = Rust),
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                    ) { Text("Jeszcze nie", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = { onAnswer(session.current.id, true) }, enabled = !busy,
                        shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                    ) { Text("Pamiętam", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                }
                if (!compact) Text("Szczera odpowiedź to najlepszy krok do nauki.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FlashcardFace(session: Session, busy: Boolean, onFlip: () -> Unit, modifier: Modifier, compact: Boolean) {
    val rotation by animateFloatAsState(if (session.revealed) 180f else 0f, tween(350))
    val germanSide = rotation > 90f
    val foreground = if (germanSide) Color.White else Ink
    val secondary = if (germanSide) Color(0xFFCBDFD0) else Muted
    Surface(
        onClick = onFlip, enabled = !busy,
        color = if (germanSide) Green else Color.White, shape = RoundedCornerShape(30.dp),
        modifier = modifier.graphicsLayer { rotationY = rotation; cameraDistance = 14 * density },
    ) {
        Column(
            Modifier.fillMaxSize().graphicsLayer { rotationY = if (germanSide) 180f else 0f }.padding(if (compact) 18.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (germanSide) "DEUTSCH" else "PO POLSKU", fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = secondary)
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Text(
                    if (germanSide) session.current.german else session.current.polish,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    fontSize = if (compact) 26.sp else 32.sp, lineHeight = if (compact) 33.sp else 41.sp,
                    fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, color = foreground,
                )
            }
            if (!compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.semantics {
                    contentDescription = "Poprawne odpowiedzi z rzędu: ${session.current.streak} z $MASTERY_STREAK"
                }) {
                    repeat(MASTERY_STREAK) { index ->
                        Box(Modifier.size(7.dp).background(if (index < session.current.streak) secondary else secondary.copy(alpha = 0.22f), CircleShape))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Text(if (germanSide) "Dotknij, aby wrócić do polskiego" else "Dotknij, aby odkryć tłumaczenie", fontSize = 12.sp, textAlign = TextAlign.Center, color = secondary)
        }
    }
}

@Composable
private fun LoadingScreen(state: StudyState, onRetry: () -> Unit) {
    Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.error == null) {
            CircularProgressIndicator(color = Green)
            Spacer(Modifier.height(20.dp))
            Text("Przygotowuję Twoje słówka…", color = Muted)
        } else {
            Text("Nie udało się wczytać słówek", textAlign = TextAlign.Center, fontSize = 23.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(16.dp))
            Text(state.error, textAlign = TextAlign.Center, color = Muted)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, enabled = !state.busy) { Text("Spróbuj ponownie") }
        }
    }
}
