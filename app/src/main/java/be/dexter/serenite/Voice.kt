package be.dexter.serenite

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Guidage vocal avec la synthèse vocale française du téléphone (hors ligne si la voix est installée). */
class Voice(ctx: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(ctx.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale.FRANCE)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.FRENCH)
        tts.setSpeechRate(0.85f)
        tts.setPitch(0.95f)
        ready = true
    }

    fun say(text: String) {
        if (ready) runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "serenite") }
    }

    fun stop() { runCatching { tts.stop() } }
    fun shutdown() { runCatching { tts.stop(); tts.shutdown() } }
}

@Composable
fun rememberVoice(): Voice {
    val ctx = LocalContext.current
    val v = remember { Voice(ctx) }
    DisposableEffect(Unit) { onDispose { v.shutdown() } }
    return v
}

/** Bouton d'en-tête pour couper / activer la voix. */
@Composable
fun VoiceToggle(state: AppState, store: Store, voice: Voice) {
    val on = state.settings.voice
    TextButton(onClick = {
        if (on) voice.stop()
        store.update { it.copy(settings = it.settings.copy(voice = !on)) }
    }) { Text(if (on) "🔊" else "🔇") }
}
