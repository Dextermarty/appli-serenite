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
            settings = Settings(name = "Dexter", remindEvening = true, moodTime = 19 * 60, oneDayPerDay = false),
            moods = listOf(MoodEntry("m", 1000L, 4, listOf("Calme"), listOf("Sport"), "note \"guillemets\"\nligne")),
            journal = listOf(JournalEntry("j", 2000L, "question", "Q ?", "Réponse é à")),
            thoughts = listOf(ThoughtRecord("t", 3000L, "sit", "pensée", "Stress", 80, listOf("catastrophe"), "p", "c", "alt", 40)),
            activities = listOf(PlannedActivity("a", "Marcher", 20000, true, 7, 5)),
            sessions = listOf(Session("s", 4000L, "pmr", 360)),
            programs = mapOf("stress" to ProgramProgress("stress", 20000, setOf(0, 1), 20001)),
            activeProgram = "stress",
        )
        assertEquals(s, StateJson.fromJson(StateJson.toJson(s)))
    }

    @Test fun jsonToleratesMissingFields() {
        val s = StateJson.fromJson("{}")
        assertEquals(AppState(), s)
    }

    @Test fun contentIsConsistent() {
        Content.programs.forEach { p ->
            assertEquals(7, p.days.size)
            p.days.forEach { d ->
                val k = d.link?.kind ?: return@forEach
                assertTrue("$k inconnu", k in setOf("mood", "journal", "thought", "distortions", "breath", "grounding", "pmr", "bodyscan", "activity"))
                if (k == "breath") assertTrue(Content.breathPatterns.any { it.id == d.link.arg })
            }
        }
        assertEquals(12, Content.distortions.map { it.id }.toSet().size)
        assertTrue(Content.promptOfDay(-5).isNotBlank())
    }
}
