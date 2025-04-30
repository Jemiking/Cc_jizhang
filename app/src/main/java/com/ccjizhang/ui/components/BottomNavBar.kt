package com.ccjizhang.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.BottomNavItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.theme.CCJiZhangTheme

/**
 * 底部导航栏组件
 */
@Composable
fun BottomNavBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    items: List<BottomNavItem>
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        items.forEach { item ->
            AddNavItem(
                navItem = item,
                currentDestination = currentDestination,
                navController = navController
            )
        }
    }
}

/**
 * 添加单个导航项
 */
@Composable
fun RowScope.AddNavItem(
    navItem: BottomNavItem,
    currentDestination: NavDestination?,
    navController: NavHostController
) {
    val selected = currentDestination?.hierarchy?.any { it.route == navItem.route } == true

    NavigationBarItem(
        selected = selected,
        onClick = {
            navController.navigate(navItem.route) {
                // 避免创建新的导航堆栈
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                // 避免重复点击相同项目
                launchSingleTop = true
                // 恢复状态
                restoreState = true
            }
        },
        modifier = Modifier.padding(vertical = 4.dp),
        icon = {
            Icon(
                imageVector = if (selected) navItem.selectedIcon else navItem.unselectedIcon,
                contentDescription = navItem.title,
                modifier = Modifier.size(26.dp)
            )
        },
        label = {
            Text(
                text = navItem.title,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent
        )
    )
}

// 添加预览
@Preview(showBackground = true)
@Composable
fun PreviewBottomNavBar() {
    CCJiZhangTheme {
        val items = listOf(
            BottomNavItem(
                title = "首页",
                route = "home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home
            ),
            BottomNavItem(
                title = "我的",
                route = "profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person
            ),
            BottomNavItem(
                title = "设置",
                route = "settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings
            )
        )

        BottomNavBar(
            navController = rememberNavController(),
            currentDestination = null,
            items = items
        )
    }
}