package be.dexter.serenite

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Écran standard : en-tête (retour optionnel), contenu défilant. */
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = if (onBack == null) 20.dp else 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            actions()
        }
        val m = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
        Column(
            if (scroll) m.verticalScroll(rememberScrollState()) else m,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Carte cliquable avec titre, sous-titre et contenu libre à gauche (emoji…). */
@Composable
fun ActionCard(
    title: String,
    subtitle: String? = null,
    leading: String? = null,
    container: Color? = null,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (container != null) CardDefaults.cardColors(containerColor = container) else CardDefaults.cardColors(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Text(leading, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Muted(subtitle)
            }
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(options: List<String>, selected: Collection<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o in selected, onClick = { onToggle(o) }, label = { Text(o) })
        }
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}

// ---------- Couleurs d'humeur ----------

val moodColors = listOf(Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFF4DB6AC))
fun moodColor(mood: Float): Color = moodColors[(mood.toInt() - 1).coerceIn(0, 4)]

// ---------- Dates ----------

private val fr = Locale.FRENCH
private val zone: ZoneId get() = ZoneId.systemDefault()
private val fmtDay = DateTimeFormatter.ofPattern("EEEE d MMMM", fr)
private val fmtShort = DateTimeFormatter.ofPattern("d MMM", fr)
private val fmtTime = DateTimeFormatter.ofPattern("HH:mm", fr)

fun fmtDayOf(at: Long): String = Instant.ofEpochMilli(at).atZone(zone).format(fmtDay).replaceFirstChar { it.titlecase(fr) }
fun fmtEpochDay(d: Long): String = LocalDate.ofEpochDay(d).format(fmtDay).replaceFirstChar { it.titlecase(fr) }
fun fmtShortDay(d: Long): String = LocalDate.ofEpochDay(d).format(fmtShort)
fun fmtTimeOf(at: Long): String = Instant.ofEpochMilli(at).atZone(zone).format(fmtTime)
fun fmtMinute(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

fun relativeDay(at: Long): String {
    val d = Stats.day(at)
    val t = Stats.today()
    return when (d) {
        t -> "Aujourd'hui"
        t - 1 -> "Hier"
        else -> fmtDayOf(at)
    }
}

fun pickTime(ctx: Context, minute: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minute / 60, minute % 60, true).show()
}

fun greeting(name: String): String {
    val h = java.time.LocalTime.now().hour
    val base = when (h) {
        in 5..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        else -> "Bonsoir"
    }
    return if (name.isBlank()) base else "$base $name"
}
