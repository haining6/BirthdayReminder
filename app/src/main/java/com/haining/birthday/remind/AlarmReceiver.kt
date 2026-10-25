package com.haining.birthday.remind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

/** 每日闹钟、来电提醒的“稍后/知道了”、以及开机/改时间后的重新安排 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val pending = goAsync()
        thread(name = "reminder") {
            try {
                when (intent.action) {
                    // 只有到了当天设定时间才执行（闹钟被系统推迟到午夜后也不会提前发第二天的提醒）
                    ACTION_DAILY -> Scheduler.ensure(ctx)
                    ACTION_RING -> Notifier.ring(ctx, RingInfo.from(intent))
                    ACTION_SNOOZE -> {
                        val info = RingInfo.from(intent)
                        Notifier.cancel(ctx, info.id)
                        Scheduler.snooze(ctx, info)
                    }
                    ACTION_DISMISS -> Notifier.cancel(ctx, RingInfo.from(intent).id)
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> Scheduler.ensure(ctx)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DAILY = "com.haining.birthday.DAILY"
        const val ACTION_RING = "com.haining.birthday.RING"
        const val ACTION_SNOOZE = "com.haining.birthday.SNOOZE"
        const val ACTION_DISMISS = "com.haining.birthday.DISMISS"
    }
}
