package com.skye.llmusage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.skye.llmusage.data.ThemeMode

private val Indigo = Color(0xFF4355B9)
private val IndigoDim = Color(0xFFBAC3FF)
private val IndigoDeep = Color(0xFF2E3B8F)
private val OrangeWarn = Color(0xFFFF9F0A)
private val RedAlert = Color(0xFFFF453A)

private val LightScheme = lightColorScheme(primary = Indigo).copy(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoDim,
    onPrimaryContainer = IndigoDeep,
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E0F9),
    onSecondaryContainer = Color(0xFF181A2C),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEFEDF7),
    surfaceContainerHigh = Color(0xFFE9E7F2),
)

private val DarkScheme = darkColorScheme(primary = IndigoDim).copy(
    primary = IndigoDim,
    onPrimary = Color(0xFF10207A),
    primaryContainer = IndigoDeep,
    onPrimaryContainer = IndigoDim,
    secondary = Color(0xFFC5C4DD),
    secondaryContainer = Color(0xFF434459),
    onSecondaryContainer = Color(0xFFE1E0FA),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainer = Color(0xFF1B1B21),
    surfaceContainerHigh = Color(0xFF25252D),
)

/** 阈值色:与参考实现一致,<70 主题色 / ≥70 橙 / ≥90 红 */
@Composable
fun tierColor(pct: Float): Color = when {
    pct >= 90f -> RedAlert
    pct >= 70f -> OrangeWarn
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun LlmUsageTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
