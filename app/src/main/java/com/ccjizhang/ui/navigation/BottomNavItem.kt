package com.ccjizhang.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项数据类
 */
data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasNews: Boolean = false,
    val badgeCount: Int? = null
) 