package com.haining.birthday.remind

import android.content.Context
import com.haining.birthday.core.Dates
import com.haining.birthday.core.Occurrence
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventLog
import com.haining.birthday.data.Prefs
import com.haining.birthday.data.Repo
import java.time.LocalDate

object Reminder {

    /** 今天需要提醒的：距离下一次的天数正好在“提前天数”里 */
    fun dueOn(ctx: Context, today: LocalDate): List<Pair<Event, Occurrence>> =
        Repo.load(ctx).mapNotNull { e ->
            Dates.next(e, today)?.takeIf { it.days in e.offsets }?.let { e to it }
        }.sortedBy { it.second.days }

    /** 每天执行一次（同一天重复调用会被忽略） */
    @Synchronized
    fun runDaily(ctx: Context) {
        val p = Prefs(ctx)
        val today = LocalDate.now()
        if (p.lastCheckDay == today.toEpochDay()) return
        p.lastCheckDay = today.toEpochDay()
        for ((e, o) in dueOn(ctx, today)) fire(ctx, e, o)
    }

    /** 按事件设置的方式发出提醒 */
    fun fire(ctx: Context, e: Event, o: Occurrence) {
        val p = Prefs(ctx)
        val msg = Dates.message(e, o)
        Notifier.remind(ctx, e, o)
        if (e.ring) Notifier.ring(ctx, RingInfo.of(e, o))
        if (e.sms) {
            val err = if (!p.smsReady) "还没有设置接收短信的号码" else
                SmsSender.send(ctx, p.smsTo, Dates.tag(e) + msg + noteOf(e), p.smsSubId, e.name)
            if (err != null) {
                EventLog.add(ctx, false, "短信未发出：$err（${Dates.title(e)}）")
                Notifier.status(ctx, "提醒短信没有发出", err)
            }
        }
        if (e.email) {
            if (!p.mailReady) {
                EventLog.add(ctx, false, "邮件未发出：还没有设置邮箱（${Dates.title(e)}）")
                Notifier.status(ctx, "提醒邮件没有发出", "还没有在设置里配置邮箱")
            } else {
                val body = msg + noteOf(e) + "\n\n—— 来自「生日提醒」App"
                EmailJob.enqueue(ctx, Dates.tag(e) + Dates.daysWord(o.days) + "是" + Dates.title(e), body)
            }
        }
        EventLog.add(ctx, true, "已提醒：$msg")
    }

    private fun noteOf(e: Event) = if (e.note.isBlank()) "" else "。备注：" + e.note
}
