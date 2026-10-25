package com.haining.birthday.core

import com.haining.birthday.data.Event
import com.haining.birthday.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LunarTest {

    /** 历年春节（农历正月初一）的公历日期 */
    private val springFestival = mapOf(
        1950 to "1950-02-17", 1960 to "1960-01-28", 1970 to "1970-02-06", 1980 to "1980-02-16",
        1985 to "1985-02-20", 1990 to "1990-01-27", 1995 to "1995-01-31", 2000 to "2000-02-05",
        2001 to "2001-01-24", 2002 to "2002-02-12", 2003 to "2003-02-01", 2004 to "2004-01-22",
        2005 to "2005-02-09", 2006 to "2006-01-29", 2007 to "2007-02-18", 2008 to "2008-02-07",
        2009 to "2009-01-26", 2010 to "2010-02-14", 2011 to "2011-02-03", 2012 to "2012-01-23",
        2013 to "2013-02-10", 2014 to "2014-01-31", 2015 to "2015-02-19", 2016 to "2016-02-08",
        2017 to "2017-01-28", 2018 to "2018-02-16", 2019 to "2019-02-05", 2020 to "2020-01-25",
        2021 to "2021-02-12", 2022 to "2022-02-01", 2023 to "2023-01-22", 2024 to "2024-02-10",
        2025 to "2025-01-29", 2026 to "2026-02-17", 2027 to "2027-02-06", 2028 to "2028-01-26",
        2029 to "2029-02-13", 2030 to "2030-02-03",
    )

    @Test
    fun springFestivalDates() {
        for ((y, d) in springFestival) {
            assertEquals("春节 $y", LocalDate.parse(d), Lunar.toSolar(y, 1, 1, false))
            assertEquals("春节 $y", LunarDate(y, 1, 1, false), Lunar.fromSolar(LocalDate.parse(d)))
        }
    }

    @Test
    fun midAutumnAndToday() {
        assertEquals(LocalDate.of(2024, 9, 17), Lunar.toSolar(2024, 8, 15, false))
        assertEquals(LocalDate.of(2025, 10, 6), Lunar.toSolar(2025, 8, 15, false))
        assertEquals(LocalDate.of(2026, 9, 25), Lunar.toSolar(2026, 8, 15, false))
        assertEquals("八月廿八", Lunar.format(Lunar.fromSolar(LocalDate.of(2026, 10, 8))))
    }

    @Test
    fun leapMonths() {
        assertEquals(4, Lunar.leapMonth(2020))
        assertEquals(2, Lunar.leapMonth(2023))
        assertEquals(6, Lunar.leapMonth(2025))
        assertEquals(0, Lunar.leapMonth(2026))
        assertEquals(5, Lunar.leapMonth(2028))
        // 闰月在正常月份之后
        val june = Lunar.toSolar(2025, 6, 1, false)!!
        val leapJune = Lunar.toSolar(2025, 6, 1, true)!!
        assertEquals(Lunar.monthDays(2025, 6).toLong(), leapJune.toEpochDay() - june.toEpochDay())
        assertEquals(LunarDate(2025, 6, 1, true), Lunar.fromSolar(leapJune))
        // 没有闰月的年份按正常月份算
        assertEquals(Lunar.toSolar(2026, 6, 1, false), Lunar.toSolar(2026, 6, 1, true))
    }

    @Test
    fun roundTripEveryDay() {
        var d = LocalDate.of(1900, 1, 31)
        val end = LocalDate.of(2100, 12, 31)
        while (!d.isAfter(end)) {
            val l = Lunar.fromSolar(d)
            assertEquals(d.toString(), d, Lunar.toSolar(l.year, l.month, l.day, l.leap))
            d = d.plusDays(1)
        }
    }

    @Test
    fun names() {
        assertEquals("初一", Lunar.dayName(1))
        assertEquals("初十", Lunar.dayName(10))
        assertEquals("十五", Lunar.dayName(15))
        assertEquals("二十", Lunar.dayName(20))
        assertEquals("廿八", Lunar.dayName(28))
        assertEquals("三十", Lunar.dayName(30))
        assertEquals("正月", Lunar.monthName(1))
        assertEquals("闰六月", Lunar.monthName(6, true))
        assertEquals("腊月", Lunar.monthName(12))
        assertEquals("丙午", Lunar.ganZhi(2026))
        assertEquals("马", Lunar.zodiac(2026))
        assertEquals("甲辰", Lunar.ganZhi(2024))
        assertEquals("龙", Lunar.zodiac(2024))
    }
}

class DatesTest {
    private val today = LocalDate.of(2026, 10, 8)

    private fun ev(lunar: Boolean, y: Int, m: Int, d: Int, type: EventType = EventType.BIRTHDAY, leap: Boolean = false) =
        Event(id = 1, name = "妈妈", type = type, lunar = lunar, year = y, month = m, day = d, leap = leap)

    @Test
    fun lunarBirthdayToday() {
        val o = Dates.next(ev(true, 1968, 8, 28), today)!!
        assertEquals(today, o.date)
        assertEquals(0, o.days)
        assertEquals(58, o.nth)
        assertEquals("今天是妈妈的生日（10月8日 周四 · 农历八月廿八 · 58岁生日）", Dates.message(ev(true, 1968, 8, 28), o))
    }

    @Test
    fun solarBirthdayNextYear() {
        val o = Dates.next(ev(false, 1990, 5, 20), today)!!
        assertEquals(LocalDate.of(2027, 5, 20), o.date)
        assertEquals(37, o.nth)
    }

    @Test
    fun feb29InCommonYear() {
        val o = Dates.next(ev(false, 2000, 2, 29), LocalDate.of(2027, 1, 1))!!
        assertEquals(LocalDate.of(2027, 2, 28), o.date)
        val leapYear = Dates.next(ev(false, 2000, 2, 29), LocalDate.of(2028, 1, 1))!!
        assertEquals(LocalDate.of(2028, 2, 29), leapYear.date)
    }

    @Test
    fun lunarDay30InShortMonth() {
        // 年份不详的“腊月三十”，在只有 29 天的腊月取廿九（除夕）
        val e = ev(true, 0, 12, 30)
        val o = Dates.next(e, today)!!
        val l = Lunar.fromSolar(o.date)
        assertEquals(12, l.month)
        assertEquals(Lunar.monthDays(l.year, 12), l.day)
        assertNull(o.nth)
    }

    @Test
    fun futureAnniversaryStartsAtOrigin() {
        val o = Dates.next(ev(false, 2028, 5, 1, EventType.ANNIVERSARY), today)!!
        assertEquals(LocalDate.of(2028, 5, 1), o.date)
        assertEquals(0, o.nth)
    }

    @Test
    fun offsetsAreDaysUntil() {
        val e = ev(false, 0, 10, 15).copy(offsets = listOf(0, 7))
        assertEquals(7, Dates.next(e, today)!!.days)
        assertEquals("7天后是妈妈的生日（10月15日 周四）", Dates.message(e, Dates.next(e, today)!!))
    }
}
