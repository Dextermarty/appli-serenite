package be.dexter.serenite

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Saisie ou modification d'un check-in. arg = id à modifier, vide = nouveau. */
@Composable
fun MoodEditor(state: AppState, store: Store, arg: String, onBack: () -> Unit) {
    val existing = state.moods.firstOrNull { it.id == arg }
    var mood by remember { mutableIntStateOf(existing?.mood ?: 0) }
    var emotions by remember { mutableStateOf(existing?.emotions?.toSet() ?: emptySet()) }
    var factors by remember { mutableStateOf(existing?.factors?.toSet() ?: emptySet()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    Screen(if (existing == null) "Check-in" else "Modifier le check-in", onBack) {
        SectionTitle("Comment te sens-tu ?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..5).forEach { v ->
                val sel = mood == v
                Column(
                    Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .background(if (sel) moodColors[v - 1].copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(16.dp))
                        .border(if (sel) 2.dp else 0.dp, if (sel) moodColors[v - 1] else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { mood = v }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Content.moodEmojis[v - 1], style = MaterialTheme.typography.headlineMedium)
                    Text(Content.moodLabels[v - 1], style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        SectionTitle("Émotions")
        Muted("Agréables")
        ChipFlow(Content.positiveEmotions, emotions) { e -> emotions = if (e in emotions) emotions - e else emotions + e }
        Muted("Difficiles")
        ChipFlow(Content.negativeEmotions, emotions) { e -> emotions = if (e in emotions) emotions - e else emotions + e }

        SectionTitle("Qu'est-ce qui a joué ?")
        ChipFlow(Content.factors, factors) { f -> factors = if (f in factors) factors - f else factors + f }

        SectionTitle("Note")
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text("Ce qui s'est passé, ce que tu ressens…") }, minLines = 3)

        Button(
            enabled = mood > 0,
            onClick = {
                store.update { s ->
                    val e = MoodEntry(existing?.id ?: Store.newId(), existing?.at ?: System.currentTimeMillis(), mood,
                        Content.emotions.filter { it in emotions }, Content.factors.filter { it in factors }, note.trim())
                    s.copy(moods = (s.moods.filter { it.id != e.id } + e).sortedBy { it.at })
                }
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enregistrer") }
        if (mood in 1..2) {
            InfoCard {
                Text("Journée difficile ?", fontWeight = FontWeight.SemiBold)
                Muted("Après l'enregistrement, un exercice de respiration ou une fiche de pensée peut aider. Si c'est trop lourd, le bouton « Besoin d'aide » de l'accueil donne des numéros d'écoute.")
            }
        }
    }
}

/** Onglet Humeur : statistiques, courbe, facteurs et historique. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodTab(state: AppState, store: Store, nav: Nav) {
    var period by remember { mutableIntStateOf(1) } // 0 = 7 j, 1 = 30 j, 2 = 90 j
    val days = listOf(7, 30, 90)[period]
    var toDelete by remember { mutableStateOf<MoodEntry?>(null) }

    Screen("Humeur", actions = { IconButton(onClick = { nav.push("mood") }) { Icon(Icons.Filled.Add, "Nouveau check-in") } }) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("7 jours", "30 jours", "90 jours").forEachIndexed { i, l ->
                SegmentedButton(selected = period == i, onClick = { period = i }, shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
            }
        }
        if (state.moods.isEmpty()) {
            InfoCard { Text("Aucun check-in pour l'instant."); Muted("Fais ton premier check-in avec le bouton +. Après quelques jours, tes tendances apparaîtront ici.") }
            return@Screen
        }
        val avg = Stats.average(state, days)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Moyenne", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(avg?.let { "%.1f / 5".format(it) } ?: "–", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                MoodChart(Stats.dailyAverages(state, days))
                Row(Modifier.fillMaxWidth()) {
                    Muted(fmtShortDay(Stats.today() - days + 1), Modifier.weight(1f))
                    Muted("aujourd'hui")
                }
            }
        }

        val top = Stats.topEmotions(state, days)
        if (top.isNotEmpty()) {
            SectionTitle("Émotions fréquentes")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val max = top.first().second.toFloat()
                    top.forEach { (e, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(e, Modifier.width(110.dp))
                            Box(Modifier.weight(1f)) {
                                Box(
                                    Modifier.fillMaxWidth(n / max).height(10.dp).background(
                                        if (e in Content.positiveEmotions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        RoundedCornerShape(5.dp),
                                    ),
                                )
                            }
                            Text("  $n")
                        }
                    }
                }
            }
        }

        val impact = Stats.factorImpact(state)
        SectionTitle("Ce qui influence ton humeur")
        if (impact.isEmpty()) Muted("Coche des facteurs dans tes check-ins : dès qu'un facteur apparaît 3 fois, son effet moyen s'affiche ici.")
        else Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                impact.forEach { (f, d) ->
                    Row {
                        Text(f, Modifier.weight(1f))
                        Text(
                            (if (d > 0) "+" else "") + "%.1f".format(d),
                            color = when {
                                d >= 0.2f -> Color(0xFF2E7D32)
                                d <= -0.2f -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Muted("Écart par rapport à ton humeur moyenne quand ce facteur est coché.")
            }
        }

        SectionTitle("Historique")
        state.moods.sortedByDescending { it.at }.take(60).forEach { m ->
            Card(onClick = { nav.push("mood", m.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(moodColors[m.mood - 1].copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(Content.moodEmojis[m.mood - 1], style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${relativeDay(m.at)} · ${fmtTimeOf(m.at)}", fontWeight = FontWeight.SemiBold)
                        val sub = (m.emotions + m.factors).joinToString(", ")
                        if (sub.isNotBlank()) Muted(sub)
                        if (m.note.isNotBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                    IconButton(onClick = { toDelete = m }) { Icon(Icons.Filled.Delete, "Supprimer") }
                }
            }
        }
    }

    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer ce check-in ?") },
            confirmButton = { TextButton(onClick = { store.update { s -> s.copy(moods = s.moods.filter { it.id != m.id }) }; toDelete = null }) { Text("Supprimer") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } },
        )
    }
}

/** Courbe des moyennes journalières (1..5). Les jours sans check-in sont sautés. */
@Composable
fun MoodChart(points: List<Pair<Long, Float?>>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val n = points.size
        if (n == 0) return@Canvas
        val padY = 10f
        fun x(i: Int) = if (n == 1) size.width / 2 else i * size.width / (n - 1)
        fun y(v: Float) = padY + (5f - v) / 4f * (size.height - 2 * padY)
        for (v in 1..5) drawLine(grid, Offset(0f, y(v.toFloat())), Offset(size.width, y(v.toFloat())), strokeWidth = 1f)
        val path = Path()
        var started = false
        points.forEachIndexed { i, (_, v) ->
            if (v == null) return@forEachIndexed
            if (!started) { path.moveTo(x(i), y(v)); started = true } else path.lineTo(x(i), y(v))
        }
        drawPath(path, line, style = Stroke(width = 4f))
        val r = if (n > 40) 4f else 7f
        points.forEachIndexed { i, (_, v) -> if (v != null) drawCircle(moodColor(v + 0.5f), r, Offset(x(i), y(v))) }
    }
}
