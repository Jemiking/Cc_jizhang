package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * 导航包装器组件，为Screen组件提供统一的导航参数
 *
 * @param navController 导航控制器
 * @param content 需要包装的内容，接收导航参数
 */
@Composable
fun NavWrapper(
    navController: NavHostController,
    content: @Composable (
        navController: NavHostController
    ) -> Unit
) {
    content(navController)
}

/**
 * 导航参数集合，包含各种导航相关的参数和回调
 * @deprecated 使用NavParametersUnified代替，此类将在未来版本中移除
 */
@Deprecated("使用NavParametersUnified代替，此类将在未来版本中移除")
data class NavParameters(
    val navController: NavHostController,
    val onNavigateBack: () -> Unit,
    val onNavigateToHome: () -> Unit = { navController.navigate(NavRoutes.Home) },
    val onNavigateToSettings: () -> Unit = { navController.navigate(NavRoutes.Settings) },
    val onNavigateToAccounts: () -> Unit = { navController.navigate(NavRoutes.Accounts) },
    val onNavigateToBudgets: () -> Unit = { navController.navigate(NavRoutes.AllBudgets) },
    val onNavigateToTransactions: () -> Unit = { navController.navigate(NavRoutes.Transactions) },
    val onNavigateToAnalysis: () -> Unit = { navController.navigate(NavRoutes.Analysis) },
    // 高级功能导航
    val onNavigateToSavingGoals: () -> Unit = { navController.navigate(NavRoutes.SavingGoals) },
    val onNavigateToRecurringTransactions: () -> Unit = { navController.navigate(NavRoutes.RecurringTransactions) },
    val onNavigateToInvestments: () -> Unit = { navController.navigate(NavRoutes.Investments) },
    val onNavigateToReports: () -> Unit = { navController.navigate(NavRoutes.FinancialReports) },
    val onNavigateToFamilySharing: () -> Unit = { navController.navigate(NavRoutes.FamilyMembers) }
)

/**
 * 创建导航参数集合
 * @deprecated 使用createUnifiedNavParameters代替，此函数将在未来版本中移除
 */
@Deprecated("使用createUnifiedNavParameters代替，此函数将在未来版本中移除")
@Composable
fun createNavParameters(navController: NavHostController): NavParameters {
    return NavParameters(
        navController = navController,
        onNavigateBack = { navController.navigateUp() }
    )
}

/**
 * 增强型导航包装器，使用NavParameters
 * @deprecated 使用NavWrapper代替，此函数将在未来版本中移除
 */
@Deprecated("使用NavWrapper代替，此函数将在未来版本中移除")
@Composable
fun NavWrapperEx(
    navController: NavHostController,
    content: @Composable (NavParameters) -> Unit
) {
    val navParameters = createNavParameters(navController)
    content(navParameters)
}