package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * 统一的导航参数集合，使用NavRoutes作为唯一的路由定义来源
 */
data class NavParametersUnified(
    val navController: NavHostController,
    val onNavigateBack: () -> Unit,
    // 主要导航
    val onNavigateToHome: () -> Unit = {
        navController.navigate(NavRoutes.Home) {
            launchSingleTop = true
            popUpTo(NavRoutes.Home) {
                saveState = true
            }
            restoreState = true
        }
    },
    val onNavigateToSettings: () -> Unit = {
        navController.navigate(NavRoutes.Settings) {
            launchSingleTop = true
            popUpTo(NavRoutes.Home)
            restoreState = true
        }
    },
    val onNavigateToAccounts: () -> Unit = {
        navController.navigate(NavRoutes.Accounts) {
            launchSingleTop = true
            popUpTo(NavRoutes.Home)
            restoreState = true
        }
    },
    val onNavigateToTransactions: () -> Unit = {
        navController.navigate(NavRoutes.Transactions) {
            launchSingleTop = true
            popUpTo(NavRoutes.Home)
            restoreState = true
        }
    },
    val onNavigateToStatistics: () -> Unit = {
        navController.navigate(NavRoutes.Statistics) {
            launchSingleTop = true
            popUpTo(NavRoutes.Home)
            restoreState = true
        }
    },

    // 账户相关
    val onNavigateToAccountManagement: () -> Unit = {
        navController.navigate(NavRoutes.AccountManagement) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountCategoryManagement: () -> Unit = {
        navController.navigate(NavRoutes.AccountCategoryManagement) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountAdd: () -> Unit = {
        navController.navigate(NavRoutes.AccountAdd) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountEdit: (Long) -> Unit = { accountId ->
        navController.navigate(NavRoutes.accountEdit(accountId)) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountDetail: (Long) -> Unit = { accountId ->
        navController.navigate(NavRoutes.accountDetail(accountId)) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountTransfer: () -> Unit = {
        navController.navigate(NavRoutes.AccountTransfer) {
            launchSingleTop = true
        }
    },
    val onNavigateToAccountBalanceAdjust: () -> Unit = {
        navController.navigate(NavRoutes.AccountBalanceAdjust) {
            launchSingleTop = true
        }
    },

    // 交易相关
    val onNavigateToTransactionAdd: () -> Unit = {
        navController.navigate(NavRoutes.TransactionAdd) {
            launchSingleTop = true
        }
    },
    val onNavigateToTransactionEdit: (Long) -> Unit = { transactionId ->
        navController.navigate(NavRoutes.transactionEdit(transactionId)) {
            launchSingleTop = true
        }
    },
    val onNavigateToTransactionDetail: (Long) -> Unit = { transactionId ->
        navController.navigate(NavRoutes.transactionDetail(transactionId)) {
            launchSingleTop = true
        }
    },

    // 设置相关
    val onNavigateToCategoryManagement: () -> Unit = {
        navController.navigate(NavRoutes.CategoryManagement) {
            launchSingleTop = true
        }
    },
    val onNavigateToTagManagement: () -> Unit = {
        navController.navigate(NavRoutes.TagManagement) {
            launchSingleTop = true
        }
    },
    val onNavigateToDataBackup: () -> Unit = {
        navController.navigate(NavRoutes.DataBackup) {
            launchSingleTop = true
        }
    },

    val onNavigateToSecuritySettings: () -> Unit = {
        navController.navigate(NavRoutes.SecuritySettings) {
            launchSingleTop = true
        }
    },
    val onNavigateToWebDavSettings: () -> Unit = {
        navController.navigate(NavRoutes.WebDavSettings) {
            launchSingleTop = true
        }
    },
    val onNavigateToDataMigration: () -> Unit = {
        navController.navigate(NavRoutes.DataMigration) {
            launchSingleTop = true
        }
    },
    val onNavigateToThemeSettings: () -> Unit = {
        navController.navigate(NavRoutes.ThemeSettings) {
            launchSingleTop = true
        }
    },

    // 高级功能导航
    val onNavigateToSavingGoals: () -> Unit = {
        navController.navigate(NavRoutes.SavingGoals) {
            launchSingleTop = true
        }
    },
    val onNavigateToRecurringTransactions: () -> Unit = {
        navController.navigate(NavRoutes.RecurringTransactions) {
            launchSingleTop = true
        }
    },
    val onNavigateToInvestments: () -> Unit = {
        navController.navigate(NavRoutes.Investments) {
            launchSingleTop = true
        }
    },
    val onNavigateToFinancialReports: () -> Unit = {
        navController.navigate(NavRoutes.FinancialReports) {
            launchSingleTop = true
        }
    },
    val onNavigateToFamilyMembers: () -> Unit = {
        navController.navigate(NavRoutes.FamilyMembers) {
            launchSingleTop = true
        }
    },

    // 信用卡相关
    val onNavigateToCreditCardList: () -> Unit = {
        navController.navigate(NavRoutes.CreditCardList) {
            launchSingleTop = true
        }
    },
    val onNavigateToCreditCardDetail: (Long) -> Unit = { creditCardId ->
        navController.navigate(NavRoutes.creditCardDetail(creditCardId)) {
            launchSingleTop = true
        }
    },
    val onNavigateToCreditCardPayment: (Long) -> Unit = { creditCardId ->
        navController.navigate(NavRoutes.creditCardPayment(creditCardId)) {
            launchSingleTop = true
        }
    },

    // 导航分析相关
    val onNavigateToNavAnalyticsReport: () -> Unit = {
        navController.navigate(NavRoutes.NavAnalyticsReport) {
            launchSingleTop = true
        }
    },

    // 导航性能相关
    val onNavigateToNavPerformanceReport: () -> Unit = {
        navController.navigate(NavRoutes.NavPerformanceReport) {
            launchSingleTop = true
        }
    },

    // 日志查看器
    val onNavigateToLogViewer: () -> Unit = {
        navController.navigate(NavRoutes.LogViewer) {
            launchSingleTop = true
        }
    }
)

/**
 * 创建统一的导航参数集合
 */
@Composable
fun createUnifiedNavParameters(navController: NavHostController): NavParametersUnified {
    return NavParametersUnified(
        navController = navController,
        onNavigateBack = { navController.navigateUp() }
    )
}

/**
 * 统一的导航包装器，使用NavParametersUnified
 */
@Composable
fun UnifiedNavWrapper(
    navController: NavHostController,
    content: @Composable (NavParametersUnified) -> Unit
) {
    val navParameters = createUnifiedNavParameters(navController)
    content(navParameters)
}
