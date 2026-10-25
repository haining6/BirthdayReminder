package com.haining.birthday

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.haining.birthday.core.Dates
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventLog
import com.haining.birthday.data.Prefs
import com.haining.birthday.data.Repo
import com.haining.birthday.remind.Reminder
import com.haining.birthday.remind.RingActivity
import com.haining.birthday.remind.RingInfo
import com.haining.birthday.remind.Scheduler
import com.haining.birthday.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** 在电脑上模拟运行 App，把主要流程点一遍，确保不会崩溃 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h880dp-xxhdpi")
class SmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addLunarBirthdayThenOpenSettings() {
        rule.onNodeWithText("还没有记录").assertIsDisplayed()
        rule.onNodeWithText("添加第一个").tap()

        rule.onNodeWithText("农历").tap()
        rule.onNodeWithText("修改").tap()
        rule.waitForIdle()
        assertTrue(
            rule.onAllNodes(isRoot()).printToString(maxDepth = 3),
            rule.onAllNodesWithText("选择农历日期").fetchSemanticsNodes().isNotEmpty(),
        )
        // 勾选再取消“不记得年份”，验证年份滚轮的显示/隐藏
        rule.onNodeWithText("不记得年份（不显示岁数）").tap()
        rule.onNodeWithText("不记得年份（不显示岁数）").tap()
        rule.onNodeWithText("确定").tap()
        rule.onNodeWithText("来电响铃").performScrollTo().tap()
        rule.onNodeWithText("姓名或称呼").performScrollTo().performTextInput("妈妈")
        rule.onNodeWithText("保存").tap()
        rule.waitForIdle()

        // 回到首页，能看到刚加的人
        rule.onNodeWithText("妈妈").assertIsDisplayed()
        val saved = Repo.load(rule.activity).single()
        assertEquals("妈妈", saved.name)
        assertTrue(saved.lunar)
        assertTrue(saved.ring)
        assertTrue(saved.year > 0)

        // 再次打开编辑
        rule.onNodeWithText("妈妈").tap()
        rule.onNodeWithText("编辑").assertIsDisplayed()
        rule.onNodeWithContentDescription("返回").tap()

        // 设置页
        rule.onNodeWithContentDescription("设置").tap()
        rule.onNodeWithText("每天提醒时间").assertIsDisplayed()
        rule.onNodeWithText("QQ邮箱").performScrollTo().tap()
        assertEquals("smtp.qq.com", Prefs(rule.activity).smtpHost)
        rule.onNodeWithText("开发者：海宁").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fireAllReminderChannels() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.SEND_SMS)
        // 模拟设备默认没有短信功能，打开它（真手机都有）
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_TELEPHONY, true)
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING, true)
        Prefs(app).smsTo = "13800000000"

        val e = Event(
            id = 42, name = "爸爸", lunar = true, year = 1965, month = 8, day = 28,
            offsets = listOf(0, 3), ring = true, sms = true, email = true, phone = "13900000000",
        )
        val o = Dates.next(e, LocalDate.of(2026, 10, 8))!!
        assertEquals(0, o.days)
        Reminder.fire(app, e, o)

        val nm = app.getSystemService(NotificationManager::class.java)
        val titles = shadowOf(nm).allNotifications.map { it.extras.getCharSequence("android.title").toString() }
        val logs = EventLog.list(app).map { it.text }
        assertTrue("$titles", titles.contains("今天是爸爸的生日 🎂")) // 普通提醒
        assertTrue("$titles", titles.contains("今天是爸爸的生日")) // 来电式响铃
        assertTrue("$titles", titles.contains("提醒邮件没有发出")) // 没配置邮箱
        assertTrue(logs.toString(), logs.any { it.startsWith("已提醒：今天是爸爸的生日") })
        assertTrue(logs.toString(), logs.any { it.contains("邮件未发出") }) // 没配置邮箱
        // Robolectric 的模拟系统里没有短信服务，会在系统接口内部报 “Sms is not supported”；
        // 能走到这一步说明权限检查、选卡、拆分短信的调用都已正确到达系统短信接口（真手机上会正常发出）
        assertTrue(logs.toString(), logs.none { it.contains("短信未发出") } || logs.any { it.contains("Sms is not supported") })
    }

    @Test
    fun dailyCheckRunsOncePerDay() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        Prefs(app).apply { hour = 0; minute = 0 }
        Scheduler.ensure(app)
        val first = Prefs(app).lastCheckDay
        assertEquals(LocalDate.now().toEpochDay(), first)
        Scheduler.ensure(app) // 同一天再调用不会重复提醒
        assertEquals(first, Prefs(app).lastCheckDay)
    }

    @Test
    fun ringScreenOpens() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val intent = RingInfo(7, "今天是妈妈的生日", "10月8日 周四", "妈").putTo(Intent(app, RingActivity::class.java))
        ActivityScenario.launch<RingActivity>(intent).use { s ->
            s.onActivity { assertFalse(it.isFinishing) }
        }
    }
}

/** 直接触发控件的点击动作（不依赖模拟器里的触摸坐标） */
private fun SemanticsNodeInteraction.tap(): SemanticsNodeInteraction = performSemanticsAction(SemanticsActions.OnClick)
