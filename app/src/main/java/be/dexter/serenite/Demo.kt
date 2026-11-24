package be.dexter.serenite

/**
 * Données de démonstration pour les captures automatiques sur émulateur.
 * Utilisé uniquement dans les builds debug (voir MainActivity), jamais dans l'APK publié.
 */
object Demo {
    fun state(now: Long = System.currentTimeMillis()): AppState {
        val today = Stats.today()
        val day = 86_400_000L
        fun at(k: Int, h: Int = 20) = now - k * day - (now % day) + h * 3_600_000L
        val moods = (0 until 21).map { k ->
            val v = listOf(4, 3, 3, 2, 3, 4, 3, 2, 2, 3, 4, 4, 3, 3, 4, 2, 3, 3, 4, 3, 4)[k]
            MoodEntry("m$k", at(k), v,
                if (v <= 2) listOf("Fatigue", "Stress") else if (v == 3) listOf("Calme", "Fatigue") else listOf("Calme", "Gratitude"),
                if (k % 3 == 0) listOf("Travail", "Sommeil") else if (k % 3 == 1) listOf("Sport") else listOf("Famille"),
                if (k == 0) "Longue journée, réunion tendue l'après-midi." else "",
                energy = listOf(2, 2, 2, 3, 3, 4, 3, 3, 4, 4, 4, 3, 4, 4, 3, 3, 4, 4, 3, 4, 4)[k],
                load = listOf(4, 5, 4, 4, 3, 3, 3, 4, 3, 2, 3, 3, 2, 3, 3, 4, 3, 2, 3, 3, 2)[k])
        }
        return AppState(
            onboarded = true,
            settings = Settings(name = "Dexter", lock = false),
            moods = moods,
            journal = listOf(
                JournalEntry("j1", at(0, 21), "gratitude", "", "• Un café au calme ce matin\n• Un collègue qui m'a aidé\n• La marche du retour"),
                JournalEntry("j2", at(2, 21), "question", Content.journalPrompts[3], "La réunion de lundi. Ce qui dépendait de moi : préparer mes chiffres, le reste non."),
            ),
            thoughts = listOf(ThoughtRecord("t1", at(1, 18), "Remarque du chef sur mon rapport", "Je n'y arrive plus, je vais tout rater", "Anxiété",
                80, listOf("catastrophe", "generalisation"), "Le rapport avait deux erreurs", "Le reste a été validé ; on m'a confié le dossier suivant",
                "Deux corrections à faire, pas un échec global.", 40)),
            activities = listOf(PlannedActivity("a1", "Marcher 20 min", today, false), PlannedActivity("a2", "Appeler Marc", today - 1, true, 7, 5)),
            sessions = (0 until 6).map { Session("s$it", at(it, 12), "breath:coherence", 300) },
            programs = mapOf("burnout" to ProgramProgress("burnout", today - 3, setOf(0, 1, 2), today - 1)),
            activeProgram = "burnout",
            habits = listOf(
                Habit("h1", "Marcher 20 minutes", "🚶", setOf(today, today - 1, today - 3, today - 4)),
                Habit("h2", "Pas de mails pro après 19 h", "📵", setOf(today - 1, today - 2)),
                Habit("h3", "Vraie pause de midi, sans écran", "🥪", setOf(today, today - 2)),
            ),
            tests = listOf(
                TestResult("b1", at(20), "bat12", List(12) { if (it < 3) 4 else 3 }, 3.25f),
                TestResult("b2", at(1), "bat12", List(12) { if (it < 3) 3 else 2 }, 2.25f + 0.5f),
            ),
            objectives = setOf("burnout", "sommeil"),
            plan = mapOf("signaux" to "Je dors mal, je relis mes mails le soir, je deviens irritable avec mes proches.",
                "limites" to "Pas de mails après 19 h. Pause de midi réelle."),
        )
    }
}
