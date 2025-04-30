package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.screens.accounts.AccountManagementScreenEnhanced

/**
 * 增强版账户管理页面的包装组件
 */
@Composable
fun WrappedAccountManagementScreenEnhancedUnified(
    navController: NavHostController
) {
    AccountManagementScreenEnhanced(navController = navController)
}
