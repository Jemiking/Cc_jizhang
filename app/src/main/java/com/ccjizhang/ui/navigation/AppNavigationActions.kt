package com.ccjizhang.ui.navigation

import androidx.navigation.NavHostController

/**
 * 应用导航动作
 * @deprecated 使用UnifiedNavigationActions代替，此类将在未来版本中移除
 */
@Deprecated("使用UnifiedNavigationActions代替，此类将在未来版本中移除")
class AppNavigationActions(private val navController: NavHostController) {

    // 返回上一级
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }

    // 导航到添加交易页面
    val navigateToAddTransaction: () -> Unit = {
        navController.navigate(NavRoutes.AddTransaction)
    }

    // 导航到编辑交易页面
    fun navigateToEditTransaction(transactionId: Long) {
        navController.navigate(NavRoutes.addTransaction(transactionId))
    }

    // 导航到所有交易页面
    val navigateToAllTransactions: () -> Unit = {
        navController.navigate(NavRoutes.Transactions)
    }

    // 导航到交易详情页面
    fun navigateToTransactionDetail(transactionId: Long) {
        navController.navigate(NavRoutes.transactionDetail(transactionId))
    }

    // 导航到所有账户页面
    val navigateToAccounts: () -> Unit = {
        navController.navigate(NavRoutes.Accounts)
    }

    // 导航到添加账户页面
    val navigateToAddAccount: () -> Unit = {
        navController.navigate(NavRoutes.AddAccount)
    }

    // 导航到编辑账户页面
    fun navigateToEditAccount(accountId: Long) {
        navController.navigate(NavRoutes.editAccount(accountId))
    }

    // 导航到账户详情页面
    fun navigateToAccountDetail(accountId: Long) {
        navController.navigate(NavRoutes.accountDetail(accountId))
    }

    // 导航到所有分类页面
    val navigateToCategories: () -> Unit = {
        navController.navigate(NavRoutes.Categories)
    }

    // 导航到添加分类页面
    val navigateToAddCategory: () -> Unit = {
        navController.navigate(NavRoutes.AddCategory)
    }

    // 导航到编辑分类页面
    fun navigateToEditCategory(categoryId: Long) {
        navController.navigate(NavRoutes.editCategory(categoryId))
    }

    // 导航到分类详情页面
    fun navigateToCategoryDetail(categoryId: Long) {
        navController.navigate(NavRoutes.categoryDetail(categoryId))
    }

    // 导航到所有预算页面
    val navigateToBudgets: () -> Unit = {
        navController.navigate(NavRoutes.AllBudgets)
    }

    // 导航到添加预算页面
    val navigateToAddBudget: () -> Unit = {
        navController.navigate(NavRoutes.AddBudget)
    }

    // 导航到编辑预算页面
    fun navigateToEditBudget(budgetId: Long) {
        navController.navigate(NavRoutes.editBudget(budgetId))
    }

    // 导航到预算详情页面
    fun navigateToBudgetDetail(budgetId: Long) {
        navController.navigate(NavRoutes.budgetDetail(budgetId))
    }

    // 导航到分析页面
    val navigateToAnalysis: () -> Unit = {
        navController.navigate(NavRoutes.Analysis)
    }

    // 导航到设置页面
    val navigateToSettings: () -> Unit = {
        navController.navigate(NavRoutes.Settings)
    }

    // 导航到定期交易页面
    val navigateToRecurringTransactions: () -> Unit = {
        navController.navigate(NavRoutes.RecurringTransactions)
    }

    // 导航到添加定期交易页面
    val navigateToAddRecurringTransaction: () -> Unit = {
        navController.navigate(NavRoutes.RecurringTransactionAdd)
    }

    // 导航到编辑定期交易页面
    fun navigateToEditRecurringTransaction(recurringTransactionId: Long) {
        navController.navigate(NavRoutes.recurringTransactionEdit(recurringTransactionId))
    }

    // 导航到定期交易详情页面
    fun navigateToRecurringTransactionDetail(recurringTransactionId: Long) {
        navController.navigate(NavRoutes.recurringTransactionDetail(recurringTransactionId))
    }

    // 导航到投资产品页面
    val navigateToInvestments: () -> Unit = {
        navController.navigate(NavRoutes.Investments)
    }

    // 导航到添加投资产品页面
    val navigateToAddInvestment: () -> Unit = {
        navController.navigate(NavRoutes.InvestmentAdd)
    }

    // 导航到编辑投资产品页面
    fun navigateToEditInvestment(investmentId: Long) {
        navController.navigate(NavRoutes.investmentEdit(investmentId))
    }

    // 导航到投资产品详情页面
    fun navigateToInvestmentDetail(investmentId: Long) {
        navController.navigate(NavRoutes.investmentDetail(investmentId))
    }

    // 导航到财务报告页面
    val navigateToFinancialReports: () -> Unit = {
        navController.navigate(NavRoutes.FinancialReports)
    }

    // 导航到财务报告详情页面
    fun navigateToFinancialReportDetail(reportId: Long) {
        navController.navigate(NavRoutes.financialReportDetail(reportId))
    }

    // 导航到储蓄目标页面
    val navigateToSavingGoals: () -> Unit = {
        navController.navigate(NavRoutes.SavingGoals)
    }

    // 导航到添加储蓄目标页面
    val navigateToAddSavingGoal: () -> Unit = {
        navController.navigate(NavRoutes.SavingGoalAdd)
    }

    // 导航到编辑储蓄目标页面
    fun navigateToEditSavingGoal(goalId: Long) {
        navController.navigate(NavRoutes.savingGoalEdit(goalId))
    }

    // 导航到储蓄目标详情页面
    fun navigateToSavingGoalDetail(goalId: Long) {
        navController.navigate(NavRoutes.savingGoalDetail(goalId))
    }
}