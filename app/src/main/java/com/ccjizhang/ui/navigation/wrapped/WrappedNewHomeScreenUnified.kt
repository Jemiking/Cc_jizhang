package com.ccjizhang.ui.navigation.wrapped

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavWrapper
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.screens.home.NewHomeScreen

/**
 * 新版主页面的包装组件
 */
@Composable
fun WrappedNewHomeScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        NewHomeScreen(
            navController = nav,
            onNavigateToTransactions = navParams.onNavigateToTransactions,
            onNavigateToAccounts = navParams.onNavigateToAccounts,
            onNavigateToBudgets = { nav.navigate(NavRoutes.AllBudgets) },
            onNavigateToAnalysis = { nav.navigate(NavRoutes.Analysis) },
            onNavigateToSettings = navParams.onNavigateToSettings,
            onNavigateToAddTransaction = { nav.navigate(NavRoutes.TransactionAdd) },
            onNavigateToSavingGoals = { nav.navigate(NavRoutes.SavingGoals) },
            onNavigateToRecurringTransactions = { nav.navigate(NavRoutes.RecurringTransactions) }
        )
    }
}
