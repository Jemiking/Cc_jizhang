package com.ccjizhang.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.theme.luminance
import com.ccjizhang.ui.viewmodels.ThemeViewModel

/**
 * 主题设置页面
 *
 * @param navController 导航控制器
 * @param onNavigateBack 返回上一页的回调
 */
@Composable
fun ThemeSettingsScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    // 获取ThemeManager实例
    val themeManager = androidx.compose.ui.platform.LocalContext.current.applicationContext.let {
        (it as? com.ccjizhang.CCJiZhangApp)?.themeManager
    } ?: error("无法获取ThemeManager实例")

    // 使用ThemeManager的状态，确保整个应用共享同一个主题状态
    val themeState = themeManager.themeState.value

    RoundedTopBarScaffold(
        title = "主题设置",
        onBackClick = { onNavigateBack() },
        showBackButton = true
    ) { paddingValues ->
        // 显示加载状态
        if (themeState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // 主题模式选择
                ThemeModeSection(
                    selectedMode = themeState.themeMode,
                    onModeSelected = { themeManager.setThemeMode(it) }
                )

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // 主题颜色选择
                ThemeColorSection(
                    selectedColor = themeManager.availableColorsHex[themeManager.availableColors.indexOf(themeState.primaryColor)],
                    availableColors = themeManager.availableColorsHex,
                    onColorSelected = { themeManager.updateThemeColor(it) }
                )
            }
        }
    }
}

/**
 * 主题模式选择部分
 */
@Composable
fun ThemeModeSection(
    selectedMode: com.ccjizhang.ui.theme.ThemeManager.ThemeMode,
    onModeSelected: (com.ccjizhang.ui.theme.ThemeManager.ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "主题模式",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 主题模式选项
        ThemeModeOption(
            title = "跟随系统",
            subtitle = "自动使用系统深色/浅色模式",
            icon = Icons.Default.Settings,
            isSelected = selectedMode == com.ccjizhang.ui.theme.ThemeManager.ThemeMode.SYSTEM,
            onClick = { onModeSelected(com.ccjizhang.ui.theme.ThemeManager.ThemeMode.SYSTEM) }
        )

        ThemeModeOption(
            title = "浅色模式",
            subtitle = "始终使用浅色主题",
            icon = Icons.Default.LightMode,
            isSelected = selectedMode == com.ccjizhang.ui.theme.ThemeManager.ThemeMode.LIGHT,
            onClick = { onModeSelected(com.ccjizhang.ui.theme.ThemeManager.ThemeMode.LIGHT) }
        )

        ThemeModeOption(
            title = "深色模式",
            subtitle = "始终使用深色主题",
            icon = Icons.Default.DarkMode,
            isSelected = selectedMode == com.ccjizhang.ui.theme.ThemeManager.ThemeMode.DARK,
            onClick = { onModeSelected(com.ccjizhang.ui.theme.ThemeManager.ThemeMode.DARK) }
        )
    }
}

/**
 * 单个主题模式选项
 */
@Composable
fun ThemeModeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

/**
 * 主题颜色选择部分
 */
@Composable
fun ThemeColorSection(
    selectedColor: String,
    availableColors: List<String>,
    onColorSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "主题颜色",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            items(availableColors) { colorHex ->
                ColorItem(
                    colorHex = colorHex,
                    isSelected = colorHex == selectedColor,
                    onClick = { onColorSelected(colorHex) }
                )
            }
        }
    }
}

/**
 * 单个颜色选项
 */
@Composable
fun ColorItem(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(android.graphics.Color.parseColor(colorHex))

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}