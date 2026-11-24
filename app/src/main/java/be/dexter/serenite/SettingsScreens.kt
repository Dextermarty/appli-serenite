package be.dexter.serenite

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
private fun SwitchRow(title: String, sub: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            if (sub != null) Muted(sub)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun ReminderRow(title: String, on: Boolean, minute: Int, onToggle: (Boolean) -> Unit, onTime: (Int) -> Unit) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        TextButton(enabled = on, onClick = { pickTime(ctx, minute, onTime) }) { Text(fmtMinute(minute)) }
        Switch(on, onToggle)
    }
}

@Composable
fun SettingsScreen(state: AppState, store: Store, nav: Nav, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val st = state.settings
    var name by remember { mutableStateOf(st.name) }
    var confirmWipe by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<AppState?>(null) }
    fun set(f: (Settings) -> Settings) = store.update { it.copy(settings = f(it.settings)) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(StateJson.toJson(store.state.value).toByteArray()) }
        }.isSuccess
        Toast.makeText(ctx, if (ok) "Sauvegarde exportée" else "Échec de l'export", Toast.LENGTH_SHORT).show()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val s = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { StateJson.fromJson(it.readBytes().decodeToString()) }
        }.getOrNull()
        if (s == null) Toast.makeText(ctx, "Fichier illisible", Toast.LENGTH_SHORT).show() else pendingImport = s
    }

    Screen("Réglages", onBack) {
        SectionTitle("Profil")
        OutlinedTextField(name, { name = it; set { s -> s.copy(name = it.trim()) } }, Modifier.fillMaxWidth(), label = { Text("Prénom") }, singleLine = true)

        SectionTitle("Rappels")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                ReminderRow("Check-in d'humeur", st.remindMood, st.moodTime, { v -> set { it.copy(remindMood = v) } }, { m -> set { it.copy(moodTime = m) } })
                ReminderRow("Journal du soir", st.remindEvening, st.eveningTime, { v -> set { it.copy(remindEvening = v) } }, { m -> set { it.copy(eveningTime = m) } })
                ReminderRow("Parcours du jour", st.remindProgram, st.programTime, { v -> set { it.copy(remindProgram = v) } }, { m -> set { it.copy(programTime = m) } })
            }
        }
        Muted("Le rappel d'humeur ne s'affiche pas si le check-in du jour est déjà fait.")

        SectionTitle("Exercices et parcours")
        SwitchRow("Vibrations", "Pendant la respiration et les exercices guidés", st.vibrate) { v -> set { it.copy(vibrate = v) } }
        SwitchRow("Un jour de parcours par jour", "Sinon, le jour suivant se débloque immédiatement", st.oneDayPerDay) { v -> set { it.copy(oneDayPerDay = v) } }

        SectionTitle("Données")
        Muted("Tout est stocké uniquement sur ce téléphone. ${state.moods.size} check-ins, ${state.journal.size} entrées de journal, ${state.thoughts.size} fiches, ${state.sessions.size} séances.")
        Button(onClick = { exporter.launch("serenite-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("Exporter une sauvegarde") }
        OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Restaurer une sauvegarde") }
        OutlinedButton(
            onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Effacer toutes les données") }

        SectionTitle("À propos")
        InfoCard { Muted(Content.DISCLAIMER) }
        TextButton(onClick = { nav.push("help") }) { Text("Numéros d'aide") }
        Muted("Sérénité " + runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty())
    }

    if (confirmWipe) AlertDialog(
        onDismissRequest = { confirmWipe = false },
        title = { Text("Tout effacer ?") },
        text = { Text("Check-ins, journal, fiches, séances et parcours seront supprimés définitivement. Pense à exporter une sauvegarde avant.") },
        confirmButton = {
            TextButton(onClick = {
                store.update { AppState(onboarded = true, settings = it.settings) }
                confirmWipe = false
            }) { Text("Effacer", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Annuler") } },
    )

    pendingImport?.let { s ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restaurer cette sauvegarde ?") },
            text = { Text("${s.moods.size} check-ins, ${s.journal.size} entrées, ${s.thoughts.size} fiches. Les données actuelles seront remplacées.") },
            confirmButton = {
                TextButton(onClick = {
                    store.replace(s.copy(onboarded = true))
                    pendingImport = null
                    Toast.makeText(ctx, "Sauvegarde restaurée", Toast.LENGTH_SHORT).show()
                }) { Text("Restaurer") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Annuler") } },
        )
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Screen("Besoin d'aide", onBack) {
        Text("Si tu te sens en danger ou dépassé, tu n'as pas à rester seul avec ça. Ces lignes sont gratuites et anonymes.")
        Content.helpLines.forEach { h ->
            Card(
                onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + h.number))) } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📞  " + h.name, fontWeight = FontWeight.SemiBold)
                    Muted(h.desc)
                }
            }
        }
        Muted("Appuie sur une carte pour composer le numéro. Ton médecin traitant peut aussi t'orienter vers un psychologue.")
    }
}
