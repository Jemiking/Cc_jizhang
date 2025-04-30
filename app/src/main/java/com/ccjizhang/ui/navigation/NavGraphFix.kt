package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ccjizhang.ui.screens.settings.SettingsScreen

/**
 * 修复版导航图 - 用于解决账户管理页面导航问题
 * @deprecated 使用UnifiedNavGraph代替，此类将在未来版本中移除
 */
@Deprecated("使用UnifiedNavGraph代替，此类将在未来版本中移除")
@Composable
fun NavGraphFix(
    navController: NavHostController,
    startDestination: String = NavRoutes.Home,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 首页
        composable(route = NavRoutes.Home) {
            WrappedHomeScreenUnified(navController = navController)
        }

        // 交易列表页面
        composable(route = NavRoutes.Transactions) {
            WrappedTransactionsScreenUnified(navController = navController)
        }

        // 统计页面
        composable(route = NavRoutes.Statistics) {
            WrappedStatsScreenUnified(navController = navController)
        }

        // 账户页面
        composable(route = NavRoutes.Accounts) {
            WrappedAccountsScreenUnified(navController = navController)
        }

        // 设置页面
        composable(route = NavRoutes.Settings) {
            WrappedSettingsScreenUnified(navController = navController)
        }

        // 账户管理页面
        composable(route = NavRoutes.AccountManagement) {
            WrappedAccountManagementScreenUnified(navController = navController)
        }

        // 分类管理页面
        composable(route = NavRoutes.CategoryManagement) {
            WrappedCategoryManagementScreenUnified(navController = navController)
        }

        // 标签管理页面
        composable(route = NavRoutes.TagManagement) {
            WrappedTagManagementScreenUnified(navController = navController)
        }

        // 数据备份与恢复页面
        composable(route = NavRoutes.DataBackup) {
            WrappedDataBackupScreenUnified(navController = navController)
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

        // ===== 设置相关导航 =====

        // 自动备份设置页面 (已合并到DataBackup)
        composable(route = NavRoutes.AutoBackupSetting) {
            // 重定向到统一的备份与恢复页面
            WrappedUnifiedBackupScreenUnified(navController = navController)
        }

        // 安全设置页面
        composable(route = NavRoutes.SecuritySettings) {
            WrappedSecuritySettingsScreenUnified(navController = navController)
        }

        // 自定义分组管理页面 - 已移除
        composable(route = NavRoutes.CustomGroups) {
            // 返回到设置页面
            SettingsScreen(navController = navController)
        }

        // WebDAV设置页面
        composable(route = NavRoutes.WebDavSettings) {
            WrappedWebDavSettingsScreenUnified(navController = navController)
        }

        // 数据迁移页面
        composable(route = NavRoutes.DataMigration) {
            WrappedDataMigrationScreenUnified(navController = navController)
        }

        // 账户转账页面
        composable(route = NavRoutes.AccountTransfer) {
            WrappedAccountTransferScreenUnified(navController = navController)
        }

        // 账户余额调整页面
        composable(route = NavRoutes.AccountBalanceAdjust) {
            WrappedAccountBalanceAdjustScreenUnified(navController = navController)
        }

        // 信用卡列表
        composable(NavRoutes.CreditCardList) {
            WrappedCreditCardListScreenUnified(navController = navController)
        }

        // 信用卡详情
        composable(
            NavRoutes.CreditCardDetail,
            arguments = listOf(navArgument("creditCardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val creditCardId = backStackEntry.arguments?.getLong("creditCardId") ?: 0
            WrappedCreditCardDetailScreenUnified(navController = navController, creditCardId = creditCardId)
        }

        // 信用卡还款
        composable(
            NavRoutes.CreditCardPayment,
            arguments = listOf(navArgument("creditCardId") { type = NavType.LongType })
        ) { backStackEntry ->
            val creditCardId = backStackEntry.arguments?.getLong("creditCardId") ?: 0
            WrappedCreditCardPaymentScreenUnified(navController = navController, creditCardId = creditCardId)
        }

        // 主题设置页面
        composable(route = NavRoutes.ThemeSettings) {
            WrappedThemeSettingsScreenUnified(navController = navController)
        }



        // 添加交易页面
        composable(route = NavRoutes.TransactionAdd) {
            WrappedAddEditTransactionScreenUnified(navController = navController)
        }

        // 编辑交易页面
        composable(
            route = NavRoutes.TransactionEdit,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                }
            )
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
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            WrappedTransactionDetailScreenUnified(
                navController = navController,
                transactionId = transactionId
            )
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
    }
}
