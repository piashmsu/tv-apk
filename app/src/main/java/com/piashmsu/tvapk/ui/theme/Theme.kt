package com.piashmsu.tvapk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Indigo = Color(0xFF7C5CFF)
private val Aqua = Color(0xFF22D3EE)
private val Sunset = Color(0xFFFF6B81)
private val Surface0 = Color(0xFF080A12)
private val Surface1 = Color(0xFF0E1220)
private val Surface2 = Color(0xFF161B2E)
private val OnSurface = Color(0xFFE6E8F2)

private val DarkScheme = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Aqua,
    onSecondary = Color.Black,
    tertiary = Sunset,
    onTertiary = Color.White,
    background = Surface0,
    onBackground = OnSurface,
    surface = Surface1,
    onSurface = OnSurface,
    surfaceVariant = Surface2,
    onSurfaceVariant = OnSurface.copy(alpha = 0.85f),
)

private val LightScheme = lightColorScheme(
    primary = Indigo,
    secondary = Aqua,
    tertiary = Sunset,
)

@Composable
fun TvApkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep brand identity by default
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = TvApkTypography, content = content)
}
