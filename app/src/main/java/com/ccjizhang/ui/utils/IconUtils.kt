package com.ccjizhang.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 图标工具类
 */
object IconUtils {
    // 图标缓存，避免重复创建图标对象
    private val iconCache = mutableMapOf<String, ImageVector>()

    // 常用图标预加载列表
    private val commonIcons = listOf(
        "Category", "Tag", "AccountBalance", "Backup", "Security",
        "Delete", "DarkMode", "Palette", "Settings"
    )

    // 是否已初始化
    private var isInitialized = false

    /**
     * 初始化常用图标缓存
     */
    fun initializeCommonIcons() {
        if (isInitialized) return

        // 预加载常用图标
        commonIcons.forEach { iconName ->
            getIconForNameInternal(iconName)?.let { icon ->
                iconCache[iconName] = icon
            }
        }

        isInitialized = true
    }

    /**
     * 根据图标名称获取图标（内部实现，不使用缓存）
     */
    private fun getIconForNameInternal(iconName: String): ImageVector? {
        return when (iconName) {
            "Category" -> Icons.Default.Category
            "Tag" -> Icons.Default.Tag
            "AccountBalance" -> Icons.Default.AccountBalance
            "Backup" -> Icons.Default.Backup
            "Security" -> Icons.Default.Security
            "Delete" -> Icons.Default.Delete
            "DarkMode" -> Icons.Default.DarkMode
            "Palette" -> Icons.Default.Palette
            "Language" -> Icons.Default.Language
            "Notifications" -> Icons.Default.Notifications
            "Settings" -> Icons.Default.Settings
            "Star" -> Icons.Default.Star
            "AttachMoney" -> Icons.Default.AttachMoney
            "Person" -> Icons.Default.Person
            "ImportExport" -> Icons.Default.ImportExport
            "Analytics" -> Icons.Default.Analytics
            "Speed" -> Icons.Default.Speed
            "Article" -> Icons.Default.Article
            "Info" -> Icons.Default.Info
            "Feedback" -> Icons.Default.Feedback
            else -> Icons.Default.Settings
        }
    }

    /**
     * 根据图标名称获取图标（使用缓存）
     */
    @Composable
    fun getIconForName(iconName: String): ImageVector {
        // 如果缓存中有，直接返回
        iconCache[iconName]?.let { return it }

        // 否则创建并缓存
        val icon = getIconForNameInternal(iconName) ?: Icons.Default.Settings
        iconCache[iconName] = icon
        return icon
    }

    /**
     * 清除图标缓存
     */
    fun clearIconCache() {
        iconCache.clear()
        isInitialized = false
    }
}
