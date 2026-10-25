package com.haining.birthday.remind

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.haining.birthday.R
import com.haining.birthday.core.Dates
import com.haining.birthday.core.Occurrence
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventType
import com.haining.birthday.ui.MainActivity

/** 来电式提醒需要的信息（事件被删除后稍后提醒也能正常显示） */
data class RingInfo(val id: Int, val title: String, val text: String, val avatar: String) {
    fun putTo(i: Intent): Intent = i.putExtra("rid", id).putExtra("title", title)
        .putExtra("text", text).putExtra("avatar", avatar)

    companion object {
        fun from(i: Intent) = RingInfo(
            i.getIntExtra("rid", 0),
            i.getStringExtra("title").orEmpty(),
            i.getStringExtra("text").orEmpty(),
            i.getStringExtra("avatar").orEmpty(),
        )

        fun of(e: Event, o: Occurrence) = RingInfo(
            id = ("ring" + e.id).hashCode(),
            title = (if (o.days == 0) "今天是" else Dates.daysWord(o.days) + "是") + Dates.title(e),
            text = Dates.detail(e, o),
            avatar = avatarOf(e.name),
        )
    }
}

fun avatarOf(name: String): String {
    val s = name.trim()
    return if (s.isEmpty()) "?" else String(Character.toChars(s.codePointAt(0)))
}

object Notifier {
    const val CH_REMIND = "remind"
    const val CH_RING = "ring"
    const val CH_STATUS = "status"
    private const val COLOR = 0xFFE2545C.toInt()

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val remind = NotificationChannel(CH_REMIND, "重要日子提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "生日、纪念日到期或提前提醒"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val ring = NotificationChannel(CH_RING, "来电式响铃提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "像来电一样持续响铃，直到点“知道了”（最长 1 分钟）"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 1000, 1000, 1000)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val status = NotificationChannel(CH_STATUS, "发送结果", NotificationManager.IMPORTANCE_LOW).apply {
            description = "短信、邮件发送失败时告诉你"
        }
        nm.createNotificationChannels(listOf(remind, ring, status))
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            ctx.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** 普通提醒通知 */
    fun remind(ctx: Context, e: Event, o: Occurrence) {
        if (!canPost(ctx)) return
        ensureChannels(ctx)
        val id = ("remind" + e.id + "_" + o.days).hashCode()
        val title = when (o.days) {
            0 -> "今天是" + Dates.title(e) + if (e.type == EventType.BIRTHDAY) " 🎂" else ""
            else -> Dates.daysWord(o.days) + "：" + Dates.title(e)
        }
        val body = Dates.detail(e, o) + if (e.note.isNotBlank()) "\n备注：" + e.note else ""
        val b = Notification.Builder(ctx, CH_REMIND)
            .setSmallIcon(R.drawable.ic_stat_cake)
            .setColor(COLOR)
            .setContentTitle(title)
            .setContentText(Dates.detail(e, o))
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(openApp(ctx))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
        val phone = e.phone.filter { it.isDigit() || it == '+' }
        if (o.days == 0 && phone.isNotEmpty() && e.type != EventType.MEMORIAL) {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val wish = if (e.type == EventType.BIRTHDAY) "生日快乐！" else "节日快乐！"
            val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                .putExtra("sms_body", wish).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            b.addAction(action(PendingIntent.getActivity(ctx, id + 1, dial, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT), "打电话"))
            b.addAction(action(PendingIntent.getActivity(ctx, id + 2, sms, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT), "发短信祝福"))
        }
        ctx.getSystemService(NotificationManager::class.java).notify(id, b.build())
    }

    /** 来电式提醒：全屏界面 + 持续响铃，最长 1 分钟 */
    fun ring(ctx: Context, info: RingInfo) {
        if (!canPost(ctx)) return
        ensureChannels(ctx)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val full = PendingIntent.getActivity(
            ctx, info.id,
            info.putTo(Intent(ctx, RingActivity::class.java)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            flags,
        )
        val snooze = PendingIntent.getBroadcast(
            ctx, info.id, info.putTo(Intent(ctx, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_SNOOZE)), flags,
        )
        val dismiss = PendingIntent.getBroadcast(
            ctx, info.id, info.putTo(Intent(ctx, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DISMISS)), flags,
        )
        val n = Notification.Builder(ctx, CH_RING)
            .setSmallIcon(R.drawable.ic_stat_cake)
            .setColor(COLOR)
            .setContentTitle(info.title)
            .setContentText(info.text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .setOngoing(true)
            .setTimeoutAfter(RING_MILLIS)
            .addAction(action(snooze, "10分钟后再提醒"))
            .addAction(action(dismiss, "知道了"))
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        ctx.getSystemService(NotificationManager::class.java).notify(info.id, n)
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(NotificationManager::class.java).cancel(id)
    }

    /** 短信 / 邮件发送失败时提示 */
    fun status(ctx: Context, title: String, text: String) {
        if (!canPost(ctx)) return
        ensureChannels(ctx)
        val n = Notification.Builder(ctx, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_cake)
            .setColor(COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify((title + text).hashCode(), n)
    }

    private fun action(pi: PendingIntent, label: String): Notification.Action =
        Notification.Action.Builder(null as Icon?, label, pi).build()

    const val RING_MILLIS = 60_000L
}
