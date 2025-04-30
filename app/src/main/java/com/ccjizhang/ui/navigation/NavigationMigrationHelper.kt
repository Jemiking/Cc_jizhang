package com.ccjizhang.ui.navigation

/**
 * 导航路由迁移辅助类
 * 用于帮助从AppDestinations迁移到NavRoutes
 * @deprecated 迁移已完成，此类将在未来版本中移除
 */
@Deprecated("迁移已完成，此类将在未来版本中移除")
object NavigationMigrationHelper {
    // 字符串到NavRoutes的映射
    private val routeMapping = mapOf(
        "dashboard" to NavRoutes.Dashboard,
        "all_transactions" to NavRoutes.AllTransactions,
        "add_transaction" to NavRoutes.AddTransaction,
        "add_edit_transaction" to NavRoutes.AddEditTransaction,
        "transaction_detail" to NavRoutes.TransactionDetail,
        "all_accounts" to NavRoutes.AllAccounts,
        "add_edit_account" to NavRoutes.AddEditAccount,
        "account_detail" to NavRoutes.AccountDetail,
        "categories" to NavRoutes.Categories,
        "add_edit_category" to NavRoutes.AddEditCategory,
        "category_detail" to NavRoutes.CategoryDetail,
        "account_management" to NavRoutes.AccountManagement,
        "all_budgets" to NavRoutes.AllBudgets,
        "add_edit_budget" to NavRoutes.AddEditBudget,
        "budget_detail" to NavRoutes.BudgetDetail,
        "analysis" to NavRoutes.Analysis,
        "settings" to NavRoutes.Settings,
        "recurring_transactions" to NavRoutes.RecurringTransactions,
        "add_edit_recurring_transaction" to NavRoutes.RecurringTransactionAdd,
        "recurring_transaction_detail" to NavRoutes.RecurringTransactionDetail,
        "investments" to NavRoutes.Investments,
        "add_edit_investment" to NavRoutes.InvestmentAdd,
        "investment_detail" to NavRoutes.InvestmentDetail,
        "financial_reports" to NavRoutes.FinancialReports,
        "financial_report_detail" to NavRoutes.FinancialReportDetail,
        "saving_goals" to NavRoutes.SavingGoals,
        "add_edit_saving_goal" to NavRoutes.SavingGoalAdd,
        "saving_goal_detail" to NavRoutes.SavingGoalDetail
    )

    // 字符串字面量到NavRoutes的映射
    private val stringLiteralMapping = mapOf(
        "data_backup" to NavRoutes.DataBackup,
        "auto_backup_setting" to NavRoutes.AutoBackupSetting,
        "security_settings" to NavRoutes.SecuritySettings,
        "webdav_settings" to NavRoutes.WebDavSettings,
        "data_migration" to NavRoutes.DataMigration
    )

    /**
     * 将字符串路由转换为NavRoutes路由
     * @deprecated 迁移已完成，此方法将在未来版本中移除
     */
    @Deprecated("迁移已完成，此方法将在未来版本中移除")
    fun convertToNavRoutes(route: String): String {
        return routeMapping[route] ?: stringLiteralMapping[route] ?: route
    }

    /**
     * 将导航调用从AppDestinations转换为NavRoutes
     * 例如：navController.navigate("dashboard") -> navController.navigate(NavRoutes.Dashboard)
     * @deprecated 迁移已完成，此方法将在未来版本中移除
     */
    @Deprecated("迁移已完成，此方法将在未来版本中移除")
    fun convertNavigationCall(code: String): String {
        var result = code

        // 替换字符串常量
        routeMapping.forEach { (route, navRoute) ->
            result = result.replace("\"$route\"", "NavRoutes.$navRoute")
        }

        // 替换字符串字面量
        stringLiteralMapping.forEach { (literal, navRoute) ->
            result = result.replace("\"$literal\"", "NavRoutes.$navRoute")
        }

        return result
    }
}
