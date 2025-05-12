package com.ccjizhang.ui.theme

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.ccjizhang.ui.viewmodels.ThemeViewModel

/**
 * 应用主题枚举类
 */
enum class AppTheme {
    SYSTEM,  // 跟随系统
    LIGHT,   // 浅色模式
    DARK     // 深色模式
}

// 创建颜色方案构建器函数
private fun createDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = SurfaceLight,
    onSecondary = SurfaceLight,
    onTertiary = SurfaceLight,
    onBackground = SurfaceLight,
    onSurface = SurfaceLight
)

private fun createLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Background,
    surface = SurfaceLight,
    onPrimary = SurfaceLight,
    onSecondary = SurfaceLight,
    onTertiary = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CCJiZhangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // 默认关闭动态颜色
    themeManager: ThemeManager = androidx.compose.ui.platform.LocalContext.current.applicationContext.let {
        (it as? com.ccjizhang.CCJiZhangApp)?.themeManager
    } ?: error("无法获取ThemeManager实例"),
    content: @Composable () -> Unit
) {
    // 使用ThemeManager的状态，确保整个应用共享同一个主题状态
    val themeState = themeManager.themeState.value

    // 根据保存的主题模式决定是否使用深色主题
    val useDarkTheme = when (themeState.themeMode) {
        ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeManager.ThemeMode.DARK -> true
        ThemeManager.ThemeMode.LIGHT -> false
    }

    // 使用自定义主色调
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> createDarkColorScheme(themeState.primaryColor)
        else -> createLightColorScheme(themeState.primaryColor)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        // 设置状态栏和导航栏颜色
        SideEffect {
            val window = (view.context as Activity).window

            // 设置导航栏颜色与应用背景色匹配
            window.navigationBarColor = colorScheme.surface.toArgb()

            // 设置导航栏内容颜色
            WindowCompat.getInsetsController(window, view).apply {
                // 状态栏颜色已在MainActivity中设置
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}