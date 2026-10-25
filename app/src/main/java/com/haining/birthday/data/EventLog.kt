package com.haining.birthday.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 提醒记录：最近 60 条，让你能确认每次提醒到底发没发出去。 */
object EventLog {
    private const val MAX = 60

    data class Entry(val time: Long, val ok: Boolean, val text: String)

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("log", Context.MODE_PRIVATE)

    @Synchronized
    fun add(ctx: Context, ok: Boolean, text: String) {
        val sp = sp(ctx)
        val old = JSONArray(sp.getString("items", "[]"))
        val arr = JSONArray()
        for (i in maxOf(0, old.length() - MAX + 1) until old.length()) arr.put(old.get(i))
        arr.put(JSONObject().put("t", System.currentTimeMillis()).put("ok", ok).put("m", text))
        sp.edit().putString("items", arr.toString()).apply()
    }

    @Synchronized
    fun list(ctx: Context): List<Entry> {
        val arr = JSONArray(sp(ctx).getString("items", "[]"))
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Entry(o.optLong("t"), o.optBoolean("ok"), o.optString("m"))
        }.reversed()
    }

    @Synchronized
    fun clear(ctx: Context) {
        sp(ctx).edit().remove("items").apply()
    }
}
