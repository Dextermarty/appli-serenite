package be.dexter.serenite

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val bandColors = listOf(Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFEF6C00), Color(0xFFE53935))

/** Couleur d'un niveau de résultat, du vert au rouge selon le nombre de niveaux du test. */
fun bandColor(q: Questionnaire, band: Int): Color {
    val n = q.bands.size
    val idx = if (n <= 1) 0 else (band * (bandColors.size - 1) / (n - 1))
    return bandColors[idx.coerceIn(0, bandColors.lastIndex)]
}

fun fmtScore(q: Questionnaire, s: Float) = if (q.mean) "%.2f".format(s) else s.toInt().toString()

// ---------------- Questionnaires ----------------

@Composable
fun TestsScreen(state: AppState, nav: Nav, onBack: () -> Unit) {
    Screen("Bilans", onBack) {
        Muted("Des questionnaires reconnus pour suivre ton état dans le temps. Refais-les toutes les 2 à 4 semaines. Ce ne sont pas des diagnostics : montre-les à ton médecin si un résultat t'inquiète.")
        Library.tests.forEach { q ->
            val last = state.tests.filter { it.testId == q.id }.maxByOrNull { it.at }
            ActionCard(q.name,
                if (last == null) "${q.items.size} questions · jamais fait"
                else "Dernier : ${fmtScore(q, last.score)} — ${q.bands[q.band(last.score)].second} (${relativeDay(last.at).lowercase()})",
                "📋") { nav.push("test", q.id) }
        }
    }
}

@Composable
fun TestScreen(state: AppState, store: Store, id: String, nav: Nav, onBack: () -> Unit) {
    val q = Library.test(id) ?: return onBack()
    var step by remember { mutableIntStateOf(-1) } // -1 intro, 0..n-1 questions, n résultat
    val answers = remember { mutableStateListOf<Int>().apply { repeat(q.items.size) { add(-1) } } }
    var result by remember { mutableStateOf<TestResult?>(null) }
    val history = state.tests.filter { it.testId == q.id }.sortedByDescending { it.at }

    Screen(q.name, onBack) {
        when {
            step < 0 -> {
                Text(q.intro)
                Muted("${q.items.size} questions · environ ${if (q.items.size > 9) 3 else 2} minutes")
                Button(onClick = { step = 0 }, modifier = Modifier.fillMaxWidth()) { Text("Commencer") }
                if (history.isNotEmpty()) {
                    SectionTitle("Historique")
                    ScoreHistory(q, history)
                }
                Muted(q.source)
            }
            step < q.items.size -> {
                LinearProgressIndicator(progress = { step / q.items.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                Muted("Question ${step + 1} / ${q.items.size}")
                Text(q.items[step], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                q.answers.forEachIndexed { i, a ->
                    val sel = answers[step] == q.values[i]
                    Card(
                        onClick = {
                            answers[step] = q.values[i]
                            if (step < q.items.lastIndex) step++
                            else {
                                val r = TestResult(Store.newId(), System.currentTimeMillis(), q.id, answers.toList(), q.score(answers.toList()))
                                store.update { s -> s.copy(tests = s.tests + r) }
                                result = r
                                step = q.items.size
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (sel) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
                    ) { Text(a, Modifier.padding(16.dp)) }
                }
                if (step > 0) TextButton(onClick = { step-- }) { Text("Question précédente") }
            }
            else -> {
                val r = result ?: history.firstOrNull() ?: return@Screen
                TestResultCard(q, r)
                if (q.id == "phq9" && r.answers.getOrElse(8) { 0 } > 0) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tu as indiqué avoir eu des pensées de mort ou de te faire du mal.", fontWeight = FontWeight.SemiBold)
                            Text("C'est important d'en parler à quelqu'un dès aujourd'hui : ton médecin, un proche, ou une ligne d'écoute gratuite et anonyme, 24 h/24.")
                            Button(onClick = { nav.push("help") }, modifier = Modifier.fillMaxWidth()) { Text("Voir les numéros d'aide") }
                        }
                    }
                }
                val band = q.band(r.score)
                if (band >= 1) InfoCard {
                    Text("Et maintenant ?", fontWeight = FontWeight.SemiBold)
                    Text(
                        when (q.id) {
                            "bat12" -> "Ce score indique un niveau d'épuisement à prendre au sérieux. Parles-en à ton médecin traitant ou au médecin du travail, et ouvre ton plan anti-rechute."
                            else -> if (band >= 2) "Ce score mérite d'en parler à ton médecin traitant ou à un psychologue." else "Garde un œil sur l'évolution et refais ce bilan dans 2 semaines."
                        }
                    )
                    if (q.id == "bat12") TextButton(onClick = { nav.push("plan") }) { Text("Ouvrir mon plan anti-rechute") }
                }
                if (history.size > 1) {
                    SectionTitle("Évolution")
                    ScoreHistory(q, history)
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Terminé") }
            }
        }
    }
}

@Composable
fun TestResultCard(q: Questionnaire, r: TestResult) {
    val band = q.band(r.score)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(bandColor(q, band), CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(q.bands[band].second, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text("Score : ${fmtScore(q, r.score)} / ${fmtScore(q, q.max)}")
            Box(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) {
                val lo = if (q.mean) 1f else 0f
                Box(Modifier.fillMaxWidth(((r.score - lo) / (q.max - lo)).coerceIn(0.02f, 1f)).height(10.dp).background(bandColor(q, band), RoundedCornerShape(5.dp)))
            }
            q.subscales.forEach { (name, idx) ->
                val m = idx.map { r.answers.getOrElse(it) { 0 } }.average()
                Row {
                    Text(name, Modifier.weight(1f))
                    Text("%.1f / 5".format(m), fontWeight = FontWeight.SemiBold)
                }
            }
            Muted(relativeDay(r.at) + " · " + fmtTimeOf(r.at))
        }
    }
}

@Composable
fun ScoreHistory(q: Questionnaire, history: List<TestResult>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            history.take(12).forEach { r ->
                val b = q.band(r.score)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(bandColor(q, b), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(fmtShortDay(Stats.day(r.at)), Modifier.width(70.dp))
                    Text(fmtScore(q, r.score), Modifier.width(50.dp), fontWeight = FontWeight.SemiBold)
                    Muted(q.bands[b].second)
                }
            }
        }
    }
}

// ---------------- Habitudes ----------------

@Composable
fun HabitsScreen(state: AppState, store: Store, onBack: () -> Unit) {
    val today = Stats.today()
    var adding by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }
    val active = state.habits.filter { !it.archived }

    fun toggle(h: Habit, day: Long) = store.update { s ->
        s.copy(habits = s.habits.map { if (it.id == h.id) it.copy(days = if (day in it.days) it.days - day else it.days + day) else it })
    }

    Screen("Habitudes", onBack, actions = { IconButton(onClick = { adding = true }) { Icon(Icons.Filled.Add, "Ajouter") } }) {
        if (active.isEmpty()) {
            Muted("De petites habitudes répétées protègent mieux que de grandes résolutions. Choisis-en 2 ou 3 pour commencer.")
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) { Text("Choisir mes habitudes") }
        }
        active.forEach { h ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.title, fontWeight = FontWeight.SemiBold)
                            val st = Stats.habitStreak(h, today)
                            Muted(if (st > 0) "🔥 $st j d'affilée · ${h.days.count { it > today - 30 }}/30 j" else "${h.days.count { it > today - 30 }}/30 derniers jours")
                        }
                        Checkbox(checked = today in h.days, onCheckedChange = { toggle(h, today) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (6 downTo 0).forEach { k ->
                            val d = today - k
                            val done = d in h.days
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(java.time.LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.FRENCH),
                                    style = MaterialTheme.typography.labelSmall)
                                Box(
                                    Modifier.size(26.dp).background(
                                        if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape,
                                    ),
                                )
                            }
                        }
                    }
                    Row {
                        TextButton(onClick = { toggle(h, today - 1) }) { Text(if (today - 1 in h.days) "Décocher hier" else "Fait hier") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { store.update { s -> s.copy(habits = s.habits.map { if (it.id == h.id) it.copy(archived = true) else it }) } }) {
                            Icon(Icons.Filled.Delete, "Retirer")
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Ajouter une habitude") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Library.habitIdeas.filter { (_, t) -> active.none { it.title == t } }.forEach { (e, t) ->
                        TextButton(onClick = {
                            store.update { s -> s.copy(habits = s.habits.filter { it.title != t } + Habit(Store.newId(), t, e)) }
                        }) { Text("$e  $t", Modifier.fillMaxWidth()) }
                    }
                    OutlinedTextField(custom, { custom = it }, label = { Text("Autre habitude") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (custom.isNotBlank()) store.update { s -> s.copy(habits = s.habits + Habit(Store.newId(), custom.trim(), "⭐")) }
                    custom = ""
                    adding = false
                }) { Text(if (custom.isBlank()) "Fermer" else "Ajouter") }
            },
        )
    }
}

// ---------------- Plan anti-rechute ----------------

@Composable
fun PlanScreen(state: AppState, store: Store, onBack: () -> Unit) {
    val texts = remember { mutableStateOf(state.plan) }
    Screen("Mon plan anti-rechute", {
        store.update { it.copy(plan = texts.value.filterValues { v -> v.isNotBlank() }) }
        onBack()
    }) {
        Muted("Rédigé à froid, ce plan te guidera le jour où les signaux reviennent. Il s'enregistre automatiquement. Tu peux le montrer à ton médecin ou à un proche.")
        Library.planSections.forEach { sec ->
            SectionTitle(sec.title)
            OutlinedTextField(
                texts.value[sec.id] ?: "", { v ->
                    texts.value = texts.value + (sec.id to v)
                    store.update { it.copy(plan = texts.value.filterValues { t -> t.isNotBlank() }) }
                },
                Modifier.fillMaxWidth(), placeholder = { Text(sec.hint) }, minLines = 3,
            )
        }
        Button(onClick = {
            store.update { it.copy(plan = texts.value.filterValues { v -> v.isNotBlank() }) }
            onBack()
        }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
    }
}

// ---------------- Explorer ----------------

@Composable
fun ExploreTab(state: AppState, nav: Nav) {
    Screen("Explorer", actions = { IconButton(onClick = { nav.push("search") }) { Icon(Icons.Filled.Search, "Rechercher") } }) {
        val mine = Library.themes.filter { it.id in state.objectives }
        val others = Library.themes.filter { it.id !in state.objectives }
        if (state.objectives.isEmpty()) ActionCard("Choisir mes objectifs", "Pour que l'appli te propose ce qui compte pour toi", "🎯") { nav.push("objectives") }
        if (mine.isNotEmpty()) SectionTitle("Pour mes objectifs")
        mine.forEach { t -> ActionCard(t.title, t.desc, t.emoji, MaterialTheme.colorScheme.secondaryContainer) { nav.push("theme", t.id) } }
        SectionTitle(if (mine.isEmpty()) "Thèmes" else "Autres thèmes")
        others.forEach { t -> ActionCard(t.title, t.desc, t.emoji) { nav.push("theme", t.id) } }
        SectionTitle("Raccourcis")
        ActionCard("Tous les parcours", "${Content.programs.size} programmes guidés", "🧭") { nav.push("programs") }
        ActionCard("Bilans", "Burn-out, humeur, anxiété", "📋") { nav.push("tests") }
        ActionCard("Habitudes", "${state.habits.count { !it.archived }} habitude(s) suivie(s)", "✅") { nav.push("habits") }
        ActionCard("Mon plan anti-rechute", if (state.plan.isEmpty()) "À rédiger" else "${state.plan.size} / ${Library.planSections.size} sections remplies", "🛡️") { nav.push("plan") }
        if (state.objectives.isNotEmpty()) TextButton(onClick = { nav.push("objectives") }) { Text("Modifier mes objectifs") }
    }
}

@Composable
fun ThemeScreen(state: AppState, store: Store, id: String, nav: Nav, onBack: () -> Unit) {
    val t = Library.theme(id) ?: return onBack()
    Screen("${t.emoji} ${t.title}", onBack) {
        Muted(t.desc)
        SectionTitle("Parcours")
        t.programs.mapNotNull { Content.program(it) }.forEach { p ->
            val n = state.programs[p.id]?.doneDays?.size ?: 0
            ActionCard(p.title, if (n > 0) "$n / ${p.days.size} jours" else p.subtitle, p.emoji) { nav.push("program", p.id) }
        }
        SectionTitle("Exercices")
        t.tools.forEach { (label, r) -> ActionCard(label, null, "▶️") { nav.push(r) } }
        SectionTitle("Bilans")
        t.tests.mapNotNull { Library.test(it) }.forEach { q -> ActionCard(q.name, "${q.items.size} questions", "📋") { nav.push("test", q.id) } }
        SectionTitle("Habitudes suggérées")
        t.habits.forEach { title ->
            val has = state.habits.any { it.title == title && !it.archived }
            val emoji = Library.habitIdeas.firstOrNull { it.second == title }?.first ?: "✅"
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$emoji  $title", Modifier.weight(1f))
                    if (has) Text("Suivie ✓", color = MaterialTheme.colorScheme.primary)
                    else TextButton(onClick = {
                        store.update { s -> s.copy(habits = s.habits.filter { it.title != title } + Habit(Store.newId(), title, emoji)) }
                    }) { Text("Ajouter") }
                }
            }
        }
    }
}

@Composable
fun ObjectivesScreen(state: AppState, store: Store, onBack: () -> Unit) {
    var sel by remember { mutableStateOf(state.objectives) }
    Screen("Mes objectifs", onBack) {
        Muted("Qu'est-ce qui compte le plus pour toi en ce moment ? Plusieurs choix possibles.")
        Library.objectives.forEach { (id, label) ->
            Card(
                onClick = { sel = if (id in sel) sel - id else sel + id },
                modifier = Modifier.fillMaxWidth(),
                colors = if (id in sel) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((Library.theme(id)?.emoji ?: "🎯") + "  " + label, Modifier.weight(1f))
                    if (id in sel) Text("✓", fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(onClick = {
            store.update { s ->
                val first = Library.objectives.map { it.first }.firstOrNull { it in sel }
                s.copy(objectives = sel, activeProgram = s.activeProgram ?: first?.let { Library.theme(it)?.programs?.firstOrNull() })
            }
            onBack()
        }, modifier = Modifier.fillMaxWidth(), enabled = sel.isNotEmpty()) { Text("Valider") }
    }
}

// ---------------- Recherche ----------------

private data class Hit(val title: String, val sub: String, val route: Route)

private fun allHits(): List<Hit> = buildList {
    Content.programs.forEach { p ->
        add(Hit(p.title, "Parcours · " + p.subtitle, Route("program", p.id)))
        p.days.forEachIndexed { i, d -> add(Hit(d.title, "${p.title} · jour ${i + 1} · " + d.lesson, Route("program", p.id))) }
    }
    Library.themes.forEach { add(Hit(it.title, "Thème · " + it.desc, Route("theme", it.id))) }
    Library.tests.forEach { add(Hit(it.name, "Bilan · " + it.intro, Route("test", it.id))) }
    Content.breathPatterns.forEach { add(Hit(it.name, "Respiration · " + it.desc, Route("breath", it.id))) }
    Content.distortions.forEach { add(Hit(it.name, "Piège de pensée · " + it.desc, Route("distortions"))) }
    listOf(
        Hit("Ancrage 5-4-3-2-1", "Exercice · revenir au présent, anxiété", Route("grounding")),
        Hit("Relaxation progressive", "Exercice · tension musculaire, stress", Route("pmr")),
        Hit("Scan corporel", "Exercice · détente, sommeil", Route("bodyscan")),
        Hit("Pause 3 minutes", "Exercice · pleine conscience, travail", Route("pause3")),
        Hit("Lieu ressource", "Visualisation · calme, sécurité", Route("safeplace")),
        Hit("Détente avant le sommeil", "Visualisation · sommeil, endormissement", Route("sleeprelax")),
        Hit("Décompression de fin de journée", "Exercice · burn-out, déconnexion, travail", Route("decompression")),
        Hit("Fiche de pensée", "TCC · restructuration cognitive, pensées", Route("thought")),
        Hit("Activités ressources", "TCC · activation comportementale, énergie", Route("activity")),
        Hit("Mon plan anti-rechute", "Burn-out · signaux d'alerte, limites", Route("plan")),
        Hit("Habitudes", "Suivi quotidien", Route("habits")),
        Hit("Journal", "Écriture, gratitude", Route("journal", "")),
        Hit("Besoin d'aide", "Numéros d'urgence et d'écoute", Route("help")),
    ).forEach { add(it) }
}

private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

@Composable
fun SearchScreen(nav: Nav, onBack: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val hits = remember { allHits() }
    Screen("Rechercher", onBack) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("sommeil, burn-out, respiration…") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) })
        val words = norm(q).split(" ").filter { it.length >= 2 }
        if (words.isNotEmpty()) {
            val res = hits.filter { h -> val t = norm(h.title + " " + h.sub); words.all { it in t } }
                .sortedByDescending { h -> words.count { it in norm(h.title) } }.distinctBy { it.title + it.route }
            if (res.isEmpty()) Muted("Aucun résultat.")
            res.take(40).forEach { h -> ActionCard(h.title, h.sub.take(120), null) { nav.push(h.route) } }
        }
    }
}
