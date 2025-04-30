package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavWrapper
import com.ccjizhang.ui.screens.budget.AddEditBudgetScreen

/**
 * 添加/编辑预算页面的包装组件
 */
@Composable
fun WrappedAddEditBudgetScreenUnified(
    navController: NavHostController,
    budgetId: Long? = null
) {
    NavWrapper(navController) { nav ->
        AddEditBudgetScreen(
            navController = nav,
            budgetId = budgetId
        )
    }
}
