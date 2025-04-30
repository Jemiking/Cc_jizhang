package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.screens.settings.UnifiedBackupScreen

/**
 * 统一备份与恢复页面的包装组件
 */
@Composable
fun WrappedUnifiedBackupScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        UnifiedBackupScreen(navController = nav)
    }
}
