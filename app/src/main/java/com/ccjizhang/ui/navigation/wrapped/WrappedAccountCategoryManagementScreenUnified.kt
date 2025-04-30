package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.screens.account.AccountCategoryManagementScreen

/**
 * 账户分类管理页面的包装组件
 */
@Composable
fun WrappedAccountCategoryManagementScreenUnified(
    navController: NavHostController
) {
    AccountCategoryManagementScreen(navController = navController)
}
