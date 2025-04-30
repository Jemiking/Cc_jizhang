package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavWrapper
import com.ccjizhang.ui.screens.budget.BudgetDetailScreen

/**
 * 预算详情页面的包装组件
 */
@Composable
fun WrappedBudgetDetailScreenUnified(
    navController: NavHostController,
    budgetId: Long
) {
    NavWrapper(navController) { nav ->
        BudgetDetailScreen(
            navController = nav,
            budgetId = budgetId
        )
    }
}
