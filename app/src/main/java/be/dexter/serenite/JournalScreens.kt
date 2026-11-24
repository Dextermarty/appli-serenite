package be.dexter.serenite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val kindLabels = mapOf("libre" to "Libre", "gratitude" to "Gratitude", "question" to "Question")
private val kindEmoji = mapOf("libre" to "📝", "gratitude" to "🙏", "question" to "💭")

/** Onglet Journal : raccourcis d'écriture et liste des entrées. */
@Composable
fun JournalTab(state: AppState, nav: Nav) {
    var filter by remember { mutableStateOf<String?>(null) }
    val today = Stats.today()
    Screen("Journal") {
        ActionCard("Écrire librement", "Vider sa tête, raconter sa journée", "📝") { nav.push("journal", "") }
        ActionCard("Trois gratitudes", "Trois choses positives, même petites", "🙏") { nav.push("journal", "g:") }
        ActionCard("Question du jour", Content.promptOfDay(today), "💭") { nav.push("journal", "q:" + Content.promptOfDay(today)) }

        SectionTitle("Mes entrées (${state.journal.size})")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == null, { filter = null }, { Text("Tout") })
            kindLabels.forEach { (k, l) -> FilterChip(filter == k, { filter = if (filter == k) null else k }, { Text(l) }) }
        }
        val list = state.journal.filter { filter == null || it.kind == filter }.sortedByDescending { it.at }
        if (list.isEmpty()) Muted("Rien pour l'instant.")
        list.forEach { j ->
            Card(onClick = { nav.push("journal", j.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${kindEmoji[j.kind] ?: "📝"}  ${relativeDay(j.at)} · ${fmtTimeOf(j.at)}", fontWeight = FontWeight.SemiBold)
                    if (j.prompt.isNotBlank()) Muted(j.prompt)
                    Text(j.text, maxLines = 4, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Éditeur. arg : "" = libre, "g:" = gratitude, "q:<question>" = question guidée, sinon id d'une entrée existante.
 */
@Composable
fun JournalEditor(state: AppState, store: Store, arg: String, onBack: () -> Unit) {
    val existing = state.journal.firstOrNull { it.id == arg }
    val kind = existing?.kind ?: when {
        arg.startsWith("g:") -> "gratitude"
        arg.startsWith("q:") -> "question"
        else -> "libre"
    }
    var prompt by remember { mutableStateOf(existing?.prompt ?: if (kind == "question") arg.removePrefix("q:") else "") }
    var promptIdx by remember { mutableIntStateOf(Content.journalPrompts.indexOf(prompt).coerceAtLeast(0)) }
    var text by remember { mutableStateOf(if (kind == "gratitude") "" else existing?.text ?: "") }
    val g0 = existing?.text?.lines()?.map { it.removePrefix("• ") } ?: emptyList()
    var g1 by remember { mutableStateOf(g0.getOrElse(0) { "" }) }
    var g2 by remember { mutableStateOf(g0.getOrElse(1) { "" }) }
    var g3 by remember { mutableStateOf(g0.getOrElse(2) { "" }) }
    var confirmDelete by remember { mutableStateOf(false) }

    val content = if (kind == "gratitude") listOf(g1, g2, g3).map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n") { "• $it" } else text.trim()

    Screen(
        when (kind) { "gratitude" -> "Gratitude"; "question" -> "Question guidée"; else -> "Journal" },
        onBack,
        actions = { if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Supprimer") } },
    ) {
        if (existing != null) Muted("${fmtDayOf(existing.at)} · ${fmtTimeOf(existing.at)}")
        when (kind) {
            "gratitude" -> {
                Muted("Pense à des choses concrètes : une rencontre, un repas, un moment de calme, quelque chose que tu as réussi.")
                OutlinedTextField(g1, { g1 = it }, Modifier.fillMaxWidth(), label = { Text("1.") })
                OutlinedTextField(g2, { g2 = it }, Modifier.fillMaxWidth(), label = { Text("2.") })
                OutlinedTextField(g3, { g3 = it }, Modifier.fillMaxWidth(), label = { Text("3.") })
            }
            else -> {
                if (kind == "question") {
                    InfoCard {
                        Text(prompt, style = MaterialTheme.typography.titleMedium)
                        if (existing == null) TextButton(onClick = {
                            promptIdx = (promptIdx + 1) % Content.journalPrompts.size
                            prompt = Content.journalPrompts[promptIdx]
                        }) {
                            Icon(Icons.Filled.Refresh, null)
                            Text("  Autre question")
                        }
                    }
                }
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = { Text("Écris ce qui te vient, sans te censurer…") }, minLines = 10)
            }
        }
        Button(
            enabled = content.isNotBlank(),
            onClick = {
                store.update { s ->
                    val e = JournalEntry(existing?.id ?: Store.newId(), existing?.at ?: System.currentTimeMillis(), kind, prompt, content)
                    s.copy(journal = (s.journal.filter { it.id != e.id } + e).sortedBy { it.at })
                }
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Enregistrer") }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette entrée ?") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { s -> s.copy(journal = s.journal.filter { it.id != existing.id }) }
                    confirmDelete = false
                    onBack()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}
