package com.haining.birthday.remind

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.haining.birthday.data.EventLog

data class Sim(val subId: Int, val label: String)

/** 用指定的卡给你自己设置的号码发提醒短信。不读取、不上传任何短信。 */
object SmsSender {

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    /** 列出手机里的 SIM 卡（需要“电话”权限） */
    @SuppressLint("MissingPermission")
    fun sims(ctx: Context): List<Sim> {
        if (ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
        return runCatching {
            ctx.getSystemService(SubscriptionManager::class.java)
                .activeSubscriptionInfoList.orEmpty()
                .sortedBy { it.simSlotIndex }
                .map { Sim(it.subscriptionId, "卡${it.simSlotIndex + 1} · ${it.displayName ?: it.carrierName ?: ""}") }
        }.getOrDefault(emptyList())
    }

    @Suppress("DEPRECATION")
    private fun manager(ctx: Context, subId: Int): SmsManager {
        val base: SmsManager? = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else null
        return when {
            subId < 0 -> base ?: SmsManager.getDefault()
            base != null -> base.createForSubscriptionId(subId)
            else -> SmsManager.getSmsManagerForSubscriptionId(subId)
        }
    }

    /** 返回 null 表示已交给系统发送；否则返回失败原因。最终结果由 [SmsResultReceiver] 记录。 */
    fun send(ctx: Context, to: String, text: String, subId: Int, label: String): String? {
        if (!hasPermission(ctx)) return "没有“发送短信”权限，请打开 App 授权"
        val number = to.filter { it.isDigit() || it == '+' }
        if (number.isEmpty()) return "接收号码不正确"
        return try {
            val sm = manager(ctx, subId)
            val parts = sm.divideMessage(text)
            val base = (System.nanoTime() and 0xFFFFF).toInt() * 8
            val sent = ArrayList<PendingIntent>(parts.size)
            parts.indices.forEach { i ->
                val it = Intent(ctx, SmsResultReceiver::class.java)
                    .putExtra("label", label)
                    .putExtra("last", i == parts.size - 1)
                sent += PendingIntent.getBroadcast(ctx, base + i, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
            }
            sm.sendMultipartTextMessage(number, null, parts, sent, null)
            null
        } catch (e: Exception) {
            e.message ?: e.javaClass.simpleName
        }
    }
}

/** 接收系统回报的短信发送结果，写入提醒记录 */
class SmsResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra("label").orEmpty()
        if (resultCode == Activity.RESULT_OK) {
            if (intent.getBooleanExtra("last", true)) EventLog.add(context, true, "短信已发出（$label）")
        } else {
            val reason = when (resultCode) {
                SmsManager.RESULT_ERROR_RADIO_OFF -> "无线信号已关闭（飞行模式？）"
                SmsManager.RESULT_ERROR_NO_SERVICE -> "当前无服务"
                SmsManager.RESULT_ERROR_NULL_PDU -> "短信内容异常"
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "运营商或系统拒绝发送（可能被手机管家拦截，或欠费）"
                else -> "错误代码 $resultCode"
            }
            EventLog.add(context, false, "短信发送失败（$label）：$reason")
            Notifier.status(context, "提醒短信发送失败", reason)
        }
    }
}
