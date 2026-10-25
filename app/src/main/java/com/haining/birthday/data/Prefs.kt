package com.haining.birthday.data

import android.content.Context
import android.content.SharedPreferences

/** 设置项，全部只存在本机。 */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun int(key: String, def: Int) = sp.getInt(key, def)
    private fun str(key: String) = sp.getString(key, "") ?: ""
    private fun bool(key: String, def: Boolean = false) = sp.getBoolean(key, def)
    private fun put(key: String, v: Int) = sp.edit().putInt(key, v).apply()
    private fun put(key: String, v: String) = sp.edit().putString(key, v).apply()
    private fun put(key: String, v: Boolean) = sp.edit().putBoolean(key, v).apply()

    /** 每天检查并发送提醒的时间 */
    var hour: Int
        get() = int("hour", 9)
        set(v) = put("hour", v)
    var minute: Int
        get() = int("minute", 0)
        set(v) = put("minute", v)

    /** 新建时默认的提醒设置 */
    var defOffsets: List<Int>
        get() = (sp.getString("defOffsets", "0,1,7") ?: "").split(',').mapNotNull { it.trim().toIntOrNull() }
        set(v) = put("defOffsets", v.sorted().joinToString(","))
    var defRing: Boolean
        get() = bool("defRing")
        set(v) = put("defRing", v)
    var defSms: Boolean
        get() = bool("defSms")
        set(v) = put("defSms", v)
    var defEmail: Boolean
        get() = bool("defEmail")
        set(v) = put("defEmail", v)

    /** 短信：用哪张卡发（-1 = 系统默认）、发给哪个号码 */
    var smsSubId: Int
        get() = int("smsSubId", -1)
        set(v) = put("smsSubId", v)
    var smsTo: String
        get() = str("smsTo")
        set(v) = put("smsTo", v)

    /** 邮件 SMTP */
    var smtpHost: String
        get() = str("smtpHost")
        set(v) = put("smtpHost", v)
    var smtpPort: Int
        get() = int("smtpPort", 465)
        set(v) = put("smtpPort", v)
    /** true = SSL（465），false = STARTTLS（587） */
    var smtpSsl: Boolean
        get() = bool("smtpSsl", true)
        set(v) = put("smtpSsl", v)
    var smtpUser: String
        get() = str("smtpUser")
        set(v) = put("smtpUser", v)
    var mailTo: String
        get() = str("mailTo")
        set(v) = put("mailTo", v)

    /** 授权码：加密后保存 */
    var smtpPass: String
        get() = sp.getString("smtpPassEnc", null)?.let { Secret.decrypt(it) } ?: ""
        set(v) {
            val e = sp.edit()
            if (v.isEmpty()) e.remove("smtpPassEnc") else e.putString("smtpPassEnc", Secret.encrypt(v))
            e.apply()
        }
    val hasSmtpPass: Boolean get() = sp.contains("smtpPassEnc")

    val smsReady: Boolean get() = smsTo.isNotBlank()
    val mailReady: Boolean get() = smtpHost.isNotBlank() && smtpUser.isNotBlank() && hasSmtpPass

    /** 最近一次执行每日检查的日期（epochDay） */
    var lastCheckDay: Long
        get() = sp.getLong("lastCheckDay", -1)
        set(v) {
            sp.edit().putLong("lastCheckDay", v).commit()
        }

    var askedNotification: Boolean
        get() = bool("askedNotification")
        set(v) = put("askedNotification", v)

    fun nextJobId(): Int {
        val id = int("jobSeq", 0)
        put("jobSeq", (id + 1) % 1000)
        return 1000 + id
    }
}
