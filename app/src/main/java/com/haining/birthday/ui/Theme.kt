package com.haining.birthday.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.haining.birthday.data.EventType

private val Light = lightColorScheme(
    primary = Color(0xFFE2545C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = Color(0xFF410008),
    secondary = Color(0xFF775654),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD7),
    onSecondaryContainer = Color(0xFF2C1514),
    tertiary = Color(0xFF7458C8),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF231919),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF231919),
    surfaceVariant = Color(0xFFF5DDDB),
    onSurfaceVariant = Color(0xFF6B5856),
    outline = Color(0xFF9C8987),
    outlineVariant = Color(0xFFE6D2D0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0EE),
    surfaceContainer = Color(0xFFFCEAE8),
    surfaceContainerHigh = Color(0xFFF6E4E2),
    surfaceContainerHighest = Color(0xFFF1DEDD),
    error = Color(0xFFBA1A1A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB3B0),
    onPrimary = Color(0xFF5F1418),
    primaryContainer = Color(0xFF7E2A2F),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = Color(0xFFE7BDB9),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3D),
    onSecondaryContainer = Color(0xFFFFDAD7),
    tertiary = Color(0xFFCDBDFF),
    background = Color(0xFF1A1111),
    onBackground = Color(0xFFF1DEDD),
    surface = Color(0xFF1A1111),
    onSurface = Color(0xFFF1DEDD),
    surfaceVariant = Color(0xFF534342),
    onSurfaceVariant = Color(0xFFD8C2C0),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534342),
    surfaceContainerLowest = Color(0xFF140C0C),
    surfaceContainerLow = Color(0xFF231919),
    surfaceContainer = Color(0xFF271D1D),
    surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
    error = Color(0xFFFFB4AB),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

/** 每种类型的头像底色 */
fun typeColor(t: EventType): Color = when (t) {
    EventType.BIRTHDAY -> Color(0xFFF06A6A)
    EventType.ANNIVERSARY -> Color(0xFF8B6CEF)
    EventType.MEMORIAL -> Color(0xFF7A869A)
    EventType.OTHER -> Color(0xFF2BA59B)
}
