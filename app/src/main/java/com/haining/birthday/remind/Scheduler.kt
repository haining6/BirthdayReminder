package com.haining.birthday.remind

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.haining.birthday.data.Prefs
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * 只用一个系统闹钟：每天到点唤醒一次，检查完马上排好下一天。
 * 平时不常驻后台、不轮询，几乎不耗电。
 */
object Scheduler {

    fun nextTrigger(p: Prefs, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var t = now.toLocalDate().atTime(p.hour, p.minute).atZone(now.zone)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t
    }

    fun schedule(ctx: Context) {
        val at = nextTrigger(Prefs(ctx)).toInstant().toEpochMilli()
        val pi = PendingIntent.getBroadcast(
            ctx, 1,
            Intent(ctx, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DAILY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        setExact(ctx, at, pi)
    }

    /** 来电式提醒：10 分钟后再响 */
    fun snooze(ctx: Context, info: RingInfo, minutes: Int = 10) {
        val pi = PendingIntent.getBroadcast(
            ctx, info.id,
            info.putTo(Intent(ctx, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RING)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        setExact(ctx, System.currentTimeMillis() + minutes * 60_000L, pi)
    }

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setExact(ctx: Context, at: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (canExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /**
     * 打开 App、开机、改系统时间时调用：
     * 如果今天的提醒时间已过但还没检查（比如那会儿手机关机了），立即补上；然后排好下一次。
     */
    fun ensure(ctx: Context) {
        val p = Prefs(ctx)
        val now = LocalDateTime.now()
        val due = !now.toLocalTime().isBefore(LocalTime.of(p.hour, p.minute))
        if (due && p.lastCheckDay != now.toLocalDate().toEpochDay()) Reminder.runDaily(ctx)
        schedule(ctx)
    }
}
