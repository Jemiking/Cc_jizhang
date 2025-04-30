package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.screens.settings.UnifiedSecurityScreen

/**
 * 统一安全设置页面的包装组件
 */
@Composable
fun WrappedUnifiedSecurityScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        UnifiedSecurityScreen(navController = nav)
    }
}
