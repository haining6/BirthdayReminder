package com.haining.birthday.ui

import android.Manifest
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.haining.birthday.BuildConfig
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventLog
import com.haining.birthday.data.Prefs
import com.haining.birthday.data.Repo
import com.haining.birthday.remind.MailConfig
import com.haining.birthday.remind.Notifier
import com.haining.birthday.remind.RingInfo
import com.haining.birthday.remind.Scheduler
import com.haining.birthday.remind.Smtp
import com.haining.birthday.remind.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

private data class MailPreset(val name: String, val host: String, val port: Int, val ssl: Boolean)

private val PRESETS = listOf(
    MailPreset("QQ邮箱", "smtp.qq.com", 465, true),
    MailPreset("163邮箱", "smtp.163.com", 465, true),
    MailPreset("126邮箱", "smtp.126.com", 465, true),
    MailPreset("139邮箱", "smtp.139.com", 465, true),
    MailPreset("Gmail", "smtp.gmail.com", 465, true),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    tick: Int,
    events: List<Event>,
    onBack: () -> Unit,
    onEventsChanged: (List<Event>) -> Unit,
) {
    val ctx = LocalContext.current
    val p = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }

    // ---------- 状态 ----------
    var hour by remember { mutableIntStateOf(p.hour) }
    var minute by remember { mutableIntStateOf(p.minute) }
    var defOffsets by remember { mutableStateOf(p.defOffsets) }
    var defRing by remember { mutableStateOf(p.defRing) }
    var defSms by remember { mutableStateOf(p.defSms) }
    var defEmail by remember { mutableStateOf(p.defEmail) }

    var smsTo by remember { mutableStateOf(p.smsTo) }
    var subId by remember { mutableIntStateOf(p.smsSubId) }
    var sims by remember { mutableStateOf(SmsSender.sims(ctx)) }
    var simMenu by remember { mutableStateOf(false) }

    var host by remember { mutableStateOf(p.smtpHost) }
    var port by remember { mutableStateOf(p.smtpPort.toString()) }
    var ssl by remember { mutableStateOf(p.smtpSsl) }
    var user by remember { mutableStateOf(p.smtpUser) }
    var pass by remember { mutableStateOf(p.smtpPass) }
    var passEdited by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var mailTo by remember { mutableStateOf(p.mailTo) }
    var mailBusy by remember { mutableStateOf(false) }
    var mailResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val logs = remember(tick, refresh) { EventLog.list(ctx) }
    val notifOk = remember(tick, refresh) { Notifier.canPost(ctx) }
    val exactOk = remember(tick, refresh) { Scheduler.canExact(ctx) }
    val batteryOk = remember(tick, refresh) {
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    }
    val fullScreenOk = remember(tick, refresh) {
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }

    // 授权码输入停顿 0.6 秒后再加密保存；离开页面时也保存一次
    LaunchedEffect(pass) {
        if (passEdited) {
            delay(600)
            p.smtpPass = pass
            passEdited = false
        }
    }
    val latestPass by rememberUpdatedState(pass)
    val latestEdited by rememberUpdatedState(passEdited)
    DisposableEffect(Unit) {
        onDispose { if (latestEdited) p.smtpPass = latestPass }
    }

    // ---------- 权限 ----------
    var afterSmsGranted by remember { mutableStateOf<(() -> Unit)?>(null) }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) afterSmsGranted?.invoke() else toast(ctx, "没有短信权限，无法发送提醒短信")
        afterSmsGranted = null
    }
    val phonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) {
            sims = SmsSender.sims(ctx)
            simMenu = true
        } else {
            toast(ctx, "未授权，将使用系统默认的短信卡")
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        if (!it) openNotificationSettings(ctx)
    }

    fun sendTestSms() {
        val err = SmsSender.send(ctx, smsTo, "【生日提醒】这是一条测试短信，收到说明短信提醒设置成功。", subId, "测试")
        if (err == null) toast(ctx, "已交给系统发送，请留意是否收到") else toast(ctx, "发送失败：$err")
        refresh++
    }

    // ---------- 备份 ----------
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)!!.use { it.write(Repo.toJson(events).toByteArray(Charsets.UTF_8)) }
            }.onSuccess { toast(ctx, "已导出 ${events.size} 条") }
                .onFailure { toast(ctx, "导出失败：${it.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                Repo.parse(text)
            }.onSuccess { imported ->
                val merged = LinkedHashMap<Long, Event>()
                events.forEach { merged[it.id] = it }
                imported.forEach { merged[it.id] = it }
                val list = merged.values.toList()
                Repo.save(ctx, list)
                onEventsChanged(list)
                toast(ctx, "已导入 ${imported.size} 条")
            }.onFailure { toast(ctx, "导入失败：不是本应用导出的备份文件") }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
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
            // ===== 提醒时间 =====
            Section("提醒时间") {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        TimePickerDialog(ctx, { _, h, m ->
                            hour = h; minute = m
                            p.hour = h; p.minute = m
                            val app = ctx.applicationContext
                            thread { Scheduler.ensure(app) }
                        }, hour, minute, true).show()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("每天提醒时间", style = MaterialTheme.typography.titleMedium)
                        Hint("到点检查当天要提醒的事，并发出通知 / 短信 / 邮件")
                    }
                    Text(
                        hhmm(hour, minute),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("新建时默认提前", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                OffsetChips(defOffsets) { defOffsets = it; p.defOffsets = it }
                Spacer(Modifier.height(10.dp))
                Text("新建时默认提醒方式", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                MethodChips(
                    defRing, defSms, defEmail,
                    onRing = { defRing = it; p.defRing = it },
                    onSms = { defSms = it; p.defSms = it },
                    onEmail = { defEmail = it; p.defEmail = it },
                )
            }

            // ===== 短信 =====
            Section("短信提醒") {
                Text(
                    "到时由本机用你选的卡，给下面的号码发一条提醒短信。双卡手机可以用卡1发给卡2。按普通短信收费（一般 0.1 元/条）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(
                        onClick = {
                            if (sims.isNotEmpty()) simMenu = true
                            else phonePermission.launch(Manifest.permission.READ_PHONE_STATE)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            "发送卡：" + (sims.firstOrNull { it.subId == subId }?.label ?: "系统默认"),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = simMenu, onDismissRequest = { simMenu = false }) {
                        DropdownMenuItem(text = { Text("系统默认") }, onClick = {
                            subId = -1; p.smsSubId = -1; simMenu = false
                        })
                        sims.forEach { s ->
                            DropdownMenuItem(text = { Text(s.label) }, onClick = {
                                subId = s.subId; p.smsSubId = s.subId; simMenu = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = smsTo,
                    onValueChange = { smsTo = it; p.smsTo = it.trim() },
                    label = { Text("接收提醒的手机号") },
                    placeholder = { Text("例如你的另一张卡的号码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    enabled = smsTo.isNotBlank(),
                    onClick = {
                        if (SmsSender.hasPermission(ctx)) sendTestSms()
                        else {
                            afterSmsGranted = { sendTestSms() }
                            smsPermission.launch(Manifest.permission.SEND_SMS)
                        }
                    },
                ) { Text("发送测试短信") }
                Hint("部分手机（小米、OPPO 等）会弹出“允许发送短信”的确认，请选“始终允许”，否则后台提醒短信会被拦截。")
            }

            // ===== 邮件 =====
            Section("邮件提醒") {
                Text(
                    "用你自己的邮箱给自己发提醒邮件。需要在网页版邮箱的设置里开启 SMTP 服务，并生成“授权码”填在下面（不是登录密码）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { pr ->
                        FilterChip(
                            selected = host == pr.host,
                            onClick = {
                                host = pr.host; port = pr.port.toString(); ssl = pr.ssl
                                p.smtpHost = pr.host; p.smtpPort = pr.port; p.smtpSsl = pr.ssl
                            },
                            label = { Text(pr.name) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = host, onValueChange = { host = it; p.smtpHost = it.trim() },
                    label = { Text("SMTP 服务器") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { v ->
                            port = v.filter { it.isDigit() }.take(5)
                            port.toIntOrNull()?.let { p.smtpPort = it }
                        },
                        label = { Text("端口") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(if (ssl) "SSL 加密" else "STARTTLS 加密", modifier = Modifier.weight(1f))
                    Switch(checked = ssl, onCheckedChange = { ssl = it; p.smtpSsl = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = user, onValueChange = { user = it; p.smtpUser = it.trim() },
                    label = { Text("邮箱账号") }, placeholder = { Text("例如 12345@qq.com") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it; passEdited = true },
                    label = { Text("授权码") }, singleLine = true,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showPass = !showPass }) { Text(if (showPass) "隐藏" else "显示") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mailTo, onValueChange = { mailTo = it; p.mailTo = it.trim() },
                    label = { Text("收件邮箱（留空 = 发给自己）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        enabled = !mailBusy && host.isNotBlank() && user.isNotBlank() && pass.isNotEmpty(),
                        onClick = {
                            p.smtpPass = pass
                            passEdited = false
                            mailBusy = true
                            mailResult = null
                            scope.launch {
                                val r = withContext(Dispatchers.IO) {
                                    runCatching {
                                        Smtp.send(
                                            MailConfig.from(p),
                                            "【生日提醒】测试邮件",
                                            "收到这封邮件，说明邮件提醒已经设置成功。\n\n—— 来自「生日提醒」App",
                                        )
                                    }
                                }
                                mailBusy = false
                                mailResult = r.fold(
                                    { true to "发送成功，请查收邮箱" },
                                    { false to Smtp.explain(it) },
                                )
                            }
                        },
                    ) { Text("发送测试邮件") }
                    if (mailBusy) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
                mailResult?.let { (ok, msg) ->
                    Text(
                        msg,
                        color = if (ok) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "授权码用系统硬件密钥加密后只存在本机，只会发送给你上面填写的邮件服务器。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ===== 来电式响铃 =====
            Section("来电式响铃") {
                Text(
                    "像来电一样持续响铃、弹出全屏界面，最长 1 分钟，点“知道了”停止。锁屏时也会亮屏提醒。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = {
                    if (!Notifier.canPost(ctx)) {
                        toast(ctx, "请先允许通知")
                    } else {
                        Notifier.ring(ctx, RingInfo(20240101, "今天是妈妈的生日", "这是测试效果，点“知道了”停止响铃", "妈"))
                    }
                }) { Text("试一下") }
            }

            // ===== 后台保障 =====
            Section("让提醒更准时") {
                StatusRow("通知权限", notifOk, "去开启") {
                    if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else openNotificationSettings(ctx)
                }
                if (!exactOk && Build.VERSION.SDK_INT >= 31) {
                    StatusRow("准时闹钟", false, "去开启") {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    StatusRow("全屏来电界面", fullScreenOk, "去开启") {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                }
                StatusRow("不受电池优化限制", batteryOk, "去设置") {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        .onFailure { openAppDetails(ctx) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "国产手机为了省电会限制后台。为保证准时提醒，建议在「应用详情」里：\n" +
                        "1. 允许「自启动」\n" +
                        "2. 省电策略选「无限制」/ 允许后台活动\n" +
                        "3. 通知里打开「锁屏通知」「悬浮通知 / 横幅」\n" +
                        "4. 小米 / 红米：其他权限里允许「后台弹出界面」「锁屏显示」\n" +
                        "5. 华为 / 荣耀：应用启动管理里改为手动，三项全开",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { openAppDetails(ctx) }) { Text("打开应用详情") }
            }

            // ===== 数据 =====
            Section("备份") {
                Text(
                    "数据只保存在本机。换手机前可以导出备份文件（不含邮箱授权码），在新手机上导入。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { exportLauncher.launch("生日提醒备份-${LocalDate.now()}.json") }) { Text("导出备份") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                        Text("导入备份")
                    }
                }
            }

            // ===== 记录 =====
            Section("提醒记录") {
                if (logs.isEmpty()) {
                    Text("暂无记录。每次提醒、短信和邮件的发送结果都会记在这里。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.CHINA) }
                    logs.take(30).forEach { e ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                if (e.ok) Icons.Filled.Check else Icons.Filled.Warning, null,
                                tint = if (e.ok) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(fmt.format(Date(e.time)), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(e.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    TextButton(onClick = { EventLog.clear(ctx); refresh++ }) { Text("清空记录") }
                }
            }

            // ===== 关于 =====
            Section("关于") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("生日提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text("开发者：海宁", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "无广告、无统计、无第三方 SDK，不收集任何个人信息。所有数据只存在你的手机里。" +
                        "联网权限只用于向你自己填写的邮件服务器发送提醒邮件；短信只发给你自己填写的号码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, action: String, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Filled.Check else Icons.Filled.Warning, null,
            tint = if (ok) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (ok) {
            Text("已开启", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(onClick = onFix, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)) {
                Text(action)
            }
        }
    }
}

private fun openAppDetails(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openNotificationSettings(ctx: android.content.Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { openAppDetails(ctx) }
}
