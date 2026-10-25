package com.haining.birthday.remind

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.haining.birthday.data.EventLog
import com.haining.birthday.data.Prefs
import kotlin.concurrent.thread

/** 用系统 JobScheduler 发邮件：有网才发，网络不好会自动重试，最多 6 小时。 */
object EmailJob {
    fun enqueue(ctx: Context, subject: String, body: String) {
        val extras = PersistableBundle().apply {
            putString("subject", subject)
            putString("body", body)
            putLong("created", System.currentTimeMillis())
        }
        val job = JobInfo.Builder(Prefs(ctx).nextJobId(), ComponentName(ctx, EmailJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setBackoffCriteria(60_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            .setExtras(extras)
            .build()
        ctx.getSystemService(JobScheduler::class.java).schedule(job)
    }
}

class EmailJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        val subject = params.extras.getString("subject").orEmpty()
        val body = params.extras.getString("body").orEmpty()
        val created = params.extras.getLong("created")
        thread(name = "email") {
            var retry = false
            try {
                Smtp.send(MailConfig.from(Prefs(this)), subject, body)
                EventLog.add(this, true, "邮件已发出：$subject")
            } catch (t: Throwable) {
                val tooLate = System.currentTimeMillis() - created > 6 * 3600_000L
                val permanent = t is SmtpException && t.permanent
                retry = !tooLate && !permanent
                if (!retry) {
                    val why = Smtp.explain(t)
                    EventLog.add(this, false, "邮件发送失败：$why")
                    Notifier.status(this, "提醒邮件发送失败", why)
                }
            }
            jobFinished(params, retry)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
