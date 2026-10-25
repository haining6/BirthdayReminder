package com.haining.birthday.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.haining.birthday.data.Event
import com.haining.birthday.data.Prefs
import com.haining.birthday.data.Repo
import com.haining.birthday.remind.Notifier
import com.haining.birthday.remind.Scheduler
import java.time.LocalDate
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    /** 每次回到前台 +1，用来刷新“今天”和权限状态 */
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannels(this)
        val app = applicationContext
        thread { Scheduler.ensure(app) }
        setContent {
            AppTheme { App(tick.intValue) }
        }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

private sealed interface Screen {
    data object Home : Screen
    data class Edit(val event: Event?) : Screen
    data object Settings : Screen
}

@Composable
private fun App(tick: Int) {
    val ctx = LocalContext.current
    var events by remember { mutableStateOf(Repo.load(ctx)) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val today = remember(tick) { LocalDate.now() }
    val listState = rememberLazyListState()

    // 首次打开时请求通知权限（Android 13+）
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val p = Prefs(ctx)
        if (Build.VERSION.SDK_INT >= 33 && !p.askedNotification &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            p.askedNotification = true
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun update(list: List<Event>) {
        events = list
        Repo.save(ctx, list)
    }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (targetState == Screen.Home) {
                (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 5 } + fadeOut())
            }
        },
        label = "screen",
    ) { s ->
        when (s) {
            Screen.Home -> HomeScreen(
                events = events,
                today = today,
                listState = listState,
                onAdd = { screen = Screen.Edit(null) },
                onOpen = { screen = Screen.Edit(it) },
                onSettings = { screen = Screen.Settings },
            )
            is Screen.Edit -> EditScreen(
                initial = s.event,
                today = today,
                onBack = { screen = Screen.Home },
                onSave = { e ->
                    update(events.filter { it.id != e.id } + e)
                    screen = Screen.Home
                },
                onDelete = { e ->
                    update(events.filter { it.id != e.id })
                    screen = Screen.Home
                },
            )
            Screen.Settings -> SettingsScreen(
                tick = tick,
                events = events,
                onBack = { screen = Screen.Home },
                onEventsChanged = { events = it },
            )
        }
    }
}
