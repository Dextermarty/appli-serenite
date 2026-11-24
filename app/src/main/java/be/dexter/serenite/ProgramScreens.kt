package be.dexter.serenite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgramsTab(state: AppState, store: Store, nav: Nav, onBack: (() -> Unit)? = null) {
    Screen("Parcours", onBack) {
        Muted("Des programmes de 7 à 10 jours : une courte leçon et un exercice par jour.")
        Content.programs.forEach { prog ->
            val p = state.programs[prog.id]
            val n = p?.doneDays?.size ?: 0
            val active = state.activeProgram == prog.id
            Card(
                onClick = { nav.push("program", prog.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (active) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(prog.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(prog.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Muted(prog.subtitle)
                        if (n > 0) {
                            LinearProgressIndicator(progress = { n / prog.days.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                            Muted(if (n == prog.days.size) "Terminé ✓" else "$n / ${prog.days.size} jours" + if (active) " · en cours" else "")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgramScreen(state: AppState, store: Store, id: String, nav: Nav, onBack: () -> Unit) {
    val prog = Content.program(id) ?: return onBack()
    val p = state.programs[prog.id]
    val today = Stats.today()
    val active = state.activeProgram == prog.id
    Screen(prog.title, onBack) {
        Text("${prog.emoji}  ${prog.subtitle}")
        if (!active) {
            Button(onClick = { store.update { it.copy(activeProgram = prog.id) } }, modifier = Modifier.fillMaxWidth()) {
                Text(if (p == null) "Commencer ce parcours" else "Reprendre ce parcours")
            }
        }
        prog.days.forEachIndexed { i, d ->
            val done = p != null && i in p.doneDays
            val unlocked = Stats.isUnlocked(p, i, state.settings.oneDayPerDay, today)
            Card(
                onClick = { if (unlocked) nav.push("programDay", "${prog.id}:$i") },
                enabled = unlocked,
                modifier = Modifier.fillMaxWidth(),
                colors = if (done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (done) "✅" else if (unlocked) "▶️" else "🔒", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jour ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(d.title, fontWeight = FontWeight.SemiBold)
                        if (!unlocked && p != null && (i - 1) in p.doneDays) Muted("Disponible demain")
                    }
                }
            }
        }
        if (p != null) {
            OutlinedButton(
                onClick = { store.update { s -> s.copy(programs = s.programs - prog.id, activeProgram = if (s.activeProgram == prog.id) null else s.activeProgram) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Réinitialiser ce parcours") }
        }
    }
}

@Composable
fun ProgramDayScreen(state: AppState, store: Store, arg: String, nav: Nav, onBack: () -> Unit) {
    val (id, idxS) = arg.split(":").let { it[0] to it.getOrElse(1) { "0" } }
    val prog = Content.program(id) ?: return onBack()
    val i = idxS.toIntOrNull()?.coerceIn(0, prog.days.lastIndex) ?: 0
    val d = prog.days[i]
    val p = state.programs[prog.id]
    val done = p != null && i in p.doneDays
    Screen("Jour ${i + 1}", onBack) {
        Text(d.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(d.lesson, style = MaterialTheme.typography.bodyLarge)
        InfoCard {
            Text("À faire aujourd'hui", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(d.action)
        }
        d.link?.let { link -> OutlinedButton(onClick = { nav.open(link) }, modifier = Modifier.fillMaxWidth()) { Text("Ouvrir l'exercice") } }
        if (done) Centered { Text("✅ Jour terminé") }
        else Button(
            onClick = {
                store.update { s -> s.copy(programs = s.programs + (prog.id to Stats.complete(s.programs[prog.id], prog.id, i)), activeProgram = prog.id) }
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Marquer ce jour comme terminé") }
    }
}
