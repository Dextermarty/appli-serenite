package be.dexter.serenite

/** Check-in d'humeur. mood : 1 (très mal) → 5 (très bien). */
data class MoodEntry(
    val id: String,
    val at: Long,
    val mood: Int,
    val emotions: List<String> = emptyList(),
    val factors: List<String> = emptyList(),
    val note: String = "",
)

/** Entrée de journal. kind : "libre", "gratitude" ou "question". */
data class JournalEntry(
    val id: String,
    val at: Long,
    val kind: String,
    val prompt: String = "",
    val text: String,
)

/** Fiche de restructuration cognitive (TCC). Intensités 0..100. */
data class ThoughtRecord(
    val id: String,
    val at: Long,
    val situation: String = "",
    val thought: String = "",
    val emotion: String = "",
    val before: Int = 50,
    val distortions: List<String> = emptyList(),
    val evidenceFor: String = "",
    val evidenceAgainst: String = "",
    val alternative: String = "",
    val after: Int = 50,
)

/** Activation comportementale : activité planifiée pour un jour (epochDay). Notes 0..10. */
data class PlannedActivity(
    val id: String,
    val title: String,
    val day: Long,
    val done: Boolean = false,
    val pleasure: Int = 0,
    val mastery: Int = 0,
)

/** Séance d'exercice réalisée (respiration, ancrage, relaxation…). */
data class Session(val id: String, val at: Long, val tool: String, val seconds: Int)

/** Avancement d'un parcours : jours terminés (index 0..n-1) et jour calendaire du dernier terminé. */
data class ProgramProgress(
    val programId: String,
    val startedDay: Long,
    val doneDays: Set<Int> = emptySet(),
    val lastDoneDay: Long = -1,
)

data class Settings(
    val name: String = "",
    val remindMood: Boolean = true,
    val moodTime: Int = 20 * 60, // minutes depuis minuit
    val remindEvening: Boolean = false,
    val eveningTime: Int = 21 * 60 + 30,
    val remindProgram: Boolean = true,
    val programTime: Int = 8 * 60,
    val oneDayPerDay: Boolean = true, // un jour de parcours débloqué par jour calendaire
    val vibrate: Boolean = true,
)

data class AppState(
    val onboarded: Boolean = false,
    val settings: Settings = Settings(),
    val moods: List<MoodEntry> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val thoughts: List<ThoughtRecord> = emptyList(),
    val activities: List<PlannedActivity> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val programs: Map<String, ProgramProgress> = emptyMap(),
    val activeProgram: String? = null,
)
