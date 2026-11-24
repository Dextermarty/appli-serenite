package be.dexter.serenite

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Calculs purs (testables hors Android). */
object Stats {
    var zone: ZoneId = ZoneId.systemDefault()

    fun day(at: Long): Long = Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toEpochDay()
    fun today(): Long = LocalDate.now(zone).toEpochDay()

    /** Jours (epochDay) où au moins une pratique a été faite. */
    fun activeDays(s: AppState): Set<Long> = buildSet {
        s.moods.forEach { add(day(it.at)) }
        s.journal.forEach { add(day(it.at)) }
        s.thoughts.forEach { add(day(it.at)) }
        s.sessions.forEach { add(day(it.at)) }
        s.activities.filter { it.done }.forEach { add(it.day) }
        s.programs.values.forEach { if (it.lastDoneDay >= 0) add(it.lastDoneDay) }
    }

    /** Série de jours consécutifs actifs, se terminant aujourd'hui (ou hier si rien encore aujourd'hui). */
    fun streak(s: AppState, today: Long = today()): Int {
        val days = activeDays(s)
        var d = if (today in days) today else today - 1
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }

    fun moodToday(s: AppState, today: Long = today()): MoodEntry? =
        s.moods.filter { day(it.at) == today }.maxByOrNull { it.at }

    /** Moyenne par jour sur les [days] derniers jours (null = pas de check-in ce jour). */
    fun dailyAverages(s: AppState, days: Int, today: Long = today()): List<Pair<Long, Float?>> {
        val byDay = s.moods.groupBy { day(it.at) }
        return (days - 1 downTo 0).map { k ->
            val d = today - k
            d to byDay[d]?.map { it.mood }?.average()?.toFloat()
        }
    }

    fun average(s: AppState, days: Int, today: Long = today()): Float? {
        val from = today - days + 1
        val l = s.moods.filter { day(it.at) in from..today }
        return if (l.isEmpty()) null else l.map { it.mood }.average().toFloat()
    }

    /** Émotions les plus fréquentes sur la période. */
    fun topEmotions(s: AppState, days: Int, n: Int = 5, today: Long = today()): List<Pair<String, Int>> {
        val from = today - days + 1
        return s.moods.filter { day(it.at) in from..today }.flatMap { it.emotions }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(n).map { it.key to it.value }
    }

    /**
     * Impact de chaque facteur : moyenne d'humeur quand il est présent moins moyenne globale.
     * Seuls les facteurs présents dans au moins [minCount] check-ins sont retenus.
     */
    fun factorImpact(s: AppState, minCount: Int = 3): List<Pair<String, Float>> {
        if (s.moods.isEmpty()) return emptyList()
        val all = s.moods.map { it.mood }.average()
        return s.moods.flatMap { m -> m.factors.map { it to m.mood } }
            .groupBy({ it.first }, { it.second })
            .filter { it.value.size >= minCount }
            .map { (f, l) -> f to ((l.average() - all) * 10).roundToInt() / 10f }
            .sortedByDescending { it.second }
    }

    /** Nombre de minutes de pratique (séances) sur la période. */
    fun practiceMinutes(s: AppState, days: Int, today: Long = today()): Int {
        val from = today - days + 1
        return s.sessions.filter { day(it.at) in from..today }.sumOf { it.seconds } / 60
    }

    // --- Parcours ---

    /** Le jour [index] du parcours est-il accessible ? */
    fun isUnlocked(p: ProgramProgress?, index: Int, oneDayPerDay: Boolean, today: Long = today()): Boolean {
        if (index == 0) return true
        if (p == null) return false
        if (index in p.doneDays) return true
        if ((index - 1) !in p.doneDays) return false
        // Le jour précédent est fait : si « un jour par jour », attendre le lendemain de la dernière validation.
        return !oneDayPerDay || p.lastDoneDay < today
    }

    /** Prochain jour à faire (null = parcours terminé). */
    fun nextDay(p: ProgramProgress?, length: Int): Int? =
        (0 until length).firstOrNull { p == null || it !in p.doneDays }

    fun complete(p: ProgramProgress?, programId: String, index: Int, today: Long = today()): ProgramProgress {
        val base = p ?: ProgramProgress(programId, today)
        return base.copy(doneDays = base.doneDays + index, lastDoneDay = today)
    }
}
