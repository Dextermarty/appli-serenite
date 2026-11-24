package be.dexter.serenite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Onglet Exercices : TCC, respiration, relaxation, activation comportementale. */
@Composable
fun ToolsTab(state: AppState, nav: Nav) {
    Screen("Exercices") {
        SectionTitle("Prévention du burn-out")
        ActionCard("Mon plan anti-rechute", if (state.plan.isEmpty()) "Signaux d'alerte, limites, personnes ressources" else "${state.plan.size} / ${Library.planSections.size} sections remplies", "🛡️") { nav.push("plan") }
        ActionCard("Bilans", "Burn-out (BAT-12), humeur (PHQ-9), anxiété (GAD-7)", "📋") { nav.push("tests") }
        ActionCard("Habitudes", "Petites habitudes protectrices, suivi quotidien", "✅") { nav.push("habits") }
        ActionCard("Décompression de fin de journée", "Marquer la frontière avec le travail · 3 min", "🚪") { nav.push("decompression") }

        SectionTitle("Thérapie cognitive")
        ActionCard("Fiche de pensée", "Examiner une pensée qui pèse et trouver une alternative plus juste", "🧠") { nav.push("thought") }
        ActionCard("Mes fiches (${state.thoughts.size})", "Relire les fiches précédentes", "🗂️") { nav.push("thoughts") }
        ActionCard("Pièges de la pensée", "Les 12 distorsions cognitives et comment les questionner", "🔍") { nav.push("distortions") }
        ActionCard("Activités ressources", "Planifier des moments agréables ou utiles (activation comportementale)", "🗓️") { nav.push("activity") }

        SectionTitle("Respiration")
        Content.breathPatterns.forEach { b ->
            val rhythm = listOf(b.inhale, b.hold1, b.exhale, b.hold2).filter { it > 0 }.joinToString("-")
            ActionCard(b.name, "$rhythm · ${b.defaultMinutes} min", "🌬️") { nav.push("breath", b.id) }
        }

        SectionTitle("Ancrage et relaxation")
        ActionCard("Ancrage 5-4-3-2-1", "Revenir au présent par les sens · 3 min", "🖐️") { nav.push("grounding") }
        ActionCard("Relaxation progressive", "Contracter puis relâcher chaque zone · 6 min", "💆") { nav.push("pmr") }
        ActionCard("Scan corporel", "Parcourir le corps avec attention · 6 min", "🧘") { nav.push("bodyscan") }
        ActionCard("Pause 3 minutes", "Pleine conscience express, au travail", "⏸️") { nav.push("pause3") }
        ActionCard("Lieu ressource", "Visualisation guidée d'un lieu de calme · 5 min", "🏝️") { nav.push("safeplace") }
        ActionCard("Détente avant le sommeil", "Lourdeur, souffle, descente · 6 min", "🌙") { nav.push("sleeprelax") }
    }
}

@Composable
private fun IntensitySlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row {
            Text(label, Modifier.weight(1f))
            Text("$value %", fontWeight = FontWeight.SemiBold)
        }
        Slider(value.toFloat(), { onChange((it / 5).roundToInt() * 5) }, valueRange = 0f..100f)
    }
}

/** Fiche de restructuration cognitive. arg = id d'une fiche existante, vide = nouvelle. */
@Composable
fun ThoughtEditor(state: AppState, store: Store, arg: String, onBack: () -> Unit, nav: Nav) {
    val ex = state.thoughts.firstOrNull { it.id == arg }
    var step by remember { mutableIntStateOf(if (ex != null) 5 else 0) }
    var situation by remember { mutableStateOf(ex?.situation ?: "") }
    var thought by remember { mutableStateOf(ex?.thought ?: "") }
    var emotion by remember { mutableStateOf(ex?.emotion ?: "") }
    var before by remember { mutableIntStateOf(ex?.before ?: 70) }
    var dist by remember { mutableStateOf(ex?.distortions?.toSet() ?: emptySet()) }
    var pour by remember { mutableStateOf(ex?.evidenceFor ?: "") }
    var contre by remember { mutableStateOf(ex?.evidenceAgainst ?: "") }
    var alt by remember { mutableStateOf(ex?.alternative ?: "") }
    var after by remember { mutableIntStateOf(ex?.after ?: before) }
    var info by remember { mutableStateOf<Distortion?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val titles = listOf("1. La situation", "2. La pensée automatique", "3. Les pièges", "4. Les preuves", "5. Une pensée plus juste", "Récapitulatif")

    Screen(
        "Fiche de pensée", onBack,
        actions = { if (ex != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Supprimer") } },
    ) {
        Text(titles[step], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        when (step) {
            0 -> {
                Muted("Décris les faits, comme une caméra les filmerait : où, quand, avec qui, que s'est-il passé ?")
                OutlinedTextField(situation, { situation = it }, Modifier.fillMaxWidth(), minLines = 4, placeholder = { Text("Ex. : réunion de lundi, mon chef a critiqué mon rapport devant l'équipe") })
            }
            1 -> {
                Muted("Quelle pensée a traversé ton esprit à ce moment-là ? Écris-la telle quelle.")
                OutlinedTextField(thought, { thought = it }, Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("Ex. : « Je suis incompétent, tout le monde l'a vu »") })
                Muted("Quelle émotion as-tu ressentie ?")
                ChipFlow(Content.negativeEmotions, setOf(emotion)) { emotion = if (emotion == it) "" else it }
                IntensitySlider("Intensité", before) { before = it }
            }
            2 -> {
                Muted("Cette pensée tombe-t-elle dans un ou plusieurs pièges ? Appuie sur ⓘ pour les détails.")
                Content.distortions.forEach { d ->
                    val sel = d.id in dist
                    Card(
                        onClick = { dist = if (sel) dist - d.id else dist + d.id },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (sel) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else androidx.compose.material3.CardDefaults.cardColors(),
                    ) {
                        Row(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text((if (sel) "✓  " else "") + d.name, Modifier.weight(1f), fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                            IconButton(onClick = { info = d }) { Icon(Icons.Filled.Info, "Détails") }
                        }
                    }
                }
            }
            3 -> {
                val qs = Content.distortions.filter { it.id in dist }
                if (qs.isNotEmpty()) InfoCard {
                    Text("Questions utiles", fontWeight = FontWeight.SemiBold)
                    qs.forEach { Muted("• " + it.question) }
                }
                Muted("Quels faits soutiennent la pensée ?")
                OutlinedTextField(pour, { pour = it }, Modifier.fillMaxWidth(), minLines = 3)
                Muted("Quels faits la contredisent ou la nuancent ?")
                OutlinedTextField(contre, { contre = it }, Modifier.fillMaxWidth(), minLines = 3)
            }
            4 -> {
                Muted("En tenant compte de toutes les preuves, quelle pensée serait plus équilibrée ? Pas forcément positive : plus juste.")
                OutlinedTextField(alt, { alt = it }, Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("Ex. : « Une partie du rapport était à revoir ; le reste a été accepté et je peux corriger. »") })
                IntensitySlider("Intensité de « ${emotion.ifBlank { "l'émotion" }} » maintenant", after) { after = it }
            }
            else -> {
                Recap("Situation", situation)
                Recap("Pensée", thought)
                Recap("Émotion", "${emotion.ifBlank { "—" }} : $before % → $after %")
                Recap("Pièges", Content.distortions.filter { it.id in dist }.joinToString(", ") { it.name })
                Recap("Pour", pour)
                Recap("Contre", contre)
                Recap("Pensée alternative", alt)
                if (after < before) InfoCard { Text("L'intensité a baissé de ${before - after} points. Bien joué.") }
                TextButton(onClick = { step = 0 }) { Text("Modifier") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step in 1..4) OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Précédent") }
            if (step < 4) Button(onClick = { if (step == 1 && ex == null) after = before; step++ }, modifier = Modifier.weight(1f),
                enabled = when (step) { 0 -> situation.isNotBlank(); 1 -> thought.isNotBlank(); else -> true }) { Text("Suivant") }
            if (step == 4) Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    store.update { s ->
                        val r = ThoughtRecord(ex?.id ?: Store.newId(), ex?.at ?: System.currentTimeMillis(), situation.trim(), thought.trim(), emotion,
                            before, Content.distortions.map { it.id }.filter { it in dist }, pour.trim(), contre.trim(), alt.trim(), after)
                        s.copy(thoughts = (s.thoughts.filter { it.id != r.id } + r).sortedBy { it.at })
                    }
                    step = 5
                },
            ) { Text("Enregistrer") }
            if (step == 5) Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Terminé") }
        }
    }

    info?.let { d -> DistortionDialog(d) { info = null } }
    if (confirmDelete && ex != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette fiche ?") },
            confirmButton = {
                TextButton(onClick = {
                    store.update { s -> s.copy(thoughts = s.thoughts.filter { it.id != ex.id }) }
                    confirmDelete = false
                    onBack()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun Recap(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value)
    }
}

@Composable
fun DistortionDialog(d: Distortion, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(d.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(d.desc)
                Muted("Exemple : " + d.example)
                Text("À se demander : " + d.question, fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun ThoughtList(state: AppState, nav: Nav, onBack: () -> Unit) {
    Screen("Mes fiches", onBack, actions = { IconButton(onClick = { nav.push("thought") }) { Icon(Icons.Filled.Add, "Nouvelle fiche") } }) {
        if (state.thoughts.isEmpty()) Muted("Aucune fiche pour l'instant.")
        state.thoughts.sortedByDescending { it.at }.forEach { t ->
            Card(onClick = { nav.push("thought", t.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${relativeDay(t.at)} · ${t.emotion.ifBlank { "—" }} ${t.before} % → ${t.after} %", fontWeight = FontWeight.SemiBold)
                    Text("« ${t.thought} »", maxLines = 2)
                    if (t.alternative.isNotBlank()) Muted("→ ${t.alternative}")
                }
            }
        }
    }
}

@Composable
fun DistortionsScreen(onBack: () -> Unit) {
    Screen("Pièges de la pensée", onBack) {
        Muted("Ces raccourcis de pensée sont normaux et universels. Les repérer aide à prendre du recul sur ce qu'on se dit.")
        Content.distortions.forEach { d ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(d.desc)
                    Muted(d.example)
                    Text("💡 " + d.question, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Activation comportementale : planifier et évaluer des activités. */
@Composable
fun ActivitiesScreen(state: AppState, store: Store, onBack: () -> Unit) {
    val today = Stats.today()
    var title by remember { mutableStateOf("") }
    var dayOffset by remember { mutableIntStateOf(0) }
    var rating by remember { mutableStateOf<PlannedActivity?>(null) }

    Screen("Activités ressources", onBack) {
        Muted("Quand le moral baisse, on fait moins de choses agréables… ce qui fait encore baisser le moral. Planifier de petites activités casse ce cercle. Après coup, note le plaisir et le sentiment d'accomplissement.")
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Activité") }, singleLine = true,
            placeholder = { Text("Ex. : marcher 15 min, appeler un ami…") })
        ChipFlow(listOf("Aujourd'hui", "Demain", "Après-demain"), setOf(listOf("Aujourd'hui", "Demain", "Après-demain")[dayOffset])) {
            dayOffset = listOf("Aujourd'hui", "Demain", "Après-demain").indexOf(it)
        }
        ChipFlow(listOf("Marcher", "Sport", "Appeler un proche", "Lire", "Cuisiner", "Musique", "Ranger un coin", "Nature", "Sortie"), emptySet()) { title = it }
        Button(enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
            store.update { s -> s.copy(activities = s.activities + PlannedActivity(Store.newId(), title.trim(), today + dayOffset)) }
            title = ""
        }) { Text("Planifier") }

        val upcoming = state.activities.filter { !it.done && it.day >= today }.sortedBy { it.day }
        val late = state.activities.filter { !it.done && it.day < today }.sortedByDescending { it.day }
        val done = state.activities.filter { it.done }.sortedByDescending { it.day }

        if (upcoming.isNotEmpty() || late.isNotEmpty()) SectionTitle("À faire")
        (late + upcoming).forEach { a ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text(a.title, fontWeight = FontWeight.SemiBold)
                        Muted(if (a.day == today) "Aujourd'hui" else if (a.day < today) "Prévu ${fmtShortDay(a.day)}" else fmtEpochDay(a.day))
                    }
                    TextButton(onClick = { rating = a }) { Text("Fait") }
                    IconButton(onClick = { store.update { s -> s.copy(activities = s.activities.filter { it.id != a.id }) } }) { Icon(Icons.Filled.Delete, "Supprimer") }
                }
            }
        }
        if (done.isNotEmpty()) {
            SectionTitle("Réalisées")
            val avgP = done.map { it.pleasure }.average()
            val avgM = done.map { it.mastery }.average()
            Muted("Plaisir moyen %.1f/10 · accomplissement moyen %.1f/10".format(avgP, avgM))
            done.take(30).forEach { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("✓ ${a.title}", fontWeight = FontWeight.SemiBold)
                        Muted("${fmtShortDay(a.day)} · plaisir ${a.pleasure}/10 · accomplissement ${a.mastery}/10")
                    }
                }
            }
        }
    }

    rating?.let { a ->
        var p by remember(a.id) { mutableFloatStateOf(5f) }
        var m by remember(a.id) { mutableFloatStateOf(5f) }
        AlertDialog(
            onDismissRequest = { rating = null },
            title = { Text(a.title) },
            text = {
                Column {
                    Text("Plaisir : ${p.roundToInt()}/10")
                    Slider(p, { p = it }, valueRange = 0f..10f, steps = 9)
                    Text("Accomplissement : ${m.roundToInt()}/10")
                    Slider(m, { m = it }, valueRange = 0f..10f, steps = 9)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.update { s ->
                        s.copy(activities = s.activities.map {
                            if (it.id == a.id) it.copy(done = true, pleasure = p.roundToInt(), mastery = m.roundToInt(), day = minOf(it.day, today))
                            else it
                        })
                    }
                    rating = null
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { rating = null }) { Text("Annuler") } },
        )
    }
}
