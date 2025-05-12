package com.ccjizhang

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import androidx.core.view.WindowCompat
import com.ccjizhang.ui.MainAppComposable
import com.ccjizhang.ui.viewmodels.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // 获取ThemeViewModel实例
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用完全沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 添加必要的窗口标志
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // 设置状态栏颜色为蓝色，与应用顶部栏完全一致
        window.statusBarColor = com.ccjizhang.ui.theme.Primary.toArgb()

        // 设置状态栏图标颜色
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false // 深色背景下使用浅色图标

        // 使用简单的加载界面
        setContent {
            MainAppComposable()
        }
    }
}