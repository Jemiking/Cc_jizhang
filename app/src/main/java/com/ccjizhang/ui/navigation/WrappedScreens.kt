package com.ccjizhang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ccjizhang.ui.screens.home.HomeScreen
import com.ccjizhang.ui.screens.transactions.TransactionsScreen
import com.ccjizhang.ui.screens.stats.StatsScreen
import com.ccjizhang.ui.screens.budget.BudgetScreen
import com.ccjizhang.ui.screens.settings.SettingsScreen
import com.ccjizhang.ui.screens.settings.DataBackupScreen
import com.ccjizhang.ui.screens.settings.CategoryManagementScreen
import com.ccjizhang.ui.screens.settings.TagManagementScreen
import com.ccjizhang.ui.screens.accounts.AccountManagementScreen
import com.ccjizhang.ui.screens.accounts.AccountDetailScreen
import com.ccjizhang.ui.screens.accounts.AccountEditScreen
import com.ccjizhang.ui.screens.transactions.TransactionDetailScreen
import com.ccjizhang.ui.screens.transactions.AddEditTransactionScreen
import com.ccjizhang.ui.screens.ThemeSettingsScreen
import com.ccjizhang.ui.screens.NotificationSettingsScreen
import com.ccjizhang.ui.screens.settings.AutoBackupSettingScreen
import com.ccjizhang.ui.screens.settings.SecuritySettingsScreen
import com.ccjizhang.ui.screens.settings.WebDavSettingsScreen
import com.ccjizhang.ui.screens.settings.DataMigrationScreen
import com.ccjizhang.ui.screens.accounts.AccountTransferScreen
import com.ccjizhang.ui.screens.accounts.AccountBalanceAdjustScreen
import com.ccjizhang.ui.screens.accounts.CreditCardListScreen
import com.ccjizhang.ui.screens.accounts.CreditCardDetailScreen
import com.ccjizhang.ui.screens.accounts.CreditCardPaymentScreen
import com.ccjizhang.ui.screens.currencies.CurrencySettingsScreen
import com.ccjizhang.ui.screens.savinggoal.SavingGoalScreen
import com.ccjizhang.ui.screens.savinggoal.SavingGoalAddEditScreen
import com.ccjizhang.ui.screens.savinggoal.SavingGoalDetailScreen
import com.ccjizhang.ui.screens.recurring.RecurringTransactionScreen
import com.ccjizhang.ui.screens.recurring.RecurringTransactionAddEditScreen
import com.ccjizhang.ui.screens.recurring.RecurringTransactionDetailScreen
import com.ccjizhang.ui.screens.investment.InvestmentScreen
import com.ccjizhang.ui.screens.investment.InvestmentAddEditScreen
import com.ccjizhang.ui.screens.investment.InvestmentDetailScreen
import com.ccjizhang.ui.screens.report.FinancialReportScreen
import com.ccjizhang.ui.screens.report.FinancialReportDetailScreen
import com.ccjizhang.ui.screens.report.GenerateReportScreen
import com.ccjizhang.ui.screens.family.FamilyMemberScreen
import com.ccjizhang.ui.screens.family.FamilyMemberAddEditScreen
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavWrapper
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters

/**
 * 主页面的包装组件
 */
@Composable
fun WrappedHomeScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        HomeScreen(
            navController = nav,
            onNavigateToTransactions = navParams.onNavigateToTransactions,
            onNavigateToAccounts = navParams.onNavigateToAccounts,
            onNavigateToBudgets = { nav.navigate(NavRoutes.AllBudgets) },
            onNavigateToAnalysis = { nav.navigate(NavRoutes.Analysis) },
            onNavigateToSettings = navParams.onNavigateToSettings,
            onNavigateToAddTransaction = { nav.navigate(NavRoutes.TransactionAdd) },
            onNavigateToSavingGoals = { nav.navigate(NavRoutes.SavingGoals) },
            onNavigateToRecurringTransactions = { nav.navigate(NavRoutes.RecurringTransactions) },
            onNavigateToInvestments = { nav.navigate(NavRoutes.Investments) },
            onNavigateToReports = { nav.navigate(NavRoutes.FinancialReports) },
            onNavigateToFamilySharing = { nav.navigate(NavRoutes.FamilyMembers) }
        )
    }
}

/**
 * 交易页面的包装组件
 */
@Composable
fun WrappedTransactionsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        TransactionsScreen(
            navController = nav,
            onNavigateBack = navParams.onNavigateBack,
            onNavigateToAddTransaction = navParams.onNavigateToTransactionAdd,
            onNavigateToTransactionDetail = navParams.onNavigateToTransactionDetail
        )
    }
}

/**
 * 统计页面的包装组件
 */
@Composable
fun WrappedStatsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        StatsScreen(
            navController = nav,
            onNavigateBack = navParams.onNavigateBack,
            onNavigateToStatisticsDetail = { type -> nav.navigate(NavRoutes.statisticsDetail(type)) }
        )
    }
}

/**
 * 账户页面的包装组件
 */
@Composable
fun WrappedAccountsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        AccountManagementScreen(navController = nav)
    }
}

/**
 * 预算页面的包装组件
 */
@Composable
fun WrappedBudgetsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        BudgetScreen(
            navController = nav,
            onNavigateBack = navParams.onNavigateBack,
            onNavigateToAddBudget = { nav.navigate(NavRoutes.AddEditBudget) },
            onNavigateToBudgetDetail = { id -> nav.navigate(NavRoutes.budgetDetail(id)) }
        )
    }
}

/**
 * 设置页面的包装组件
 */
@Composable
fun WrappedSettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        val navParams = createUnifiedNavParameters(nav)
        SettingsScreen(
            navController = nav,
            onNavigateBack = navParams.onNavigateBack
        )
    }
}

/**
 * 数据备份页面的包装组件
 */
@Composable
fun WrappedDataBackupScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        DataBackupScreen(navController = nav)
    }
}

/**
 * 分类管理页面的包装组件
 */
@Composable
fun WrappedCategoryManagementScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        CategoryManagementScreen(
            navController = nav
        )
    }
}

/**
 * 标签管理页面的包装组件
 */
@Composable
fun WrappedTagManagementScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        TagManagementScreen(
            navController = nav
        )
    }
}

/**
 * 账户管理页面的包装组件
 */
@Composable
fun WrappedAccountManagementScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        AccountManagementScreen(navController = nav)
    }
}

/**
 * 账户详情页面的包装组件
 */
@Composable
fun WrappedAccountDetailScreenUnified(navController: NavHostController, accountId: Long) {
    NavWrapper(navController) { nav ->
        AccountDetailScreen(
            navController = nav,
            accountId = accountId
        )
    }
}

/**
 * 添加/编辑账户页面的包装组件
 */
@Composable
fun WrappedAddEditAccountScreenUnified(navController: NavHostController, accountId: Long?) {
    NavWrapper(navController) { nav ->
        AccountEditScreen(
            navController = nav,
            accountId = accountId
        )
    }
}

/**
 * 账户转账页面的包装组件
 */
@Composable
fun WrappedAccountTransferScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        AccountTransferScreen(
            navController = nav
        )
    }
}

/**
 * 账户余额调整页面的包装组件
 */
@Composable
fun WrappedAccountBalanceAdjustScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        AccountBalanceAdjustScreen(
            navController = nav
        )
    }
}

/**
 * 交易详情页面的包装组件
 */
@Composable
fun WrappedTransactionDetailScreenUnified(navController: NavHostController, transactionId: Long) {
    NavWrapper(navController) { nav ->
        TransactionDetailScreen(
            transactionId = transactionId,
            navController = nav
        )
    }
}

/**
 * 添加/编辑交易页面的包装组件
 */
@Composable
fun WrappedAddEditTransactionScreenUnified(navController: NavHostController, transactionId: Long? = null) {
    NavWrapper(navController) { nav ->
        // 处理可空类型，如果transactionId为null，则使用0L
        val nonNullTransactionId = transactionId ?: 0L
        AddEditTransactionScreen(
            navController = nav,
            transactionId = nonNullTransactionId
        )
    }
}

/**
 * 主题设置页面的包装组件
 */
@Composable
fun WrappedThemeSettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        ThemeSettingsScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() }
        )
    }
}



/**
 * 自动备份设置页面的包装组件
 */
@Composable
fun WrappedAutoBackupSettingScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        AutoBackupSettingScreen(navController = nav)
    }
}

/**
 * 安全设置页面的包装组件
 */
@Composable
fun WrappedSecuritySettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        SecuritySettingsScreen(navController = nav)
    }
}

/**
 * WebDAV设置页面的包装组件
 */
@Composable
fun WrappedWebDavSettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        WebDavSettingsScreen(navController = nav)
    }
}

/**
 * 数据迁移页面的包装组件
 */
@Composable
fun WrappedDataMigrationScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        DataMigrationScreen(navController = nav)
    }
}

/**
 * 货币设置页面的包装组件
 */
@Composable
fun WrappedCurrencySettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        CurrencySettingsScreen(
            navController = nav
        )
    }
}

/**
 * 通知设置页面的包装组件
 */
@Composable
fun WrappedNotificationSettingsScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        NotificationSettingsScreen(
            navController = nav
        )
    }
}

/**
 * 信用卡列表页面的包装组件
 */
@Composable
fun WrappedCreditCardListScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        CreditCardListScreen(navController = nav)
    }
}

/**
 * 信用卡详情页面的包装组件
 */
@Composable
fun WrappedCreditCardDetailScreenUnified(navController: NavHostController, creditCardId: Long) {
    NavWrapper(navController) { nav ->
        CreditCardDetailScreen(
            navController = nav,
            creditCardId = creditCardId
        )
    }
}

/**
 * 信用卡还款页面的包装组件
 */
@Composable
fun WrappedCreditCardPaymentScreenUnified(navController: NavHostController, creditCardId: Long) {
    NavWrapper(navController) { nav ->
        CreditCardPaymentScreen(
            navController = nav,
            creditCardId = creditCardId
        )
    }
}

// ===== 高级功能相关包装组件 =====

/**
 * 储蓄目标列表页面的包装组件
 */
@Composable
fun WrappedSavingGoalScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        SavingGoalScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() },
            onAddSavingGoal = { nav.navigate(NavRoutes.SavingGoalAdd) },
            onNavigateToSavingGoalDetail = { id -> nav.navigate(NavRoutes.savingGoalDetail(id)) }
        )
    }
}

/**
 * 储蓄目标添加/编辑页面的包装组件
 */
@Composable
fun WrappedSavingGoalAddEditScreenUnified(navController: NavHostController, goalId: Long) {
    NavWrapper(navController) { nav ->
        SavingGoalAddEditScreen(
            navController = nav,
            goalId = goalId
        )
    }
}

/**
 * 储蓄目标详情页面的包装组件
 */
@Composable
fun WrappedSavingGoalDetailScreenUnified(navController: NavHostController, goalId: Long) {
    NavWrapper(navController) { nav ->
        SavingGoalDetailScreen(
            goalId = goalId,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToEdit = { id -> nav.navigate(NavRoutes.savingGoalEdit(id)) }
        )
    }
}

/**
 * 定期交易列表页面的包装组件
 */
@Composable
fun WrappedRecurringTransactionScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        RecurringTransactionScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToAddRecurringTransaction = { nav.navigate(NavRoutes.RecurringTransactionAdd) },
            onNavigateToRecurringTransactionDetail = { id -> nav.navigate(NavRoutes.recurringTransactionDetail(id)) }
        )
    }
}

/**
 * 定期交易添加/编辑页面的包装组件
 */
@Composable
fun WrappedRecurringTransactionAddEditScreenUnified(navController: NavHostController, transactionId: Long) {
    NavWrapper(navController) { nav ->
        RecurringTransactionAddEditScreen(
            navController = nav,
            transactionId = transactionId,
            onNavigateBack = { nav.navigateUp() },
            onSaveSuccess = { nav.navigateUp() }
        )
    }
}

/**
 * 定期交易详情页面的包装组件
 */
@Composable
fun WrappedRecurringTransactionDetailScreenUnified(navController: NavHostController, transactionId: Long) {
    NavWrapper(navController) { nav ->
        RecurringTransactionDetailScreen(
            navController = nav,
            transactionId = transactionId,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToEditTransaction = { id -> nav.navigate(NavRoutes.recurringTransactionEdit(id)) },
            onDeleteSuccess = { nav.navigateUp() }
        )
    }
}

/**
 * 投资列表页面的包装组件
 */
@Composable
fun WrappedInvestmentScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        InvestmentScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToAddInvestment = { nav.navigate(NavRoutes.InvestmentAdd) },
            onNavigateToEditInvestment = { id -> nav.navigate(NavRoutes.investmentEdit(id)) },
            onNavigateToInvestmentDetail = { id -> nav.navigate(NavRoutes.investmentDetail(id)) }
        )
    }
}

/**
 * 投资添加/编辑页面的包装组件
 */
@Composable
fun WrappedInvestmentAddEditScreenUnified(navController: NavHostController, investmentId: Long) {
    NavWrapper(navController) { nav ->
        InvestmentAddEditScreen(
            navController = nav,
            investmentId = investmentId,
            onNavigateBack = { nav.navigateUp() },
            onSaveSuccess = { nav.navigateUp() }
        )
    }
}

/**
 * 投资详情页面的包装组件
 */
@Composable
fun WrappedInvestmentDetailScreenUnified(navController: NavHostController, investmentId: Long) {
    NavWrapper(navController) { nav ->
        InvestmentDetailScreen(
            navController = nav,
            investmentId = investmentId,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToEditInvestment = { id -> nav.navigate(NavRoutes.investmentEdit(id)) },
            onDeleteSuccess = { nav.navigateUp() }
        )
    }
}

/**
 * 财务报告列表页面的包装组件
 */
@Composable
fun WrappedFinancialReportScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        FinancialReportScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() },
            onNavigateToGenerateReport = { nav.navigate(NavRoutes.GenerateReport) },
            onNavigateToReportDetail = { id -> nav.navigate(NavRoutes.financialReportDetail(id)) }
        )
    }
}

/**
 * 财务报告详情页面的包装组件
 */
@Composable
fun WrappedFinancialReportDetailScreenUnified(navController: NavHostController, reportId: Long) {
    NavWrapper(navController) { nav ->
        FinancialReportDetailScreen(
            navController = nav,
            reportId = reportId,
            onNavigateBack = { nav.navigateUp() },
            onDeleteSuccess = { nav.navigateUp() }
        )
    }
}

/**
 * 生成报告页面的包装组件
 */
@Composable
fun WrappedGenerateReportScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        GenerateReportScreen(
            navController = nav,
            onNavigateBack = { nav.navigateUp() },
            onReportGenerated = { id -> nav.navigate(NavRoutes.financialReportDetail(id)) }
        )
    }
}

// ===== 家庭成员相关包装组件 =====

/**
 * 家庭成员列表页面的包装组件
 */
@Composable
fun WrappedFamilyMemberScreenUnified(navController: NavHostController) {
    NavWrapper(navController) { nav ->
        FamilyMemberScreen(
            navController = nav,
            onNavigateToAddFamilyMember = { nav.navigate(NavRoutes.FamilyMemberAdd) },
            onNavigateToEditFamilyMember = { id -> nav.navigate(NavRoutes.familyMemberEdit(id)) }
        )
    }
}

/**
 * 家庭成员添加/编辑页面的包装组件
 */
@Composable
fun WrappedFamilyMemberAddEditScreenUnified(navController: NavHostController, memberId: Long) {
    NavWrapper(navController) { nav ->
        FamilyMemberAddEditScreen(
            navController = nav,
            memberId = memberId,
            onSaveSuccess = { nav.navigateUp() }
        )
    }
}
