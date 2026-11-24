package be.dexter.serenite

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun buzz(ctx: Context, ms: Long = 40) {
    runCatching {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else ctx.getSystemService(Vibrator::class.java)
        v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun logSession(store: Store, tool: String, seconds: Int) {
    if (seconds < 20) return
    store.update { s -> s.copy(sessions = s.sessions + Session(Store.newId(), System.currentTimeMillis(), tool, seconds)) }
}

@Composable
private fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

private data class Phase(val label: String, val seconds: Int, val target: Float) // target : taille du cercle (0.45..1)

@Composable
fun BreathingScreen(state: AppState, store: Store, patternId: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val b = Content.breath(patternId)
    var minutes by remember { mutableIntStateOf(b.defaultMinutes) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var phaseLabel by remember { mutableStateOf("Prêt") }
    var phaseEnd by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scale = remember { Animatable(0.45f) }
    val voice = rememberVoice()
    val phases = remember(b) {
        listOf(
            Phase("Inspire", b.inhale, 1f), Phase("Retiens", b.hold1, 1f),
            Phase("Expire", b.exhale, 0.45f), Phase("Retiens", b.hold2, 0.45f),
        ).filter { it.seconds > 0 }
    }
    KeepScreenOn(running)

    LaunchedEffect(running) {
        if (!running) { scale.animateTo(0.45f, tween(800)); return@LaunchedEffect }
        startedAt = System.currentTimeMillis()
        finished = false
        val total = minutes * 60_000L
        while (System.currentTimeMillis() - startedAt < total) {
            for (p in phases) {
                phaseLabel = p.label
                phaseEnd = System.currentTimeMillis() + p.seconds * 1000L
                if (state.settings.vibrate) buzz(ctx)
                if (Store.get(ctx).state.value.settings.voice) voice.say(p.label)
                if (p.target != scale.value) launch { scale.animateTo(p.target, tween(p.seconds * 1000, easing = LinearEasing)) }
                delay(p.seconds * 1000L)
            }
        }
        logSession(store, "breath:" + b.id, ((System.currentTimeMillis() - startedAt) / 1000).toInt())
        if (state.settings.vibrate) buzz(ctx, 300)
        phaseLabel = "Terminé"
        finished = true
        running = false
    }
    LaunchedEffect(running) {
        while (running) { now = System.currentTimeMillis(); delay(200) }
    }

    Screen(b.name, {
        if (running) logSession(store, "breath:" + b.id, ((System.currentTimeMillis() - startedAt) / 1000).toInt())
        onBack()
    }, actions = { VoiceToggle(state, store, voice) }) {
        Muted(b.desc)
        val primary = MaterialTheme.colorScheme.primary
        val soft = MaterialTheme.colorScheme.primaryContainer
        Box(Modifier.fillMaxWidth().size(280.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(280.dp)) {
                val rMax = size.minDimension / 2
                drawCircle(soft.copy(alpha = 0.4f), rMax)
                drawCircle(soft, rMax * scale.value)
                drawCircle(primary, rMax * scale.value, style = Stroke(width = 6f))
                drawCircle(primary.copy(alpha = 0.5f), rMax * 0.45f, center = Offset(size.width / 2, size.height / 2), style = Stroke(width = 2f))
            }
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phaseLabel, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                if (running) Text("${((phaseEnd - now) / 1000 + 1).coerceAtLeast(1)}", style = MaterialTheme.typography.headlineSmall)
            }
        }
        if (running) {
            val elapsed = (now - startedAt).coerceAtLeast(0)
            val total = minutes * 60_000L
            LinearProgressIndicator(progress = { (elapsed.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Centered { Muted("%d:%02d / %d:00".format(elapsed / 60000, (elapsed / 1000) % 60, minutes)) }
            OutlinedButton(onClick = {
                logSession(store, "breath:" + b.id, ((System.currentTimeMillis() - startedAt) / 1000).toInt())
                running = false
                phaseLabel = "Prêt"
            }, modifier = Modifier.fillMaxWidth()) { Text("Arrêter") }
        } else {
            if (finished) InfoCard { Text("Séance terminée. Prends un instant pour remarquer comment tu te sens.") }
            Text("Durée : $minutes min")
            Slider(minutes.toFloat(), { minutes = it.roundToInt().coerceIn(1, 15) }, valueRange = 1f..15f, steps = 13)
            Button(onClick = { running = true }, modifier = Modifier.fillMaxWidth()) { Text(if (finished) "Recommencer" else "Commencer") }
        }
    }
}

/** Exercice guidé étape par étape, avec minuteur et passage automatique. */
@Composable
fun GuidedScreen(title: String, tool: String, steps: List<GuidedStep>, state: AppState, store: Store, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var idx by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(steps.first().seconds) }
    var spent by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val voice = rememberVoice()
    KeepScreenOn(running)

    LaunchedEffect(running, idx) {
        if (!running) { if (!done) voice.stop(); return@LaunchedEffect }
        if (left == steps[idx].seconds && Store.get(ctx).state.value.settings.voice) voice.say(steps[idx].title + ". " + steps[idx].text)
        while (left > 0) {
            delay(1000)
            left--
            spent++
        }
        if (idx < steps.lastIndex) {
            if (state.settings.vibrate) buzz(ctx)
            idx++
            left = steps[idx].seconds
        } else {
            if (state.settings.vibrate) buzz(ctx, 300)
            if (Store.get(ctx).state.value.settings.voice) voice.say("Exercice terminé.")
            running = false
            done = true
            logSession(store, tool, spent)
        }
    }

    Screen(title, {
        if (!done) logSession(store, tool, spent)
        onBack()
    }, actions = { VoiceToggle(state, store, voice) }) {
        LinearProgressIndicator(progress = { (idx + if (done) 1 else 0) / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth())
        Muted("Étape ${idx + 1} / ${steps.size}")
        val s = steps[idx]
        InfoCard {
            Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(s.text, style = MaterialTheme.typography.bodyLarge)
        }
        Centered { Text("$left s", style = MaterialTheme.typography.displaySmall) }
        if (done) {
            InfoCard { Text("Exercice terminé. Observe ton état avant de reprendre tes activités.") }
            Button(onClick = { idx = 0; left = steps.first().seconds; spent = 0; done = false }, modifier = Modifier.fillMaxWidth()) { Text("Recommencer") }
        } else {
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) { Text(if (running) "Pause" else if (spent == 0) "Commencer" else "Reprendre") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = idx > 0, onClick = { idx--; left = steps[idx].seconds }, modifier = Modifier.weight(1f)) { Text("Précédent") }
                OutlinedButton(enabled = idx < steps.lastIndex, onClick = { idx++; left = steps[idx].seconds }, modifier = Modifier.weight(1f)) { Text("Suivant") }
            }
        }
    }
}
