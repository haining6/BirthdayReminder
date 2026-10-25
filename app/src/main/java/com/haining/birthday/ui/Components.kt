package com.haining.birthday.ui

import android.content.Context
import android.os.Build
import android.widget.NumberPicker
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.haining.birthday.data.EventType
import com.haining.birthday.remind.avatarOf

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

fun hhmm(h: Int, m: Int) = "%02d:%02d".format(h, m)

@Composable
fun Avatar(name: String, type: EventType, size: Dp = 46.dp) {
    val c = typeColor(type)
    Box(
        Modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(avatarOf(name), color = c, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
fun Tag(text: String, type: EventType) {
    val c = typeColor(type)
    Text(
        text,
        color = c,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.13f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** 带标题的圆角分组卡片 */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 6.dp),
        )
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 6.dp),
    )
}

val OFFSET_CHOICES = listOf(0, 1, 2, 3, 5, 7, 10, 15, 30)

fun offsetLabel(d: Int) = if (d == 0) "当天" else "提前${d}天"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OffsetChips(selected: List<Int>, onChange: (List<Int>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (OFFSET_CHOICES + selected.filter { it !in OFFSET_CHOICES }).sorted().forEach { d ->
            val on = d in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - d else (selected + d).sorted()) },
                label = { Text(offsetLabel(d)) },
                leadingIcon = if (on) {
                    { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                } else null,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MethodChips(
    ring: Boolean, sms: Boolean, email: Boolean,
    onRing: (Boolean) -> Unit, onSms: (Boolean) -> Unit, onEmail: (Boolean) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MethodChip("通知", Icons.Filled.Notifications, true) {}
        MethodChip("来电响铃", Icons.Filled.Call, ring) { onRing(!ring) }
        MethodChip("短信", Icons.AutoMirrored.Filled.Send, sms) { onSms(!sms) }
        MethodChip("邮件", Icons.Filled.Email, email) { onEmail(!email) }
    }
}

@Composable
private fun MethodChip(label: String, icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = on,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
    )
}

/** 系统自带的滚轮选择器（稳定、省资源） */
@Composable
fun Wheel(items: List<String>, index: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, wrap: Boolean = true) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val callback by rememberUpdatedState(onChange)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            NumberPicker(ctx).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, v -> callback(v) }
            }
        },
        update = { p ->
            val arr = items.toTypedArray()
            val cur: Array<String>? = p.displayedValues
            if (cur == null || !cur.contentEquals(arr)) {
                p.displayedValues = null
                p.minValue = 0
                p.maxValue = arr.size - 1
                p.displayedValues = arr
            }
            p.wrapSelectorWheel = wrap
            if (Build.VERSION.SDK_INT >= 29) p.textColor = textColor
            val i = index.coerceIn(0, arr.size - 1)
            if (p.value != i) p.value = i
        },
    )
}
