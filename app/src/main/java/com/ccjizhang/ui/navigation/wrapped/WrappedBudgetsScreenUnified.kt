package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.WrappedBudgetsScreenUnified

/**
 * 预算管理页面的包装组件
 */
@Composable
fun WrappedBudgetsScreenUnified(
    navController: NavHostController
) {
    com.ccjizhang.ui.navigation.WrappedBudgetsScreenUnified(navController = navController)
}
