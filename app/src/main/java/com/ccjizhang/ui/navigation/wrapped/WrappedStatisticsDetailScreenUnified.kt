package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavWrapper
import com.ccjizhang.ui.screens.stats.StatisticsDetailScreen

/**
 * 统计详情页面的包装组件
 */
@Composable
fun WrappedStatisticsDetailScreenUnified(
    navController: NavHostController,
    type: String
) {
    NavWrapper(navController) { nav ->
        StatisticsDetailScreen(
            navController = nav,
            type = type,
            onNavigateBack = { nav.navigateUp() }
        )
    }
}
