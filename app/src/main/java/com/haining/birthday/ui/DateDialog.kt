package com.haining.birthday.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.haining.birthday.core.Lunar
import java.time.LocalDate
import java.time.YearMonth

/**
 * 年 / 月 / 日 三个滚轮。农历时月份会按所选年份自动加入闰月。
 * 回调里 year=0 表示年份不详。
 */
@Composable
fun DateDialog(
    lunar: Boolean,
    year: Int,
    month: Int,
    day: Int,
    leap: Boolean,
    defaultYear: Int,
    /** 新建时第一次选日期，默认显示年份滚轮 */
    showYearByDefault: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int, leap: Boolean) -> Unit,
) {
    val maxYear = minOf(LocalDate.now().year + 10, Lunar.MAX_YEAR)
    var knowYear by remember { mutableStateOf(year > 0 || showYearByDefault) }
    var y by remember { mutableIntStateOf((if (year > 0) year else defaultYear).coerceIn(Lunar.MIN_YEAR, maxYear)) }
    var m by remember { mutableIntStateOf(month) }
    var lp by remember { mutableStateOf(leap) }
    var d by remember { mutableIntStateOf(day) }

    val years = (Lunar.MIN_YEAR..maxYear).toList()

    // 月份列表：(月, 是否闰月)
    val months: List<Pair<Int, Boolean>> = buildList {
        for (i in 1..12) {
            add(i to false)
            if (lunar && knowYear && Lunar.leapMonth(y) == i) add(i to true)
        }
    }
    val mIdx = months.indexOf(m to lp).takeIf { it >= 0 } ?: months.indexOf(m to false)
    val (effM, effLeap) = months[mIdx]

    val dayCount = when {
        lunar && knowYear -> Lunar.daysIn(y, effM, effLeap)
        lunar -> 30
        else -> YearMonth.of(if (knowYear) y else 2000, effM).lengthOfMonth()
    }
    val effD = d.coerceIn(1, dayCount)

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)) {
                Text(if (lunar) "选择农历日期" else "选择公历日期", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (knowYear) {
                        Wheel(
                            years.map { "${it}年" }, years.indexOf(y),
                            onChange = { y = years[it] }, modifier = Modifier.weight(1.3f), wrap = false,
                        )
                    }
                    Wheel(
                        months.map { (mm, l) -> if (lunar) Lunar.monthName(mm, l) else "${mm}月" }, mIdx,
                        onChange = { months[it].let { (mm, l) -> m = mm; lp = l } }, modifier = Modifier.weight(1f),
                    )
                    Wheel(
                        (1..dayCount).map { if (lunar) Lunar.dayName(it) else "${it}日" }, effD - 1,
                        onChange = { d = it + 1 }, modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clickable { knowYear = !knowYear }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = !knowYear, onCheckedChange = { knowYear = !it })
                    Text("不记得年份（不显示岁数）", style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = { onConfirm(if (knowYear) y else 0, effM, effD, effLeap) }) { Text("确定") }
                }
            }
        }
    }
}
