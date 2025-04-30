package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.UnifiedNavWrapper
import com.ccjizhang.ui.screens.settings.LogViewerScreen

/**
 * 日志查看器页面的包装组件
 */
@Composable
fun WrappedLogViewerScreenUnified(navController: NavHostController) {
    UnifiedNavWrapper(navController) { navParameters ->
        LogViewerScreen(
            onNavigateBack = navParameters.onNavigateBack
        )
    }
}
