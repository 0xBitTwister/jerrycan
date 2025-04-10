package com.example.jerrycan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 使用Nordic风格颜色创建亮色主题
private val LightColorScheme = lightColorScheme(
    primary = NordicBlue,
    onPrimary = Color.White,
    primaryContainer = NordicLightBlue,
    onPrimaryContainer = NordicDarkBlue,
    secondary = NordicDarkBlue,
    onSecondary = Color.White,
    tertiary = NordicGreen,
    onTertiary = Color.White,
    error = NordicRed,
    background = NordicBackground,
    onBackground = NordicDarkGray,
    surface = Color.White,
    onSurface = NordicDarkGray
)

// 使用Nordic风格颜色创建暗色主题
private val DarkColorScheme = darkColorScheme(
    primary = NordicLightBlue,
    onPrimary = Color.Black,
    primaryContainer = NordicDarkBlue,
    onPrimaryContainer = NordicLightBlue,
    secondary = NordicBlue,
    onSecondary = Color.Black,
    tertiary = NordicGreen,
    onTertiary = Color.Black,
    error = NordicRed,
    background = NordicDarkBackground,
    onBackground = NordicLightGray,
    surface = NordicDarkGray,
    onSurface = NordicLightGray
)

@Composable
fun JerryCanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // 恢复状态栏颜色设置为主题色
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            
            // 移除导航栏颜色设置，恢复默认样式
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color(0xFFF7F7F7).toArgb()
            
            // 使用新的API设置系统栏行为
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // 设置状态栏和导航栏文字颜色
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}