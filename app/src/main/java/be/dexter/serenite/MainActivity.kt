package be.dexter.serenite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


/** Navigation simple : un onglet + une pile d'écrans par-dessus. */
class Nav {
    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Route>()
    fun push(r: Route) { stack.add(r) }
    fun push(kind: String, arg: String = "") = push(Route(kind, arg))
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(r: Route) { pop(); push(r) }

    /** Ouvre l'outil ciblé par une leçon de parcours. */
    fun open(link: ToolLink) = when (link.kind) {
        "mood" -> push("mood")
        "journal" -> push("journal", if (link.arg.isBlank()) "" else "q:" + link.arg)
        else -> push(link.kind, link.arg)
    }
}

class MainActivity : ComponentActivity() {
    private val openRequest = mutableStateOf<String?>(null)
    private val locked = mutableStateOf(false)
    private val lockMessage = mutableStateOf<String?>(null)
    private var stoppedAt = 0L
    private var prompting = false
    private var autoPrompted = false

    private fun lockOn() = Store.get(this).state.value.settings.lock && Lock.available(this)

    /** Demande l'empreinte / le code. auto = lancé à l'ouverture (une seule fois par verrouillage). */
    fun unlock(auto: Boolean = false) {
        if (!locked.value || prompting || (auto && autoPrompted)) return
        if (!lockOn()) { locked.value = false; return }
        prompting = true
        autoPrompted = true
        Lock.prompt(this, "Déverrouille pour continuer",
            onOk = { prompting = false; stoppedAt = System.currentTimeMillis(); lockMessage.value = null; locked.value = false },
            onFail = { prompting = false; lockMessage.value = it })
    }

    override fun onStart() {
        super.onStart()
        // Reverrouille après 30 s passées hors de l'appli (ou au premier lancement).
        if (lockOn() && (stoppedAt == 0L || System.currentTimeMillis() - stoppedAt > 30_000)) {
            if (!locked.value) autoPrompted = false
            locked.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        unlock(auto = true)
    }

    override fun onStop() {
        super.onStop()
        if (!prompting) stoppedAt = System.currentTimeMillis()
    }
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        Reminders.schedule(this)
        runCatching { Widget.refresh(this) }
        setContent {
            AppTheme {
                if (locked.value) LockScreen(lockMessage.value) { unlock() }
                else AppRoot(openRequest.value) { openRequest.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        // Captures automatiques (build debug uniquement) : données de démo et ouverture directe d'un écran.
        if (BuildConfig.DEBUG && i != null) {
            if (i.getBooleanExtra("demo", false)) Store.get(this).replace(Demo.state())
            i.getStringExtra("shot")?.let { openRequest.value = it }
        }
        val o = i?.getStringExtra(Reminders.EXTRA_OPEN) ?: return
        i.removeExtra(Reminders.EXTRA_OPEN)
        openRequest.value = o
    }
}

private val Teal = Color(0xFF2E7D74)
private val Lavender = Color(0xFF7E6FB0)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFF7FD3C7), onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF1B5149), onPrimaryContainer = Color(0xFFB3F0E6),
        secondary = Color(0xFFC9BEFF), secondaryContainer = Color(0xFF473D74), onSecondaryContainer = Color(0xFFE6DEFF),
        tertiaryContainer = Color(0xFF5B4426), onTertiaryContainer = Color(0xFFFFDDB4),
    ) else lightColorScheme(
        primary = Teal, onPrimary = Color.White,
        primaryContainer = Color(0xFFCDEFE9), onPrimaryContainer = Color(0xFF00201B),
        secondary = Lavender, secondaryContainer = Color(0xFFE6DEFF), onSecondaryContainer = Color(0xFF21184A),
        tertiaryContainer = Color(0xFFFFE8CC), onTertiaryContainer = Color(0xFF2B1700),
        background = Color(0xFFF7FAF9), surface = Color(0xFFF7FAF9),
        surfaceVariant = Color(0xFFE4ECEA),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun AppRoot(openRequest: String?, consumed: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val state by store.state.collectAsState()
    val nav = remember { Nav() }

    LaunchedEffect(openRequest) {
        when (openRequest) {
            "mood" -> { nav.stack.clear(); nav.push("mood") }
            "mood:1", "mood:2", "mood:3", "mood:4", "mood:5" -> { nav.stack.clear(); nav.push("mood", "new:" + openRequest.removePrefix("mood:")) }
            "journal" -> { nav.stack.clear(); nav.tab = 2; nav.push("journal", "g:") }
            "program" -> { nav.stack.clear(); state.activeProgram?.let { nav.push("program", it) } }
            else -> when {
                openRequest == null -> {}
                openRequest.startsWith("tab:") -> { nav.stack.clear(); nav.tab = openRequest.removePrefix("tab:").toIntOrNull() ?: 0 }
                openRequest.startsWith("route:") -> {
                    val p = openRequest.removePrefix("route:").split("|", limit = 2)
                    nav.stack.clear(); nav.push(p[0], p.getOrElse(1) { "" })
                }
            }
        }
        if (openRequest != null) consumed()
    }

    if (!state.onboarded) {
        OnboardingScreen(store)
        return
    }

    BackHandler(enabled = nav.stack.isNotEmpty()) { nav.pop() }
    BackHandler(enabled = nav.stack.isEmpty() && nav.tab != 0) { nav.tab = 0 }

    Scaffold(
        bottomBar = {
            if (nav.stack.isEmpty()) NavigationBar {
                NavigationBarItem(nav.tab == 0, { nav.tab = 0 }, { Icon(Icons.Filled.Home, null) }, label = { Text("Accueil") })
                NavigationBarItem(nav.tab == 1, { nav.tab = 1 }, { Icon(Icons.Filled.Favorite, null) }, label = { Text("Humeur") })
                NavigationBarItem(nav.tab == 2, { nav.tab = 2 }, { Icon(Icons.Filled.Create, null) }, label = { Text("Journal") })
                NavigationBarItem(nav.tab == 3, { nav.tab = 3 }, { Icon(Icons.Filled.PlayArrow, null) }, label = { Text("Exercices") })
                NavigationBarItem(nav.tab == 4, { nav.tab = 4 }, { Icon(Icons.Filled.Search, null) }, label = { Text("Explorer") })
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad)) {
            val top = nav.stack.lastOrNull()
            if (top != null) Destination(top, state, store, nav)
            else when (nav.tab) {
                0 -> HomeScreen(state, store, nav)
                1 -> MoodTab(state, store, nav)
                2 -> JournalTab(state, nav)
                3 -> ToolsTab(state, nav)
                else -> ExploreTab(state, nav)
            }
        }
    }
}

@Composable
fun Destination(r: Route, state: AppState, store: Store, nav: Nav) {
    val back = { nav.pop() }
    when (r.kind) {
        "mood" -> MoodEditor(state, store, r.arg, back)
        "journal" -> JournalEditor(state, store, r.arg, back)
        "thought" -> ThoughtEditor(state, store, r.arg, back, nav)
        "thoughts" -> ThoughtList(state, nav, back)
        "distortions" -> DistortionsScreen(back)
        "breath" -> BreathingScreen(state, store, r.arg, back)
        "grounding" -> GuidedScreen("Ancrage 5-4-3-2-1", "grounding", Content.grounding, state, store, back)
        "pmr" -> GuidedScreen("Relaxation progressive", "pmr", Content.pmr, state, store, back)
        "bodyscan" -> GuidedScreen("Scan corporel", "bodyscan", Content.bodyScan, state, store, back)
        "activity" -> ActivitiesScreen(state, store, back)
        "program" -> ProgramScreen(state, store, r.arg, nav, back)
        "programDay" -> ProgramDayScreen(state, store, r.arg, nav, back)
        "settings" -> SettingsScreen(state, store, nav, back)
        "help" -> HelpScreen(back)
        "programs" -> ProgramsTab(state, store, nav, back)
        "tests" -> TestsScreen(state, nav, back)
        "test" -> TestScreen(state, store, r.arg, nav, back)
        "habits" -> HabitsScreen(state, store, back)
        "plan" -> PlanScreen(state, store, back)
        "theme" -> ThemeScreen(state, store, r.arg, nav, back)
        "objectives" -> ObjectivesScreen(state, store, back)
        "search" -> SearchScreen(nav, back)
        "pause3" -> GuidedScreen("Pause 3 minutes", "pause3", Library.pause3, state, store, back)
        "safeplace" -> GuidedScreen("Lieu ressource", "safeplace", Library.safePlace, state, store, back)
        "sleeprelax" -> GuidedScreen("Détente avant le sommeil", "sleeprelax", Library.sleepRelax, state, store, back)
        "decompression" -> GuidedScreen("Décompression de fin de journée", "decompression", Library.decompression, state, store, back)
        else -> LaunchedEffect(r) { nav.pop() }
    }
}
