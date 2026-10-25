package com.haining.birthday.core

import java.time.LocalDate

/** 农历日期。leap 表示闰月。 */
data class LunarDate(val year: Int, val month: Int, val day: Int, val leap: Boolean)

/**
 * 农历（1900–2100）与公历互转，纯查表计算，不联网。
 *
 * 每年一个整数：
 * - 低 4 位：闰哪个月（0 表示无闰月）
 * - 第 4–15 位：1–12 月大小月（从高位到低位依次是正月到腊月，1 为 30 天，0 为 29 天）
 * - 第 16 位：闰月是大月（30 天）还是小月（29 天）
 */
object Lunar {
    const val MIN_YEAR = 1900
    const val MAX_YEAR = 2100

    /** 农历 1900 年正月初一 = 公历 1900-01-31 */
    private val BASE_EPOCH_DAY = LocalDate.of(1900, 1, 31).toEpochDay()

    private val INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, // 2040-2049
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, // 2050-2059
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, // 2060-2069
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, // 2070-2079
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, // 2080-2089
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, // 2090-2099
        0x0d520, // 2100
    )

    private val MONTHS = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
    private val DIGITS = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val GAN = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val ZHI = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val ANIMALS = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    private fun info(y: Int) = INFO[y - MIN_YEAR]

    /** 该年闰几月，0 表示没有闰月 */
    fun leapMonth(y: Int): Int = info(y) and 0xF

    fun leapDays(y: Int): Int =
        if (leapMonth(y) == 0) 0 else if (info(y) and 0x10000 != 0) 30 else 29

    /** 农历 y 年 m 月（非闰月）的天数 */
    fun monthDays(y: Int, m: Int): Int = if (info(y) and (0x10000 shr m) != 0) 30 else 29

    fun yearDays(y: Int): Int {
        var sum = 348
        var bit = 0x8000
        while (bit > 0x8) {
            if (info(y) and bit != 0) sum++
            bit = bit shr 1
        }
        return sum + leapDays(y)
    }

    /** 某月实际天数，leap=true 且该年确实闰这个月时取闰月天数 */
    fun daysIn(y: Int, m: Int, leap: Boolean): Int =
        if (leap && leapMonth(y) == m) leapDays(y) else monthDays(y, m)

    fun inRange(date: LocalDate): Boolean =
        date.toEpochDay() >= BASE_EPOCH_DAY && date.year <= MAX_YEAR

    /** 公历转农历；超出范围时按边界处理 */
    fun fromSolar(date: LocalDate): LunarDate {
        var offset = (date.toEpochDay() - BASE_EPOCH_DAY).toInt().coerceAtLeast(0)
        var y = MIN_YEAR
        while (y < MAX_YEAR) {
            val d = yearDays(y)
            if (offset < d) break
            offset -= d
            y++
        }
        val leap = leapMonth(y)
        var m = 1
        var isLeap = false
        while (m <= 12) {
            val d = if (isLeap) leapDays(y) else monthDays(y, m)
            if (offset < d) break
            offset -= d
            if (!isLeap && leap == m) {
                isLeap = true
            } else {
                isLeap = false
                m++
            }
        }
        return LunarDate(y, m.coerceAtMost(12), offset + 1, isLeap)
    }

    /**
     * 农历转公历。day 超过当月天数时取当月最后一天（如三十 → 小月取廿九）；
     * leap=true 但这一年没有这个闰月时按正常月份算。超出 1900–2100 返回 null。
     */
    fun toSolar(y: Int, m: Int, d: Int, leap: Boolean): LocalDate? {
        if (y < MIN_YEAR || y > MAX_YEAR || m !in 1..12) return null
        val lm = leapMonth(y)
        val useLeap = leap && lm == m
        var offset = 0L
        for (i in MIN_YEAR until y) offset += yearDays(i)
        for (i in 1 until m) {
            offset += monthDays(y, i)
            if (i == lm) offset += leapDays(y)
        }
        if (useLeap) offset += monthDays(y, m)
        val len = if (useLeap) leapDays(y) else monthDays(y, m)
        offset += d.coerceIn(1, len) - 1
        return LocalDate.ofEpochDay(BASE_EPOCH_DAY + offset)
    }

    fun monthName(m: Int, leap: Boolean = false): String =
        (if (leap) "闰" else "") + MONTHS[(m - 1).coerceIn(0, 11)] + "月"

    fun dayName(d: Int): String = when (d) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        else -> arrayOf("初", "十", "廿", "三")[(d / 10).coerceIn(0, 3)] + DIGITS[d % 10]
    }

    fun format(ld: LunarDate): String = monthName(ld.month, ld.leap) + dayName(ld.day)

    fun ganZhi(y: Int): String = GAN[Math.floorMod(y - 4, 10)] + ZHI[Math.floorMod(y - 4, 12)]

    fun zodiac(y: Int): String = ANIMALS[Math.floorMod(y - 4, 12)]
}
