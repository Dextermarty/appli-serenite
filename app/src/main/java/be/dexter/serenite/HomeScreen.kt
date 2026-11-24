package be.dexter.serenite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(state: AppState, nav: Nav) {
    val today = Stats.today()
    val mood = Stats.moodToday(state, today)
    val streak = Stats.streak(state, today)
    Screen(
        greeting(state.settings.name),
        actions = { IconButton(onClick = { nav.push("settings") }) { Icon(Icons.Filled.Settings, "Réglages") } },
    ) {
        Muted(fmtEpochDay(today))

        // Check-in du jour
        if (mood == null) {
            ActionCard("Comment te sens-tu ?", "Ton check-in du jour, en 30 secondes", "🌤️",
                container = MaterialTheme.colorScheme.primaryContainer) { nav.push("mood") }
        } else {
            ActionCard(
                "Humeur du jour : ${Content.moodLabels[mood.mood - 1]}",
                (mood.emotions.take(3).joinToString(", ").ifBlank { "Check-in fait à ${fmtTimeOf(mood.at)}" }) + " · nouveau check-in",
                Content.moodEmojis[mood.mood - 1],
            ) { nav.push("mood") }
        }

        // Statistiques rapides
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("🔥", "$streak j", "série", Modifier.weight(1f))
            StatTile("📈", Stats.average(state, 7, today)?.let { "%.1f".format(it) } ?: "–", "humeur 7 j", Modifier.weight(1f))
            StatTile("⏱️", "${Stats.practiceMinutes(state, 7, today)} min", "pratique 7 j", Modifier.weight(1f))
        }

        // Parcours en cours
        val prog = Content.program(state.activeProgram)
        if (prog != null) {
            val p = state.programs[prog.id]
            val next = Stats.nextDay(p, prog.days.size)
            Card(onClick = { nav.push("program", prog.id) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${prog.emoji}  ${prog.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { (p?.doneDays?.size ?: 0) / prog.days.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                    when {
                        next == null -> Text("Parcours terminé, bravo !")
                        Stats.isUnlocked(p, next, state.settings.oneDayPerDay, today) ->
                            Text("Jour ${next + 1} : ${prog.days[next].title}")
                        else -> Text("Jour ${next + 1} disponible demain")
                    }
                }
            }
        } else {
            ActionCard("Commencer un parcours", "Programmes de 7 jours : stress, anxiété, sommeil, bienveillance", "🧭") { nav.tab = 4 }
        }

        SectionTitle("Pour toi maintenant")
        val quick = when {
            mood != null && mood.mood <= 2 && ("Anxiété" in mood.emotions || "Peur" in mood.emotions) ->
                Triple("Ancrage 5-4-3-2-1", "Revenir au présent quand l'anxiété monte", Route("grounding"))
            mood != null && mood.mood <= 2 ->
                Triple("Examiner une pensée", "Une fiche pour prendre du recul", Route("thought"))
            java.time.LocalTime.now().hour >= 21 ->
                Triple("Respiration 4-7-8", "Préparer l'endormissement", Route("breath", "478"))
            else -> Triple("Cohérence cardiaque", "5 minutes pour faire redescendre la pression", Route("breath", "coherence"))
        }
        ActionCard(quick.first, quick.second, "✨") { nav.push(quick.third) }
        ActionCard("Question du jour", Content.promptOfDay(today), "📝") { nav.push("journal", "q:" + Content.promptOfDay(today)) }

        InfoCard {
            Text("Conseil du jour", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(Content.tipOfDay(today))
        }

        TextButton(onClick = { nav.push("help") }, modifier = Modifier.fillMaxWidth()) { Text("Besoin d'aide maintenant ?") }
    }
}

@Composable
fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OnboardingScreen(store: Store) {
    var name by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text("🌿", style = MaterialTheme.typography.displayLarge)
        Text("Sérénité", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "Suivi d'humeur, journal, exercices de TCC, respiration et parcours guidés. Tout reste sur ton téléphone.",
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(name, { name = it }, label = { Text("Ton prénom (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        InfoCard { Muted(Content.DISCLAIMER) }
        Button(
            onClick = { store.update { it.copy(onboarded = true, settings = it.settings.copy(name = name.trim())) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Commencer") }
    }
}
