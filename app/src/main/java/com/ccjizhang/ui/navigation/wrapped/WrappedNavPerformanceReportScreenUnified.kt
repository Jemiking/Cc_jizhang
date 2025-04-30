package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.UnifiedNavWrapper
import com.ccjizhang.ui.screens.NavPerformanceReportScreen

/**
 * 导航性能报告页面的包装组件
 */
@Composable
fun WrappedNavPerformanceReportScreenUnified(navController: NavHostController) {
    UnifiedNavWrapper(navController) { navParameters ->
        NavPerformanceReportScreen(navParameters)
    }
}
