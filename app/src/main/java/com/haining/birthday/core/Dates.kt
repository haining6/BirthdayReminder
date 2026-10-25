package com.haining.birthday.core

import com.haining.birthday.data.Event
import com.haining.birthday.data.EventType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * 下一次到来的日子。
 * @param days 距今天的天数，0 = 今天
 * @param nth 第几次（几岁生日 / 几周年），年份不详时为 null
 */
data class Occurrence(val date: LocalDate, val days: Int, val nth: Int?)

object Dates {
    private val WEEK = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    /** 公历某年某月某日；2 月 29 日在平年取 2 月 28 日 */
    fun solar(y: Int, m: Int, d: Int): LocalDate {
        val ym = YearMonth.of(y, m)
        return ym.atDay(d.coerceIn(1, ym.lengthOfMonth()))
    }

    /** 事件本身那一天（年份已知时） */
    fun origin(e: Event): LocalDate? = when {
        e.year <= 0 -> null
        e.lunar -> Lunar.toSolar(e.year, e.month, e.day, e.leap)
        else -> solar(e.year, e.month, e.day)
    }

    /** 从 today（含）起最近的一次 */
    fun next(e: Event, today: LocalDate): Occurrence? {
        val origin = origin(e)
        val from = if (origin != null && origin.isAfter(today)) origin else today
        val found = if (e.lunar) nextLunar(e, from) else nextSolar(e, from)
        val (date, year) = found ?: return null
        val nth = if (e.year > 0) year - e.year else null
        return Occurrence(date, ChronoUnit.DAYS.between(today, date).toInt(), nth)
    }

    private fun nextSolar(e: Event, from: LocalDate): Pair<LocalDate, Int>? {
        for (y in from.year..from.year + 1) {
            val d = solar(y, e.month, e.day)
            if (!d.isBefore(from)) return d to y
        }
        return null
    }

    private fun nextLunar(e: Event, from: LocalDate): Pair<LocalDate, Int>? {
        if (!Lunar.inRange(from)) return null
        val ly = Lunar.fromSolar(from).year
        for (y in ly..minOf(ly + 1, Lunar.MAX_YEAR)) {
            val d = Lunar.toSolar(y, e.month, e.day, e.leap) ?: continue
            if (!d.isBefore(from)) return d to y
        }
        return null
    }

    fun week(d: LocalDate): String = WEEK[d.dayOfWeek.value - 1]

    fun md(d: LocalDate): String = "${d.monthValue}月${d.dayOfMonth}日"

    fun ymd(d: LocalDate): String = "${d.year}年${d.monthValue}月${d.dayOfMonth}日"

    fun lunarOf(d: LocalDate): String = if (Lunar.inRange(d)) Lunar.format(Lunar.fromSolar(d)) else ""

    /** 事件设定的日期，例如“农历 1968年 八月廿八”“公历 5月20日” */
    fun eventDate(e: Event): String = if (e.lunar) {
        "农历 " + (if (e.year > 0) "${e.year}年 " else "") + Lunar.monthName(e.month, e.leap) + Lunar.dayName(e.day)
    } else {
        "公历 " + (if (e.year > 0) "${e.year}年" else "") + "${e.month}月${e.day}日"
    }

    fun daysWord(days: Int): String = when (days) {
        0 -> "今天"
        1 -> "明天"
        2 -> "后天"
        else -> "${days}天后"
    }

    /** “58岁生日”“10周年” */
    fun nthText(e: Event, nth: Int?): String? = when {
        nth == null || nth < 0 -> null
        e.type == EventType.BIRTHDAY -> if (nth == 0) "出生" else "${nth}岁生日"
        else -> if (nth == 0) "当天" else "${nth}周年"
    }

    /** “妈妈的生日”“结婚纪念日” */
    fun title(e: Event): String = when (e.type) {
        EventType.BIRTHDAY -> "${e.name}的生日"
        EventType.MEMORIAL -> "${e.name}的忌日"
        else -> e.name
    }

    /** “10月11日 周日 · 农历八月三十 · 58岁生日” */
    fun detail(e: Event, o: Occurrence): String = buildList {
        add(md(o.date) + " " + week(o.date))
        if (e.lunar) add("农历" + lunarOf(o.date))
        nthText(e, o.nth)?.let { add(it) }
    }.joinToString(" · ")

    /** 提醒正文：“3天后是妈妈的生日（10月11日 周日 · 农历八月三十 · 58岁生日）” */
    fun message(e: Event, o: Occurrence): String =
        "${daysWord(o.days)}是${title(e)}（${detail(e, o)}）"

    fun tag(e: Event): String = when (e.type) {
        EventType.BIRTHDAY -> "【生日提醒】"
        EventType.ANNIVERSARY -> "【纪念日提醒】"
        EventType.MEMORIAL -> "【忌日提醒】"
        EventType.OTHER -> "【重要日子提醒】"
    }
}
