package com.haining.birthday.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.haining.birthday.core.Dates
import com.haining.birthday.core.Lunar
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventType
import com.haining.birthday.data.Prefs
import com.haining.birthday.remind.SmsSender
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    initial: Event?,
    today: LocalDate,
    onBack: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }

    var type by remember { mutableStateOf(initial?.type ?: EventType.BIRTHDAY) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var lunar by remember { mutableStateOf(initial?.lunar ?: false) }
    var year by remember { mutableStateOf(initial?.year ?: 0) }
    var month by remember { mutableStateOf(initial?.month ?: today.monthValue) }
    var day by remember { mutableStateOf(initial?.day ?: today.dayOfMonth) }
    var leap by remember { mutableStateOf(initial?.leap ?: false) }
    var offsets by remember { mutableStateOf(initial?.offsets ?: prefs.defOffsets) }
    var ring by remember { mutableStateOf(initial?.ring ?: prefs.defRing) }
    var sms by remember { mutableStateOf(initial?.sms ?: prefs.defSms) }
    var email by remember { mutableStateOf(initial?.email ?: prefs.defEmail) }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    var showDate by remember { mutableStateOf(false) }
    var datePicked by remember { mutableStateOf(initial != null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) toast(ctx, "没有短信权限，到时将无法发送提醒短信")
    }

    val draft = Event(
        id = initial?.id ?: 0, name = name.trim(), type = type, lunar = lunar, year = year, month = month,
        day = day, leap = leap, offsets = offsets, ring = ring, sms = sms, email = email,
        phone = phone.trim(), note = note.trim(),
    )
    val occ = remember(draft, today) { Dates.next(draft, today) }

    fun save() {
        if (draft.name.isEmpty()) {
            nameError = true
            return
        }
        onSave(draft.copy(id = initial?.id ?: System.currentTimeMillis()))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "添加" else "编辑") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    TextButton(onClick = ::save) { Text("保存", fontWeight = FontWeight.Bold) }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Section("类型") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) })
                    }
                }
            }

            Section("日期") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text(type.nameLabel) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("请填写名称") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !lunar,
                        onClick = { lunar = false; leap = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("公历") }
                    SegmentedButton(
                        selected = lunar,
                        onClick = { lunar = true; day = day.coerceAtMost(30) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("农历") }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = { showDate = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Dates.eventDate(draft), style = MaterialTheme.typography.titleMedium)
                            val sub = Dates.origin(draft)?.let { o ->
                                if (lunar) "即公历 ${Dates.ymd(o)}" else if (Lunar.inRange(o)) "即农历 ${Lunar.format(Lunar.fromSolar(o))}" else null
                            } ?: if (year == 0) "年份不详" else null
                            sub?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("修改", color = MaterialTheme.colorScheme.primary)
                    }
                }
                occ?.let { o ->
                    Text(
                        "下一次：${Dates.ymd(o.date)} ${Dates.week(o.date)}，" +
                            (if (o.days == 0) "就是今天" else "还有 ${o.days} 天") +
                            (Dates.nthText(draft, o.nth)?.let { "，$it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
            }

            Section("什么时候提醒") {
                OffsetChips(offsets) { offsets = it }
                Hint(
                    if (offsets.isEmpty()) "没有选择，只在列表里显示，不会提醒"
                    else "到那天的 ${hhmm(prefs.hour, prefs.minute)} 提醒（时间可在设置里改）"
                )
            }

            Section("提醒方式") {
                MethodChips(
                    ring, sms, email,
                    onRing = { ring = it },
                    onSms = {
                        sms = it
                        if (it && !SmsSender.hasPermission(ctx)) smsPermission.launch(Manifest.permission.SEND_SMS)
                    },
                    onEmail = { email = it },
                )
                if (ring) Hint("来电响铃：像来电一样持续响铃并弹出全屏界面，可在设置里试听效果")
                if (sms && !prefs.smsReady) Hint("⚠ 还没设置接收短信的号码，请到「设置 → 短信提醒」里填写")
                if (email && !prefs.mailReady) Hint("⚠ 还没设置邮箱，请到「设置 → 邮件提醒」里填写")
            }

            Section("更多（可选）") {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("对方电话") },
                    supportingText = { Text("填了的话，当天的通知上可以一键打电话、发祝福短信") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    placeholder = { Text("如：喜欢的礼物、要准备的事") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (initial != null) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("删除")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDate) {
        DateDialog(
            lunar = lunar, year = year, month = month, day = day, leap = leap,
            defaultYear = if (type == EventType.BIRTHDAY || type == EventType.MEMORIAL) 1990 else today.year,
            showYearByDefault = !datePicked,
            onDismiss = { showDate = false },
            onConfirm = { y, m, d, l ->
                year = y; month = m; day = d; leap = l
                showDate = false
                datePicked = true
            },
        )
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除「${initial.name}」？") },
            text = { Text("删除后不会再提醒，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(initial) }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}
