package com.ccjizhang

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

        // 不在这里设置状态栏颜色，让Theme.kt处理

        // 启用绘制到系统栏区域
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 添加必要的窗口标志
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // 不在这里设置状态栏图标颜色，让Theme.kt处理

        // 使用简单的加载界面
        setContent {
            MainAppComposable()
        }

        // 不再需要监听主题变更事件，主题变更通过Compose的状态系统自动应用
    }
}