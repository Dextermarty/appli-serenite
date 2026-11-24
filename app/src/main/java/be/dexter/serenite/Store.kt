package be.dexter.serenite

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/** Persistance JSON dans les SharedPreferences, exposée en StateFlow. Tout reste sur le téléphone. */
class Store private constructor(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val prefs = appCtx.getSharedPreferences("serenite", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state

    fun update(f: (AppState) -> AppState) = synchronized(this) {
        val old = _state.value
        val s = f(old)
        save(s)
        _state.value = s
        if (s.settings != old.settings || s.moods.size != old.moods.size || s.programs != old.programs) {
            runCatching { Reminders.schedule(appCtx) }
        }
    }

    /** Remplace tout l'état (import d'une sauvegarde). */
    fun replace(s: AppState) = update { s }

    private fun save(s: AppState) {
        prefs.edit().putString(KEY, StateJson.toJson(s)).apply()
    }

    private fun load(): AppState {
        val raw = prefs.getString(KEY, null) ?: return AppState()
        return try {
            StateJson.fromJson(raw)
        } catch (e: Exception) {
            android.util.Log.e("Serenite", "Lecture de l'état impossible", e)
            prefs.edit().putString(KEY + "_backup_" + System.currentTimeMillis(), raw).apply()
            AppState()
        }
    }

    companion object {
        private const val KEY = "state"
        fun newId(): String = UUID.randomUUID().toString()

        @Volatile private var inst: Store? = null
        fun get(ctx: Context): Store = inst ?: synchronized(this) { inst ?: Store(ctx).also { inst = it } }
    }
}
