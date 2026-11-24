package be.dexter.serenite

import org.json.JSONArray
import org.json.JSONObject

/** Sérialisation JSON de l'état (pure, testable hors Android). Sert aussi à l'export/import. */
object StateJson {
    const val VERSION = 1

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun <T> JSONArray?.objs(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { i -> runCatching { f(getJSONObject(i)) }.getOrNull() }

    private fun strs(l: Collection<String>) = JSONArray().apply { l.forEach { put(it) } }

    fun fromJson(raw: String): AppState {
        val o = JSONObject(raw)
        val st = o.optJSONObject("settings") ?: JSONObject()
        val def = Settings()
        val settings = Settings(
            name = st.optString("name", ""),
            remindMood = st.optBoolean("remindMood", def.remindMood),
            moodTime = st.optInt("moodTime", def.moodTime),
            remindEvening = st.optBoolean("remindEvening", def.remindEvening),
            eveningTime = st.optInt("eveningTime", def.eveningTime),
            remindProgram = st.optBoolean("remindProgram", def.remindProgram),
            programTime = st.optInt("programTime", def.programTime),
            oneDayPerDay = st.optBoolean("oneDayPerDay", def.oneDayPerDay),
            vibrate = st.optBoolean("vibrate", def.vibrate),
            lock = st.optBoolean("lock", def.lock),
        )
        val programs = o.optJSONArray("programs").objs { p ->
            val a = p.optJSONArray("done")
            ProgramProgress(
                p.getString("id"), p.optLong("start"),
                if (a == null) emptySet() else (0 until a.length()).map { a.getInt(it) }.toSet(),
                p.optLong("last", -1),
            )
        }.associateBy { it.programId }
        return AppState(
            onboarded = o.optBoolean("onboarded", false),
            settings = settings,
            moods = o.optJSONArray("moods").objs { m ->
                MoodEntry(m.getString("id"), m.getLong("at"), m.getInt("mood").coerceIn(1, 5),
                    m.optJSONArray("emo").strings(), m.optJSONArray("fac").strings(), m.optString("note"))
            },
            journal = o.optJSONArray("journal").objs { j ->
                JournalEntry(j.getString("id"), j.getLong("at"), j.optString("kind", "libre"), j.optString("prompt"), j.optString("text"))
            },
            thoughts = o.optJSONArray("thoughts").objs { t ->
                ThoughtRecord(
                    t.getString("id"), t.getLong("at"), t.optString("sit"), t.optString("th"), t.optString("emo"),
                    t.optInt("before", 50), t.optJSONArray("dist").strings(), t.optString("for"), t.optString("against"),
                    t.optString("alt"), t.optInt("after", 50),
                )
            },
            activities = o.optJSONArray("activities").objs { a ->
                PlannedActivity(a.getString("id"), a.getString("title"), a.getLong("day"), a.optBoolean("done"), a.optInt("pl"), a.optInt("ma"))
            },
            sessions = o.optJSONArray("sessions").objs { s ->
                Session(s.getString("id"), s.getLong("at"), s.getString("tool"), s.optInt("sec"))
            },
            programs = programs,
            activeProgram = o.optString("active", "").ifBlank { null },
        )
    }

    fun toJson(s: AppState): String {
        val o = JSONObject()
        o.put("v", VERSION)
        o.put("onboarded", s.onboarded)
        val st = s.settings
        o.put("settings", JSONObject()
            .put("name", st.name)
            .put("remindMood", st.remindMood).put("moodTime", st.moodTime)
            .put("remindEvening", st.remindEvening).put("eveningTime", st.eveningTime)
            .put("remindProgram", st.remindProgram).put("programTime", st.programTime)
            .put("oneDayPerDay", st.oneDayPerDay).put("vibrate", st.vibrate).put("lock", st.lock))
        o.put("moods", JSONArray().apply {
            s.moods.forEach {
                put(JSONObject().put("id", it.id).put("at", it.at).put("mood", it.mood)
                    .put("emo", strs(it.emotions)).put("fac", strs(it.factors)).put("note", it.note))
            }
        })
        o.put("journal", JSONArray().apply {
            s.journal.forEach {
                put(JSONObject().put("id", it.id).put("at", it.at).put("kind", it.kind).put("prompt", it.prompt).put("text", it.text))
            }
        })
        o.put("thoughts", JSONArray().apply {
            s.thoughts.forEach {
                put(JSONObject().put("id", it.id).put("at", it.at).put("sit", it.situation).put("th", it.thought)
                    .put("emo", it.emotion).put("before", it.before).put("dist", strs(it.distortions))
                    .put("for", it.evidenceFor).put("against", it.evidenceAgainst).put("alt", it.alternative).put("after", it.after))
            }
        })
        o.put("activities", JSONArray().apply {
            s.activities.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title).put("day", it.day).put("done", it.done)
                    .put("pl", it.pleasure).put("ma", it.mastery))
            }
        })
        o.put("sessions", JSONArray().apply {
            s.sessions.forEach { put(JSONObject().put("id", it.id).put("at", it.at).put("tool", it.tool).put("sec", it.seconds)) }
        })
        o.put("programs", JSONArray().apply {
            s.programs.values.forEach {
                put(JSONObject().put("id", it.programId).put("start", it.startedDay)
                    .put("done", JSONArray().apply { it.doneDays.sorted().forEach { d -> put(d) } }).put("last", it.lastDoneDay))
            }
        })
        s.activeProgram?.let { o.put("active", it) }
        return o.toString()
    }
}
