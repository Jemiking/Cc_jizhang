package com.ccjizhang.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ccjizhang.ui.navigation.wrapped.WrappedNavAnalyticsReportScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedNavPerformanceReportScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedLogViewerScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedStatisticsDetailScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedAddEditBudgetScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedBudgetDetailScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedAccountCategoryManagementScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedAccountManagementScreenEnhancedUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedNewHomeScreenUnified
import com.ccjizhang.ui.navigation.wrapped.WrappedBudgetsScreenUnified

/**
 * 统一导航图
 * 使用NavRoutes作为唯一的路由定义来源
 */
@Composable
fun UnifiedNavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Home,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 主要导航
        composable(
            route = NavRoutes.Home,
            enterTransition = NavAnimations.horizontalSlideEnterTransition,
            exitTransition = NavAnimations.horizontalSlideExitTransition,
            popEnterTransition = NavAnimations.horizontalSlidePopEnterTransition,
            popExitTransition = NavAnimations.horizontalSlidePopExitTransition
        ) {
            WrappedNewHomeScreenUnified(navController = navController)
        }

        composable(
            route = NavRoutes.Transactions,
            enterTransition = NavAnimations.horizontalSlideEnterTransition,
            exitTransition = NavAnimations.horizontalSlideExitTransition,
            popEnterTransition = NavAnimations.horizontalSlidePopEnterTransition,
            popExitTransition = NavAnimations.horizontalSlidePopExitTransition
        ) {
            WrappedTransactionsScreenUnified(navController = navController)
        }

        composable(
            route = NavRoutes.Statistics,
            enterTransition = NavAnimations.horizontalSlideEnterTransition,
            exitTransition = NavAnimations.horizontalSlideExitTransition,
            popEnterTransition = NavAnimations.horizontalSlidePopEnterTransition,
            popExitTransition = NavAnimations.horizontalSlidePopExitTransition
        ) {
            WrappedStatsScreenUnified(navController = navController)
        }

        composable(
            route = NavRoutes.Accounts,
            enterTransition = NavAnimations.horizontalSlideEnterTransition,
            exitTransition = NavAnimations.horizontalSlideExitTransition,
            popEnterTransition = NavAnimations.horizontalSlidePopEnterTransition,
            popExitTransition = NavAnimations.horizontalSlidePopExitTransition
        ) {
            WrappedAccountsScreenUnified(navController = navController)
        }

        composable(
            route = NavRoutes.Settings,
            enterTransition = NavAnimations.horizontalSlideEnterTransition,
            exitTransition = NavAnimations.horizontalSlideExitTransition,
            popEnterTransition = NavAnimations.horizontalSlidePopEnterTransition,
            popExitTransition = NavAnimations.horizontalSlidePopExitTransition
        ) {
            WrappedSettingsScreenUnified(navController = navController)
        }

        // 账户管理页面
        composable(
            route = NavRoutes.AccountManagement,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.AccountManagement).invoke(this) }
        ) {
            WrappedAccountManagementScreenEnhancedUnified(navController = navController)
        }

        // 账户分类管理页面
        composable(
            route = NavRoutes.AccountCategoryManagement,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.AccountManagement).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.AccountManagement).invoke(this) }
        ) {
            WrappedAccountCategoryManagementScreenUnified(navController = navController)
        }

        // 分类管理页面
        composable(
            route = NavRoutes.CategoryManagement,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.CategoryManagement).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.CategoryManagement).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.CategoryManagement).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.CategoryManagement).invoke(this) }
        ) {
            WrappedCategoryManagementScreenUnified(navController = navController)
        }

        // 标签管理页面
        composable(
            route = NavRoutes.TagManagement,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.TagManagement).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.TagManagement).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.TagManagement).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.TagManagement).invoke(this) }
        ) {
            WrappedTagManagementScreenUnified(navController = navController)
        }

        // 数据备份与恢复页面
        composable(
            route = NavRoutes.DataBackup,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.DataBackup).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.DataBackup).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.DataBackup).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.DataBackup).invoke(this) }
        ) {
            WrappedUnifiedBackupScreenUnified(navController = navController)
        }



        // 安全设置页面
        composable(
            route = NavRoutes.SecuritySettings,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.SecuritySettings).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.SecuritySettings).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.SecuritySettings).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.SecuritySettings).invoke(this) }
        ) {
            WrappedUnifiedSecurityScreenUnified(navController = navController)
        }

        // WebDAV设置页面
        composable(
            route = NavRoutes.WebDavSettings,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.WebDavSettings).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.WebDavSettings).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.WebDavSettings).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.WebDavSettings).invoke(this) }
        ) {
            WrappedWebDavSettingsScreenUnified(navController = navController)
        }

        // 数据迁移页面
        composable(
            route = NavRoutes.DataMigration,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.DataMigration).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.DataMigration).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.DataMigration).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.DataMigration).invoke(this) }
        ) {
            WrappedDataMigrationScreenUnified(navController = navController)
        }

        // 账户转账页面
        composable(
            route = NavRoutes.AccountTransfer,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.AccountTransfer).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.AccountTransfer).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.AccountTransfer).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.AccountTransfer).invoke(this) }
        ) {
            WrappedAccountTransferScreenUnified(navController = navController)
        }

        // 账户余额调整页面
        composable(
            route = NavRoutes.AccountBalanceAdjust,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.AccountBalanceAdjust).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.AccountBalanceAdjust).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.AccountBalanceAdjust).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.AccountBalanceAdjust).invoke(this) }
        ) {
            WrappedAccountBalanceAdjustScreenUnified(navController = navController)
        }

        // 信用卡列表
        composable(
            route = NavRoutes.CreditCardList,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.CreditCardList).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.CreditCardList).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.CreditCardList).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.CreditCardList).invoke(this) }
        ) {
            WrappedCreditCardListScreenUnified(navController = navController)
        }

        // 信用卡详情
        composable(
            route = NavRoutes.CreditCardDetail,
            arguments = listOf(navArgument("creditCardId") { type = NavType.LongType }),
            enterTransition = { NavAnimations.verticalSlideEnterTransition.invoke(this) },
            exitTransition = { NavAnimations.verticalSlideExitTransition.invoke(this) },
            popEnterTransition = { NavAnimations.verticalSlidePopEnterTransition.invoke(this) },
            popExitTransition = { NavAnimations.verticalSlidePopExitTransition.invoke(this) }
        ) { backStackEntry ->
            val creditCardId = backStackEntry.arguments?.getLong("creditCardId") ?: 0
            WrappedCreditCardDetailScreenUnified(navController = navController, creditCardId = creditCardId)
        }

        // 信用卡还款
        composable(
            route = NavRoutes.CreditCardPayment,
            arguments = listOf(navArgument("creditCardId") { type = NavType.LongType }),
            enterTransition = { NavAnimations.scaleEnterTransition.invoke(this) },
            exitTransition = { NavAnimations.scaleExitTransition.invoke(this) },
            popEnterTransition = { NavAnimations.scaleEnterTransition.invoke(this) },
            popExitTransition = { NavAnimations.scaleExitTransition.invoke(this) }
        ) { backStackEntry ->
            val creditCardId = backStackEntry.arguments?.getLong("creditCardId") ?: 0
            WrappedCreditCardPaymentScreenUnified(navController = navController, creditCardId = creditCardId)
        }

        // 主题设置页面
        composable(
            route = NavRoutes.ThemeSettings,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.ThemeSettings).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.ThemeSettings).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.ThemeSettings).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.ThemeSettings).invoke(this) }
        ) {
            WrappedThemeSettingsScreenUnified(navController = navController)
        }

        // 货币设置页面
        composable(
            route = NavRoutes.CurrencySettings,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.CurrencySettings).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.CurrencySettings).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.CurrencySettings).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.CurrencySettings).invoke(this) }
        ) {
            WrappedCurrencySettingsScreenUnified(navController = navController)
        }

        // 通知设置页面
        composable(
            route = NavRoutes.NotificationSettings,
            enterTransition = { NavAnimations.getEnterTransitionByRoute(NavRoutes.NotificationSettings).invoke(this) },
            exitTransition = { NavAnimations.getExitTransitionByRoute(NavRoutes.NotificationSettings).invoke(this) },
            popEnterTransition = { NavAnimations.getPopEnterTransitionByRoute(NavRoutes.NotificationSettings).invoke(this) },
            popExitTransition = { NavAnimations.getPopExitTransitionByRoute(NavRoutes.NotificationSettings).invoke(this) }
        ) {
            WrappedNotificationSettingsScreenUnified(navController = navController)
        }



        // 添加交易页面
        composable(
            route = NavRoutes.TransactionAdd,
            enterTransition = NavAnimations.scaleEnterTransition,
            exitTransition = NavAnimations.scaleExitTransition,
            popEnterTransition = NavAnimations.scaleEnterTransition,
            popExitTransition = NavAnimations.scaleExitTransition
        ) {
            WrappedAddEditTransactionScreenUnified(navController = navController)
        }

        // 编辑交易页面
        composable(
            route = NavRoutes.TransactionEdit,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = NavAnimations.scaleEnterTransition,
            exitTransition = NavAnimations.scaleExitTransition,
            popEnterTransition = NavAnimations.scaleEnterTransition,
            popExitTransition = NavAnimations.scaleExitTransition
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            WrappedAddEditTransactionScreenUnified(
                navController = navController,
                transactionId = transactionId
            )
        }

        // 交易详情页面
        composable(
            route = NavRoutes.TransactionDetail,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = NavAnimations.verticalSlideEnterTransition,
            exitTransition = NavAnimations.verticalSlideExitTransition,
            popEnterTransition = NavAnimations.verticalSlidePopEnterTransition,
            popExitTransition = NavAnimations.verticalSlidePopExitTransition
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            WrappedTransactionDetailScreenUnified(
                navController = navController,
                transactionId = transactionId
            )
        }

        // 账户详情页面
        composable(
            route = NavRoutes.AccountDetail,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = NavAnimations.verticalSlideEnterTransition,
            exitTransition = NavAnimations.verticalSlideExitTransition,
            popEnterTransition = NavAnimations.verticalSlidePopEnterTransition,
            popExitTransition = NavAnimations.verticalSlidePopExitTransition
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            WrappedAccountDetailScreenUnified(accountId = accountId, navController = navController)
        }

        // 添加账户页面
        composable(
            route = NavRoutes.AccountAdd,
            enterTransition = NavAnimations.scaleEnterTransition,
            exitTransition = NavAnimations.scaleExitTransition,
            popEnterTransition = NavAnimations.scaleEnterTransition,
            popExitTransition = NavAnimations.scaleExitTransition
        ) {
            WrappedAddEditAccountScreenUnified(
                navController = navController,
                accountId = null
            )
        }

        // 编辑账户页面
        composable(
            route = NavRoutes.AccountEdit,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.LongType
                }
            ),
            enterTransition = NavAnimations.scaleEnterTransition,
            exitTransition = NavAnimations.scaleExitTransition,
            popEnterTransition = NavAnimations.scaleEnterTransition,
            popExitTransition = NavAnimations.scaleExitTransition
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
            WrappedAddEditAccountScreenUnified(
                navController = navController,
                accountId = accountId
            )
        }

        // 账户转账页面
        composable(route = NavRoutes.AccountTransfer) {
            WrappedAccountTransferScreenUnified(navController = navController)
        }

        // 账户余额调整页面
        composable(route = NavRoutes.AccountBalanceAdjust) {
            WrappedAccountBalanceAdjustScreenUnified(navController = navController)
        }

        // ===== 高级功能相关导航 =====

        // 储蓄目标列表页面
        composable(route = NavRoutes.SavingGoals) {
            WrappedSavingGoalScreenUnified(navController = navController)
        }

        // 储蓄目标添加页面
        composable(route = NavRoutes.SavingGoalAdd) {
            WrappedSavingGoalAddEditScreenUnified(navController = navController, goalId = 0L)
        }

        // 储蓄目标编辑页面
        composable(
            route = NavRoutes.SavingGoalEdit,
            arguments = listOf(
                navArgument("goalId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId") ?: 0L
            WrappedSavingGoalAddEditScreenUnified(navController = navController, goalId = goalId)
        }

        // 储蓄目标详情页面
        composable(
            route = NavRoutes.SavingGoalDetail,
            arguments = listOf(
                navArgument("goalId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong("goalId") ?: 0L
            WrappedSavingGoalDetailScreenUnified(navController = navController, goalId = goalId)
        }

        // 定期交易列表页面
        composable(route = NavRoutes.RecurringTransactions) {
            WrappedRecurringTransactionScreenUnified(navController = navController)
        }

        // 定期交易添加页面
        composable(route = NavRoutes.RecurringTransactionAdd) {
            WrappedRecurringTransactionAddEditScreenUnified(navController = navController, transactionId = 0L)
        }

        // 定期交易编辑页面
        composable(
            route = NavRoutes.RecurringTransactionEdit,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            WrappedRecurringTransactionAddEditScreenUnified(navController = navController, transactionId = transactionId)
        }

        // 定期交易详情页面
        composable(
            route = NavRoutes.RecurringTransactionDetail,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            WrappedRecurringTransactionDetailScreenUnified(navController = navController, transactionId = transactionId)
        }

        // 投资列表页面
        composable(route = NavRoutes.Investments) {
            WrappedInvestmentScreenUnified(navController = navController)
        }

        // 添加投资页面
        composable(route = NavRoutes.InvestmentAdd) {
            WrappedInvestmentAddEditScreenUnified(navController = navController, investmentId = 0L)
        }

        // 编辑投资页面
        composable(
            route = NavRoutes.InvestmentEdit,
            arguments = listOf(
                navArgument("investmentId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val investmentId = backStackEntry.arguments?.getLong("investmentId") ?: 0L
            WrappedInvestmentAddEditScreenUnified(navController = navController, investmentId = investmentId)
        }

        // 投资详情页面
        composable(
            route = NavRoutes.InvestmentDetail,
            arguments = listOf(
                navArgument("investmentId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val investmentId = backStackEntry.arguments?.getLong("investmentId") ?: 0L
            WrappedInvestmentDetailScreenUnified(navController = navController, investmentId = investmentId)
        }

        // 财务报告列表页面
        composable(route = NavRoutes.FinancialReports) {
            WrappedFinancialReportScreenUnified(navController = navController)
        }

        // 导航分析报告页面
        composable(
            route = NavRoutes.NavAnalyticsReport,
            enterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            exitTransition = { NavAnimations.fadeExitTransition.invoke(this) },
            popEnterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            popExitTransition = { NavAnimations.fadeExitTransition.invoke(this) }
        ) {
            WrappedNavAnalyticsReportScreenUnified(navController = navController)
        }

        // 导航性能报告页面
        composable(
            route = NavRoutes.NavPerformanceReport,
            enterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            exitTransition = { NavAnimations.fadeExitTransition.invoke(this) },
            popEnterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            popExitTransition = { NavAnimations.fadeExitTransition.invoke(this) }
        ) {
            WrappedNavPerformanceReportScreenUnified(navController = navController)
        }

        // 日志查看器页面
        composable(
            route = NavRoutes.LogViewer,
            enterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            exitTransition = { NavAnimations.fadeExitTransition.invoke(this) },
            popEnterTransition = { NavAnimations.fadeEnterTransition.invoke(this) },
            popExitTransition = { NavAnimations.fadeExitTransition.invoke(this) }
        ) {
            WrappedLogViewerScreenUnified(navController = navController)
        }

        // 生成报告页面
        composable(route = NavRoutes.GenerateReport) {
            WrappedGenerateReportScreenUnified(navController = navController)
        }

        // 财务报告详情页面
        composable(
            route = NavRoutes.FinancialReportDetail,
            arguments = listOf(
                navArgument("reportId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            WrappedFinancialReportDetailScreenUnified(navController = navController, reportId = reportId)
        }

        // 统计详情页面
        composable(
            route = NavRoutes.StatisticsDetail,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                }
            ),
            enterTransition = NavAnimations.verticalSlideEnterTransition,
            exitTransition = NavAnimations.verticalSlideExitTransition,
            popEnterTransition = NavAnimations.verticalSlidePopEnterTransition,
            popExitTransition = NavAnimations.verticalSlidePopExitTransition
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "expense"
            WrappedStatisticsDetailScreenUnified(navController = navController, type = type)
        }

        // ===== 预算相关导航 =====

        // 预算列表页面
        composable(route = NavRoutes.AllBudgets) {
            WrappedBudgetsScreenUnified(navController = navController)
        }

        // 添加/编辑预算页面
        composable(route = NavRoutes.AddEditBudget) {
            WrappedAddEditBudgetScreenUnified(navController = navController)
        }

        // 预算详情页面
        composable(
            route = NavRoutes.BudgetDetail,
            arguments = listOf(
                navArgument("budgetId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: 0L
            WrappedBudgetDetailScreenUnified(navController = navController, budgetId = budgetId)
        }

        // ===== 家庭成员相关导航 =====

        // 家庭成员列表页面
        composable(route = NavRoutes.FamilyMembers) {
            WrappedFamilyMemberScreenUnified(navController = navController)
        }

        // 家庭成员添加页面
        composable(route = NavRoutes.FamilyMemberAdd) {
            WrappedFamilyMemberAddEditScreenUnified(navController = navController, memberId = 0L)
        }

        // 家庭成员编辑页面
        composable(
            route = NavRoutes.FamilyMemberEdit,
            arguments = listOf(
                navArgument("memberId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 0L
            WrappedFamilyMemberAddEditScreenUnified(navController = navController, memberId = memberId)
        }
    }
}
