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
        s.tests.forEach { add(day(it.at)) }
        s.habits.forEach { addAll(it.days) }
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

    // --- Énergie / charge (prévention burn-out) ---

    /** Moyenne journalière d'un champ 1..5 (0 = ignoré) sur [days] jours. */
    fun dailyField(s: AppState, days: Int, today: Long = today(), f: (MoodEntry) -> Int): List<Pair<Long, Float?>> {
        val byDay = s.moods.filter { f(it) > 0 }.groupBy { day(it.at) }
        return (days - 1 downTo 0).map { k -> (today - k).let { d -> d to byDay[d]?.map(f)?.average()?.toFloat() } }
    }

    /**
     * Signal d'alerte : énergie basse (≤ 2) sur les 3 derniers jours renseignés dans la semaine,
     * ou charge élevée (≥ 4) avec énergie basse, ou énergie moyenne en baisse d'au moins 1 point sur 7 jours.
     */
    fun energyAlert(s: AppState, today: Long = today()): String? {
        val last = dailyField(s, 7, today) { it.energy }.mapNotNull { it.second }
        if (last.size >= 3 && last.takeLast(3).all { it <= 2f }) return "Ton énergie est basse depuis plusieurs jours."
        val load = dailyField(s, 7, today) { it.load }.mapNotNull { it.second }
        if (last.size >= 3 && load.size >= 3 && load.takeLast(3).average() >= 4.0 && last.takeLast(3).average() <= 2.5)
            return "Charge élevée et énergie en baisse : c'est le moment de lever le pied."
        val prev = dailyField(s, 14, today) { it.energy }.take(7).mapNotNull { it.second }
        if (prev.size >= 3 && last.size >= 3 && prev.average() - last.average() >= 1.0)
            return "Ton énergie moyenne a baissé de plus d'un point cette semaine."
        return null
    }

    // --- Habitudes ---

    fun habitStreak(h: Habit, today: Long = today()): Int {
        var d = if (today in h.days) today else today - 1
        var n = 0
        while (d in h.days) { n++; d-- }
        return n
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
