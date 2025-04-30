package com.ccjizhang.ui.viewmodels

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.UserPreferencesRepository
import com.ccjizhang.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主题设置的ViewModel
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

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

    // 主题状态 - 使用Flow
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    // 主题状态 - 使用Compose的MutableState，确保UI能够立即响应变化
    private val _composeThemeState = mutableStateOf(ThemeState())
    val composeThemeState: State<ThemeState> = _composeThemeState

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
        viewModelScope.launch {
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

            // 创建新的主题状态
            val newThemeState = ThemeState(
                themeMode = themeMode,
                primaryColor = primaryColor,
                isLoading = false
            )

            // 同时更新两种状态
            _themeState.value = newThemeState
            _composeThemeState.value = newThemeState
        }
    }

    /**
     * 更改主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
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

            // 创建新的主题状态
            val newThemeState = _themeState.value.copy(themeMode = mode)

            // 同时更新两种状态
            _themeState.value = newThemeState
            _composeThemeState.value = newThemeState
        }
    }

    /**
     * 更新主题颜色
     */
    fun updateThemeColor(colorHex: String) {
        viewModelScope.launch {
            val colorIndex = availableColorsHex.indexOf(colorHex).takeIf { it >= 0 } ?: 0
            val color = availableColors[colorIndex]

            userPreferencesRepository.setUserThemeColor(colorHex)

            // 创建新的主题状态
            val newThemeState = _themeState.value.copy(primaryColor = color)

            // 同时更新两种状态
            _themeState.value = newThemeState
            _composeThemeState.value = newThemeState
        }
    }
}