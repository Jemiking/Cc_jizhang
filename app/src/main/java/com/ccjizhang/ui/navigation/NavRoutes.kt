package com.ccjizhang.ui.navigation

/**
 * 应用程序的导航路由
 */
object NavRoutes {
    // 主要导航
    const val Home = "home"
    const val Accounts = "accounts"
    const val Transactions = "transactions"
    const val Statistics = "statistics"
    const val Settings = "settings"

    // 账户管理
    const val AccountManagement = "account_management"
    const val AccountCategoryManagement = "account_category_management"

    // 币种设置
    const val CurrencySettings = "currency_settings"
    const val MultiCurrencyReport = "multi_currency_report"

    // 从AppDestinations添加的路由
    const val Dashboard = "dashboard" // 替代 AppDestinations.DASHBOARD
    const val AllTransactions = "all_transactions" // 替代 AppDestinations.ALL_TRANSACTIONS
    const val AddTransaction = "add_transaction" // 替代 AppDestinations.ADD_TRANSACTION
    const val AddEditTransaction = "add_edit_transaction" // 替代 AppDestinations.ADD_EDIT_TRANSACTION
    const val AllAccounts = "all_accounts" // 替代 AppDestinations.ALL_ACCOUNTS
    const val AddEditAccount = "add_edit_account" // 替代 AppDestinations.ADD_EDIT_ACCOUNT
    const val AddAccount = "add_account" // 新增
    const val Categories = "categories" // 替代 AppDestinations.CATEGORIES
    const val AddEditCategory = "add_edit_category" // 替代 AppDestinations.ADD_EDIT_CATEGORY
    const val AddCategory = "add_category" // 新增
    const val CategoryDetail = "category_detail" // 替代 AppDestinations.CATEGORY_DETAIL
    const val AllBudgets = "all_budgets" // 替代 AppDestinations.ALL_BUDGETS
    const val AddEditBudget = "add_edit_budget" // 替代 AppDestinations.ADD_EDIT_BUDGET
    const val AddBudget = "add_budget" // 新增
    const val BudgetDetail = "budget_detail" // 替代 AppDestinations.BUDGET_DETAIL
    const val Analysis = "analysis" // 替代 AppDestinations.ANALYSIS

    // 添加字符串字面量路由
    const val DataBackup = "data_backup" // 统一备份与恢复页面
    const val AutoBackupSetting = "auto_backup_setting" // 自动备份设置页面（已合并到DataBackup）
    const val SecuritySettings = "security_settings" // 安全设置页面
    const val WebDavSettings = "webdav_settings" // WebDAV设置页面
    const val DataMigration = "data_migration" // 数据迁移页面
    const val CustomGroups = "custom_groups" // 自定义分组管理页面

    // 账户相关
    const val AccountDetail = "account_detail/{accountId}"
    const val AccountAdd = "account_add"
    const val AccountEdit = "account_edit/{accountId}"
    const val AccountTransfer = "account_transfer"
    const val AccountBalanceAdjust = "account_balance_adjust"

    // 信用卡相关
    const val CreditCardList = "credit_card_list"
    const val CreditCardDetail = "credit_card_detail/{creditCardId}"
    const val CreditCardAdd = "credit_card_add"
    const val CreditCardEdit = "credit_card_edit/{creditCardId}"
    const val CreditCardPayment = "credit_card_payment/{creditCardId}"

    // 交易相关
    const val TransactionDetail = "transaction_detail/{transactionId}"
    const val TransactionAdd = "transaction_add"
    const val TransactionEdit = "transaction_edit/{transactionId}"

    // 分类相关
    const val CategoryManagement = "category_management"
    const val CategoryAdd = "category_add"
    const val CategoryEdit = "category_edit/{categoryId}"

    // 标签相关
    const val TagManagement = "tag_management"

    // 统计相关
    const val StatisticsDetail = "statistics_detail/{type}"

    // 用户体验优化相关
    const val ThemeSettings = "theme_settings"
    const val NotificationSettings = "notification_settings"
    const val UserProfile = "user_profile"

    // 高级功能 - 目标储蓄计划
    const val SavingGoals = "saving_goals"
    const val SavingGoalDetail = "saving_goal_detail/{goalId}"
    const val SavingGoalAdd = "saving_goal_add"
    const val SavingGoalEdit = "saving_goal_edit/{goalId}"

    // 高级功能 - 定期交易自动化
    const val RecurringTransactions = "recurring_transactions"
    const val RecurringTransactionDetail = "recurring_transaction_detail/{recurringId}"
    const val RecurringTransactionAdd = "recurring_transaction_add"
    const val RecurringTransactionEdit = "recurring_transaction_edit/{recurringId}"

    // 高级功能 - 家庭共享记账
    const val FamilyMembers = "family_members"
    const val FamilyMemberAdd = "family_member_add"
    const val FamilyMemberEdit = "family_member_edit/{memberId}"

    // 高级功能 - 理财产品跟踪
    const val Investments = "investments"
    const val InvestmentDetail = "investment_detail/{investmentId}"
    const val InvestmentAdd = "investment_add"
    const val InvestmentEdit = "investment_edit/{investmentId}"

    // 高级功能 - 财务报告生成
    const val FinancialReports = "financial_reports"
    const val FinancialReportDetail = "financial_report_detail/{reportId}"
    const val GenerateReport = "generate_report"

    // 导航分析报告
    const val NavAnalyticsReport = "nav_analytics_report"

    // 导航性能报告
    const val NavPerformanceReport = "nav_performance_report"

    // 日志查看器
    const val LogViewer = "log_viewer"

    // 路由创建辅助方法
    fun accountDetail(accountId: Long) = "account_detail/$accountId"
    fun accountEdit(accountId: Long) = "account_edit/$accountId"
    fun editAccount(accountId: Long) = "add_edit_account?accountId=$accountId"
    fun transactionDetail(transactionId: Long) = "transaction_detail/$transactionId"
    fun transactionEdit(transactionId: Long) = "transaction_edit/$transactionId"
    fun addTransaction(transactionId: Long) = "add_transaction?transactionId=$transactionId"
    fun editTransaction(transactionId: Long) = "add_edit_transaction?transactionId=$transactionId"
    fun categoryDetail(categoryId: Long) = "category_detail/$categoryId"
    fun categoryEdit(categoryId: Long) = "category_edit/$categoryId"
    fun editCategory(categoryId: Long) = "add_edit_category?categoryId=$categoryId"
    fun statisticsDetail(type: String) = "statistics_detail/$type"
    fun creditCardDetail(creditCardId: Long) = "credit_card_detail/$creditCardId"
    fun creditCardEdit(creditCardId: Long) = "credit_card_edit/$creditCardId"
    fun creditCardPayment(creditCardId: Long) = "credit_card_payment/$creditCardId"
    fun budgetDetail(budgetId: Long) = "budget_detail/$budgetId"
    fun budgetEdit(budgetId: Long) = "add_edit_budget?budgetId=$budgetId"
    fun editBudget(budgetId: Long) = "add_edit_budget?budgetId=$budgetId"

    // 高级功能路由辅助方法
    fun savingGoalDetail(goalId: Long) = "saving_goal_detail/$goalId"
    fun savingGoalEdit(goalId: Long) = "saving_goal_edit/$goalId"
    fun recurringTransactionDetail(recurringId: Long) = "recurring_transaction_detail/$recurringId"
    fun recurringTransactionEdit(recurringId: Long) = "recurring_transaction_edit/$recurringId"
    fun familyMemberEdit(memberId: Long) = "family_member_edit/$memberId"
    fun investmentDetail(investmentId: Long) = "investment_detail/$investmentId"
    fun investmentEdit(investmentId: Long) = "investment_edit/$investmentId"
    fun financialReportDetail(reportId: Long) = "financial_report_detail/$reportId"
}