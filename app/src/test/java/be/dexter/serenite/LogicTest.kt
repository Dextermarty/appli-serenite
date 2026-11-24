package be.dexter.serenite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LogicTest {
    private val zone = ZoneId.of("Europe/Brussels")
    init { Stats.zone = zone }

    private fun at(day: Long, hour: Int = 12) = LocalDate.ofEpochDay(day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private val today = LocalDate.of(2026, 11, 24).toEpochDay()

    private fun mood(day: Long, v: Int, fac: List<String> = emptyList(), emo: List<String> = emptyList()) =
        MoodEntry("m$day$v${fac.size}", at(day), v, emo, fac)

    @Test fun streakCountsConsecutiveDays() {
        val s = AppState(moods = listOf(mood(today, 3), mood(today - 1, 4), mood(today - 2, 2), mood(today - 4, 5)))
        assertEquals(3, Stats.streak(s, today))
    }

    @Test fun streakStillCountsWhenTodayEmpty() {
        val s = AppState(moods = listOf(mood(today - 1, 4), mood(today - 2, 2)))
        assertEquals(2, Stats.streak(s, today))
        assertEquals(0, Stats.streak(AppState(), today))
    }

    @Test fun streakIncludesOtherPractices() {
        val s = AppState(
            moods = listOf(mood(today, 3)),
            sessions = listOf(Session("s", at(today - 1), "breath:coherence", 300)),
            activities = listOf(PlannedActivity("a", "Marcher", today - 2, done = true)),
        )
        assertEquals(3, Stats.streak(s, today))
    }

    @Test fun dailyAveragesAndGaps() {
        val s = AppState(moods = listOf(mood(today, 2), MoodEntry("x", at(today, 20), 4), mood(today - 2, 5)))
        val l = Stats.dailyAverages(s, 3, today)
        assertEquals(listOf(today - 2, today - 1, today), l.map { it.first })
        assertEquals(5f, l[0].second)
        assertNull(l[1].second)
        assertEquals(3f, l[2].second)
        assertEquals(11f / 3, Stats.average(s, 7, today)!!, 0.001f)
    }

    @Test fun factorImpactNeedsThreeOccurrences() {
        val s = AppState(moods = listOf(
            mood(today, 5, listOf("Sport")), mood(today - 1, 5, listOf("Sport")), mood(today - 2, 5, listOf("Sport")),
            mood(today - 3, 1, listOf("Travail")), mood(today - 4, 1, listOf("Travail")),
        ))
        val impact = Stats.factorImpact(s)
        assertEquals(1, impact.size)
        assertEquals("Sport", impact[0].first)
        assertEquals(1.6f, impact[0].second, 0.01f) // 5 - 3.4
    }

    @Test fun topEmotions() {
        val s = AppState(moods = listOf(mood(today, 3, emo = listOf("Stress", "Fatigue")), mood(today - 1, 3, emo = listOf("Stress"))))
        assertEquals("Stress" to 2, Stats.topEmotions(s, 7, today = today).first())
    }

    @Test fun programUnlockOneDayPerDay() {
        assertTrue(Stats.isUnlocked(null, 0, true, today))
        assertFalse(Stats.isUnlocked(null, 1, true, today))
        val p = Stats.complete(null, "stress", 0, today)
        assertFalse(Stats.isUnlocked(p, 1, true, today)) // même jour : bloqué
        assertTrue(Stats.isUnlocked(p, 1, false, today)) // mode libre
        assertTrue(Stats.isUnlocked(p, 1, true, today + 1)) // lendemain
        assertFalse(Stats.isUnlocked(p, 2, true, today + 1))
        assertEquals(1, Stats.nextDay(p, 7))
        val all = (0 until 7).fold<Int, ProgramProgress?>(null) { acc, i -> Stats.complete(acc, "stress", i, today + i) }
        assertNull(Stats.nextDay(all, 7))
    }

    @Test fun jsonRoundTrip() {
        val s = AppState(
            onboarded = true,
            settings = Settings(name = "Dexter", remindEvening = true, moodTime = 19 * 60, oneDayPerDay = false, lock = true),
            moods = listOf(MoodEntry("m", 1000L, 4, listOf("Calme"), listOf("Sport"), "note \"guillemets\"\nligne", 2, 4)),
            journal = listOf(JournalEntry("j", 2000L, "question", "Q ?", "Réponse é à")),
            thoughts = listOf(ThoughtRecord("t", 3000L, "sit", "pensée", "Stress", 80, listOf("catastrophe"), "p", "c", "alt", 40)),
            activities = listOf(PlannedActivity("a", "Marcher", 20000, true, 7, 5)),
            sessions = listOf(Session("s", 4000L, "pmr", 360)),
            programs = mapOf("stress" to ProgramProgress("stress", 20000, setOf(0, 1), 20001)),
            activeProgram = "stress",
            habits = listOf(Habit("h", "Marcher", "🚶", setOf(20000, 20001), false)),
            tests = listOf(TestResult("r", 5000L, "bat12", List(12) { 3 }, 3f)),
            objectives = setOf("burnout", "sommeil"),
            plan = mapOf("signaux" to "Sommeil léger\nIrritabilité"),
        )
        assertEquals(s, StateJson.fromJson(StateJson.toJson(s)))
    }

    @Test fun jsonToleratesMissingFields() {
        val s = StateJson.fromJson("{}")
        assertEquals(AppState(), s)
    }

    private val kinds = setOf("mood", "journal", "thought", "distortions", "breath", "grounding", "pmr", "bodyscan", "activity",
        "test", "plan", "habits", "decompression", "pause3", "safeplace", "sleeprelax")

    @Test fun questionnaires() {
        assertEquals(12, Library.bat12.items.size)
        assertEquals(9, Library.phq9.items.size)
        assertEquals(7, Library.gad7.items.size)
        assertEquals(2.5f, Library.bat12.score(List(6) { 2 } + List(6) { 3 }), 0.001f)
        assertEquals(0, Library.bat12.band(2.53f))
        assertEquals(1, Library.bat12.band(2.54f))
        assertEquals(2, Library.bat12.band(2.96f))
        assertEquals(2, Library.phq9.band(Library.phq9.score(List(9) { 1 } + listOf()) + 1f)) // 10 → modéré
        assertEquals(3, Library.gad7.band(15f))
        Library.bat12.subscales.flatMap { it.second }.sorted().let { assertEquals((0..11).toList(), it) }
    }

    @Test fun energyAlert() {
        fun e(day: Long, en: Int, ld: Int = 0) = MoodEntry("e$day", at(day), 3, energy = en, load = ld)
        assertNull(Stats.energyAlert(AppState(moods = listOf(e(today, 4), e(today - 1, 4), e(today - 2, 3))), today))
        assertTrue(Stats.energyAlert(AppState(moods = listOf(e(today, 2), e(today - 1, 1), e(today - 2, 2))), today) != null)
        val drop = (7..13).map { e(today - it, 4) } + (0..2).map { e(today - it, 3) }
        assertTrue(Stats.energyAlert(AppState(moods = drop), today) != null)
    }

    @Test fun habitStreak() {
        val h = Habit("h", "Marcher", days = setOf(today - 1, today - 2, today - 4))
        assertEquals(2, Stats.habitStreak(h, today))
        assertEquals(3, Stats.habitStreak(h.copy(days = h.days + today), today))
    }

    @Test fun contentIsConsistent() {
        Content.programs.forEach { p ->
            assertTrue(p.days.size >= 7)
            p.days.forEach { d ->
                val k = d.link?.kind ?: return@forEach
                assertTrue("$k inconnu", k in kinds)
                if (k == "test") assertTrue(Library.test(d.link.arg) != null)
                if (k == "breath") assertTrue(Content.breathPatterns.any { it.id == d.link.arg })
            }
        }
        assertEquals(12, Content.distortions.map { it.id }.toSet().size)
        assertEquals(5, Content.programs.size)
        Library.themes.forEach { t ->
            t.programs.forEach { assertTrue(Content.program(it) != null) }
            t.tests.forEach { assertTrue(Library.test(it) != null) }
            t.tools.forEach { (_, r) -> assertTrue(r.kind, r.kind in kinds) }
            t.habits.forEach { h -> assertTrue(h, Library.habitIdeas.any { it.second == h }) }
        }
        Library.objectives.forEach { assertTrue(Library.theme(it.first) != null) }
        assertTrue(Content.promptOfDay(-5).isNotBlank())
    }
}
