package com.haining.birthday.data

import org.json.JSONArray
import org.json.JSONObject

enum class EventType(val label: String, val nameLabel: String) {
    BIRTHDAY("生日", "姓名或称呼"),
    ANNIVERSARY("纪念日", "名称，如：结婚纪念日"),
    MEMORIAL("忌日", "姓名或称呼"),
    OTHER("其他", "名称"),
}

/** 一个需要每年提醒的日子 */
data class Event(
    val id: Long,
    val name: String,
    val type: EventType = EventType.BIRTHDAY,
    val lunar: Boolean = false,
    /** 0 表示年份不详 */
    val year: Int = 0,
    val month: Int = 1,
    val day: Int = 1,
    /** 农历闰月 */
    val leap: Boolean = false,
    /** 提前几天提醒，0 = 当天 */
    val offsets: List<Int> = listOf(0),
    val ring: Boolean = false,
    val sms: Boolean = false,
    val email: Boolean = false,
    /** 对方电话，可一键拨打或发祝福短信 */
    val phone: String = "",
    val note: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("type", type.name)
        .put("lunar", lunar)
        .put("year", year)
        .put("month", month)
        .put("day", day)
        .put("leap", leap)
        .put("offsets", JSONArray(offsets))
        .put("ring", ring)
        .put("sms", sms)
        .put("email", email)
        .put("phone", phone)
        .put("note", note)

    companion object {
        fun fromJson(o: JSONObject): Event {
            val arr = o.optJSONArray("offsets")
            val offsets = if (arr == null) listOf(0) else List(arr.length()) { arr.getInt(it) }
            return Event(
                id = o.getLong("id"),
                name = o.getString("name"),
                type = EventType.entries.firstOrNull { it.name == o.optString("type") } ?: EventType.OTHER,
                lunar = o.optBoolean("lunar"),
                year = o.optInt("year"),
                month = o.getInt("month").coerceIn(1, 12),
                day = o.getInt("day").coerceIn(1, 31),
                leap = o.optBoolean("leap"),
                offsets = offsets.filter { it in 0..365 }.distinct().sorted(),
                ring = o.optBoolean("ring"),
                sms = o.optBoolean("sms"),
                email = o.optBoolean("email"),
                phone = o.optString("phone"),
                note = o.optString("note"),
            )
        }
    }
}
