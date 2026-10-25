package com.haining.birthday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haining.birthday.core.Dates
import com.haining.birthday.core.Lunar
import com.haining.birthday.core.Occurrence
import com.haining.birthday.data.Event
import com.haining.birthday.data.EventType
import java.time.LocalDate

@Composable
fun HomeScreen(
    events: List<Event>,
    today: LocalDate,
    listState: LazyListState,
    onAdd: () -> Unit,
    onOpen: (Event) -> Unit,
    onSettings: () -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val all = remember(events, today) {
        events.mapNotNull { e -> Dates.next(e, today)?.let { e to it } }
            .sortedWith(compareBy({ it.second.days }, { it.first.name }))
    }
    val q = query.trim()
    val shown = if (q.isEmpty()) all else all.filter { (e, _) -> e.name.contains(q, true) || e.note.contains(q, true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("添加") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 96.dp),
        ) {
            item {
                Header(today, searching, onSearch = { searching = !searching; query = "" }, onSettings = onSettings)
            }
            if (searching) {
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("搜索名字或备注") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            if (all.isEmpty()) {
                item { EmptyState(onAdd) }
            } else {
                if (!searching) item { Hero(all.first().first, all.first().second, onOpen) }
                val soon = shown.filter { it.second.days <= 30 }
                val later = shown.filter { it.second.days > 30 }
                if (soon.isNotEmpty()) {
                    item { GroupTitle("30 天内 · ${soon.size}") }
                    items(soon, key = { it.first.id }) { (e, o) -> EventRow(e, o, onOpen) }
                }
                if (later.isNotEmpty()) {
                    item { GroupTitle("以后 · ${later.size}") }
                    items(later, key = { it.first.id }) { (e, o) -> EventRow(e, o, onOpen) }
                }
                if (shown.isEmpty()) item { GroupTitle("没有找到“$q”") }
            }
        }
    }
}

@Composable
private fun Header(today: LocalDate, searching: Boolean, onSearch: () -> Unit, onSettings: () -> Unit) {
    val ly = Lunar.fromSolar(today).year
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("生日提醒", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "${Dates.md(today)} ${Dates.week(today)} · 农历${Dates.lunarOf(today)} · ${Lunar.ganZhi(ly)}${Lunar.zodiac(ly)}年",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "搜索")
        }
        IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "设置") }
    }
}

@Composable
private fun Hero(e: Event, o: Occurrence, onOpen: (Event) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF9A8B), Color(0xFFE2545C), Color(0xFFC2416B))))
            .drawBehind {
                // 装饰圆
                drawCircle(Color.White.copy(alpha = 0.10f), radius = 90.dp.toPx(), center = Offset(size.width - 30.dp.toPx(), 0f))
                drawCircle(Color.White.copy(alpha = 0.07f), radius = 50.dp.toPx(), center = Offset(size.width - 110.dp.toPx(), size.height))
            }
            .clickable { onOpen(e) },
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                if (o.days == 0) "🎉 就是今天" else "下一个重要日子",
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Dates.title(e), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(Dates.detail(e, o), color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                if (o.days == 0) {
                    Text("今天", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${o.days}", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp)
                        Text("天", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp, start = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun EventRow(e: Event, o: Occurrence, onOpen: (Event) -> Unit) {
    Surface(
        onClick = { onOpen(e) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(e.name, e.type)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (e.type != EventType.BIRTHDAY) {
                        Spacer(Modifier.width(6.dp))
                        Tag(e.type.label, e.type)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    Dates.detail(e, o), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            DaysBadge(o.days)
        }
    }
}

@Composable
private fun DaysBadge(days: Int) {
    if (days == 0) {
        Text(
            "今天",
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    } else {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$days", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (days <= 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text("天后", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text("🎂", fontSize = 56.sp) }
        Spacer(Modifier.height(24.dp))
        Text("还没有记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "把家人朋友的生日、纪念日加进来，\n到日子前会准时提醒你。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("添加第一个")
        }
    }
}
