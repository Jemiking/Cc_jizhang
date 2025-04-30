package com.ccjizhang.ui.theme

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.ccjizhang.data.repository.UserPreferencesRepository
import com.ccjizhang.ui.viewmodels.ThemeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 主题管理器 - 单例类，确保整个应用共享同一个主题状态
 */
@Singleton
class ThemeManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    // 创建一个协程作用域用于异步操作
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 主题模式
    enum class ThemeMode {
        SYSTEM,  // 跟随系统
        LIGHT,   // 浅色模式
        DARK     // 深色模式
    }

    // 主题状态数据类
    data class ThemeState(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val primaryColor: Color = Color(0xFF1976D2), // 默认蓝色
        val isLoading: Boolean = true
    )

    // 主题状态 - 使用Compose的MutableState，确保UI能够立即响应变化
    private val _themeState = mutableStateOf(ThemeState())
    val themeState: State<ThemeState> = _themeState

    // 可选的主题颜色列表
    val availableColors = listOf(
        Color(0xFF1976D2), // 蓝色
        Color(0xFF6200EE), // 紫色
        Color(0xFF03DAC5), // 青色
        Color(0xFF4CAF50), // 绿色
        Color(0xFFFF9800), // 橙色
        Color(0xFFF44336), // 红色
        Color(0xFFFF4081), // 粉色
        Color(0xFF795548), // 棕色
        Color(0xFF607D8B)  // 蓝灰色
    )

    // 颜色的Hex字符串表示
    val availableColorsHex = listOf(
        "#1976D2", // 蓝色
        "#6200EE", // 紫色
        "#03DAC5", // 青色
        "#4CAF50", // 绿色
        "#FF9800", // 橙色
        "#F44336", // 红色
        "#FF4081", // 粉色
        "#795548", // 棕色
        "#607D8B"  // 蓝灰色
    )

    init {
        loadThemeSettings()
    }

    /**
     * 加载主题设置
     */
    private fun loadThemeSettings() {
        scope.launch {
            // 加载主题模式
            val darkModeValue = userPreferencesRepository.getThemeMode()
            val themeMode = when(darkModeValue) {
                "system" -> ThemeMode.SYSTEM
                "dark" -> ThemeMode.DARK
                "light" -> ThemeMode.LIGHT
                else -> ThemeMode.SYSTEM
            }

            // 加载主题颜色
            val colorHex = userPreferencesRepository.getUserThemeColor()
            val colorIndex = availableColorsHex.indexOf(colorHex).takeIf { it >= 0 } ?: 0
            val primaryColor = availableColors[colorIndex]

            // 更新状态
            _themeState.value = ThemeState(
                themeMode = themeMode,
                primaryColor = primaryColor,
                isLoading = false
            )
        }
    }

    /**
     * 更改主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        scope.launch {
            val modeString = when(mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
            }
            userPreferencesRepository.setThemeMode(modeString)

            // 如果设置为深色或浅色模式，同时更新深色模式开关
            val darkModeEnabled = mode == ThemeMode.DARK
            if (mode != ThemeMode.SYSTEM) {
                userPreferencesRepository.setDarkModeEnabled(darkModeEnabled)
            }

            // 更新状态
            _themeState.value = _themeState.value.copy(themeMode = mode)
        }
    }

    /**
     * 更新主题颜色
     */
    fun updateThemeColor(colorHex: String) {
        scope.launch {
            val colorIndex = availableColorsHex.indexOf(colorHex).takeIf { it >= 0 } ?: 0
            val color = availableColors[colorIndex]

            userPreferencesRepository.setUserThemeColor(colorHex)

            // 更新状态
            _themeState.value = _themeState.value.copy(primaryColor = color)
        }
    }
}
