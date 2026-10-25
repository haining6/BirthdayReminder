package com.haining.birthday.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** 纪念日列表，保存在 App 私有目录的 events.json 里，只存本机。 */
object Repo {
    private const val FILE = "events.json"

    @Volatile
    private var cache: List<Event>? = null

    private fun file(ctx: Context) = AtomicFile(File(ctx.filesDir, FILE))

    @Synchronized
    fun load(ctx: Context): List<Event> {
        cache?.let { return it }
        val list = try {
            parse(file(ctx).readFully().toString(Charsets.UTF_8))
        } catch (_: FileNotFoundException) {
            emptyList()
        } catch (_: Exception) {
            // 文件损坏：先挪到一边保留下来，避免被后续保存覆盖
            File(ctx.filesDir, FILE).renameTo(File(ctx.filesDir, "events-broken-${System.currentTimeMillis()}.json"))
            emptyList()
        }
        cache = list
        return list
    }

    @Synchronized
    fun save(ctx: Context, list: List<Event>) {
        val f = file(ctx)
        val out = f.startWrite()
        try {
            out.write(toJson(list).toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (e: Exception) {
            f.failWrite(out)
            throw e
        }
        cache = list
    }

    fun toJson(list: List<Event>): String = JSONObject()
        .put("app", "生日提醒")
        .put("version", 1)
        .put("events", JSONArray().apply { list.forEach { put(it.toJson()) } })
        .toString(2)

    fun parse(text: String): List<Event> {
        val arr = JSONObject(text).getJSONArray("events")
        return List(arr.length()) { Event.fromJson(arr.getJSONObject(it)) }
    }
}
