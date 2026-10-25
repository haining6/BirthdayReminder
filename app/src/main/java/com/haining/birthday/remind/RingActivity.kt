package com.haining.birthday.remind

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable

/** 来电式全屏提醒界面：锁屏也能弹出，铃声由通知负责，点按钮即停止。 */
class RingActivity : ComponentActivity() {

    private val info = mutableStateOf(RingInfo(0, "", "", ""))
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        info.value = RingInfo.from(intent)
        setContent {
            val i by info
            RingScreen(i, onSnooze = ::snooze, onDismiss = ::dismiss)
        }
        handler.postDelayed({ finish() }, Notifier.RING_MILLIS)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        info.value = RingInfo.from(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dismiss() {
        Notifier.cancel(this, info.value.id)
        finish()
    }

    private fun snooze() {
        Notifier.cancel(this, info.value.id)
        Scheduler.snooze(applicationContext, info.value)
        finish()
    }
}

@Composable
private fun RingScreen(info: RingInfo, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A1A22), Color(0xFF7A2E3A), Color(0xFFE2545C))))
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 96.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("重要日子提醒", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier.size(132.dp).scale(scale).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(104.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(info.avatar, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE2545C))
                }
            }
            Spacer(Modifier.height(36.dp))
            Text(info.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(info.text, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
        }
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 48.dp, vertical = 72.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundButton(Icons.Filled.Refresh, "10分钟后", Color.White.copy(alpha = 0.22f), onSnooze)
            RoundButton(Icons.Filled.Check, "知道了", Color(0xFF34C759), onDismiss)
        }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(76.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
