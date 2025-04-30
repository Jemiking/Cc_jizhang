package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 原始导航图
 * @deprecated 使用UnifiedNavGraph代替，此类将在未来版本中移除
 */
@Deprecated("使用UnifiedNavGraph代替，此类将在未来版本中移除")
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Home,
    modifier: Modifier = Modifier
) {
    // 使用UnifiedNavGraph代替
    UnifiedNavGraph(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    )
}
