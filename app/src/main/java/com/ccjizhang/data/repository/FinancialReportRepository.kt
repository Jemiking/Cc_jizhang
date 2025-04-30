package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.FinancialReportDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.model.FinancialReport
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 财务报告数据仓库
 * 负责生成和管理财务报告
 */
@Singleton
class FinancialReportRepository @Inject constructor(
    private val financialReportDao: FinancialReportDao,
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository
) {
    
    /**
     * 获取所有财务报告
     */
    fun getAllFinancialReports(): Flow<List<FinancialReport>> {
        return financialReportDao.getAllFinancialReports()
    }
    
    /**
     * 获取指定类型的财务报告
     */
    fun getFinancialReportsByType(type: Int): Flow<List<FinancialReport>> {
        return financialReportDao.getFinancialReportsByType(type)
    }
    
    /**
     * 获取时间范围内的财务报告
     */
    fun getFinancialReportsInPeriod(fromDate: Date, toDate: Date): Flow<List<FinancialReport>> {
        return financialReportDao.getFinancialReportsInPeriod(fromDate, toDate)
    }
    
    /**
     * 搜索财务报告
     */
    fun searchFinancialReports(query: String): Flow<List<FinancialReport>> {
        return financialReportDao.searchFinancialReports(query)
    }
    
    /**
     * 获取最近生成的N个财务报告
     */
    fun getRecentFinancialReports(limit: Int): Flow<List<FinancialReport>> {
        return financialReportDao.getRecentFinancialReports(limit)
    }
    
    /**
     * 获取指定年份的年度报告
     */
    fun getYearlyReports(year: String): Flow<List<FinancialReport>> {
        return financialReportDao.getYearlyReports(year)
    }
    
    /**
     * 获取指定年份的季度报告
     */
    fun getQuarterlyReports(year: String): Flow<List<FinancialReport>> {
        return financialReportDao.getQuarterlyReports(year)
    }
    
    /**
     * 获取指定年份和月份的月度报告
     */
    fun getMonthlyReports(year: String, month: String): Flow<List<FinancialReport>> {
        return financialReportDao.getMonthlyReports(year, month)
    }
    
    /**
     * 根据ID获取财务报告
     */
    suspend fun getFinancialReportById(id: Long): FinancialReport? {
        return financialReportDao.getById(id)
    }
    
    /**
     * 添加新的财务报告
     */
    suspend fun addFinancialReport(financialReport: FinancialReport): Long {
        return financialReportDao.insert(financialReport)
    }
    
    /**
     * 更新财务报告
     */
    suspend fun updateFinancialReport(financialReport: FinancialReport) {
        financialReportDao.update(financialReport)
    }
    
    /**
     * 删除财务报告
     */
    suspend fun deleteFinancialReport(financialReport: FinancialReport) {
        financialReportDao.delete(financialReport)
    }
    
    /**
     * 更新财务报告状态
     */
    suspend fun updateStatus(id: Long, status: Int) {
        financialReportDao.updateStatus(id, status)
    }
    
    /**
     * 更新财务报告PDF文件URI
     */
    suspend fun updatePdfUri(id: Long, pdfUri: String?) {
        financialReportDao.updatePdfUri(id, pdfUri)
    }
    
    /**
     * 更新财务报告分享链接
     */
    suspend fun updateShareUrl(id: Long, shareUrl: String?) {
        financialReportDao.updateShareUrl(id, shareUrl)
    }
    
    /**
     * 删除特定日期之前的报告
     */
    suspend fun deleteReportsOlderThan(date: Date) {
        financialReportDao.deleteReportsOlderThan(date)
    }
    
    /**
     * 获取总报告数量
     */
    fun getTotalReportCount(): Flow<Int> {
        return financialReportDao.getTotalReportCount()
    }
    
    /**
     * 生成月度财务报告
     */
    suspend fun generateMonthlyReport(year: Int, month: Int): Long {
        // 计算月份开始和结束日期
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        // 生成报告标题
        val dateFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        val title = "${dateFormat.format(startDate)}月度财报"
        
        return generateReport(title, 0, startDate, endDate)
    }
    
    /**
     * 生成季度财务报告
     */
    suspend fun generateQuarterlyReport(year: Int, quarter: Int): Long {
        // 计算季度开始和结束日期
        val startMonth = (quarter - 1) * 3 + 1
        val calendar = Calendar.getInstance()
        calendar.set(year, startMonth - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.MONTH, 3)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        // 生成报告标题
        val title = "${year}年第${quarter}季度财报"
        
        return generateReport(title, 1, startDate, endDate)
    }
    
    /**
     * 生成年度财务报告
     */
    suspend fun generateYearlyReport(year: Int): Long {
        // 计算年份开始和结束日期
        val calendar = Calendar.getInstance()
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        // 生成报告标题
        val title = "${year}年度财报"
        
        return generateReport(title, 2, startDate, endDate)
    }
    
    /**
     * 生成自定义时间范围的财务报告
     */
    suspend fun generateCustomReport(title: String, startDate: Date, endDate: Date): Long {
        return generateReport(title, 3, startDate, endDate)
    }
    
    /**
     * 通用报告生成方法
     */
    private suspend fun generateReport(title: String, type: Int, startDate: Date, endDate: Date): Long {
        // 获取期间内的收入和支出
        val transactions = transactionDao.getTransactionsByDateRangeSync(startDate, endDate)
        val totalIncome = transactions.filter { transaction -> transaction.isIncome }.sumOf { transaction -> transaction.amount }
        val totalExpense = transactions.filter { transaction -> !transaction.isIncome }.sumOf { transaction -> transaction.amount }
        val netCashflow = totalIncome - totalExpense
        
        // 计算储蓄率
        val savingRate = if (totalIncome > 0) (totalIncome - totalExpense) / totalIncome else 0.0
        
        // 获取期初和期末资产
        val initialTotalAssets = calculateTotalAssetsAtDate(startDate)
        val finalTotalAssets = calculateTotalAssetsAtDate(endDate)
        
        // 计算资产增长率
        val assetGrowthRate = if (initialTotalAssets > 0) (finalTotalAssets - initialTotalAssets) / initialTotalAssets else 0.0
        
        // 构建报告数据JSON
        val reportData = JSONObject()
        reportData.put("transactionCount", transactions.size)
        reportData.put("incomeByCategory", calculateIncomeByCategory(transactions))
        reportData.put("expenseByCategory", calculateExpenseByCategory(transactions))
        reportData.put("dailyCashflow", calculateDailyCashflow(transactions))
        reportData.put("topExpenseCategories", calculateTopExpenseCategories(transactions, 5))
        reportData.put("topIncomeCategories", calculateTopIncomeCategories(transactions, 5))
        
        // 创建财务报告对象
        val report = FinancialReport(
            title = title,
            type = type,
            startDate = startDate,
            endDate = endDate,
            generatedDate = Date(),
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashflow = netCashflow,
            savingRate = savingRate,
            initialTotalAssets = initialTotalAssets,
            finalTotalAssets = finalTotalAssets,
            assetGrowthRate = assetGrowthRate,
            reportDataJson = reportData.toString(),
            status = 1, // 已完成
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // 保存到数据库
        return financialReportDao.insert(report)
    }
    
    /**
     * 计算按分类统计的收入数据
     */
    private fun calculateIncomeByCategory(transactions: List<com.ccjizhang.data.model.Transaction>): JSONObject {
        val result = JSONObject()
        val incomeTransactions = transactions.filter { transaction -> transaction.isIncome }
        
        // 按分类ID分组
        val groupedByCategory = incomeTransactions.groupBy { transaction -> transaction.categoryId }
        
        // 计算每个分类的总金额
        groupedByCategory.forEach { (categoryId, transactions) ->
            val categorySum = transactions.sumOf { transaction -> transaction.amount }
            result.put(categoryId.toString(), categorySum)
        }
        
        return result
    }
    
    /**
     * 计算按分类统计的支出数据
     */
    private fun calculateExpenseByCategory(transactions: List<com.ccjizhang.data.model.Transaction>): JSONObject {
        val result = JSONObject()
        val expenseTransactions = transactions.filter { transaction -> !transaction.isIncome }
        
        // 按分类ID分组
        val groupedByCategory = expenseTransactions.groupBy { transaction -> transaction.categoryId }
        
        // 计算每个分类的总金额
        groupedByCategory.forEach { (categoryId, transactions) ->
            val categorySum = transactions.sumOf { transaction -> transaction.amount }
            result.put(categoryId.toString(), categorySum)
        }
        
        return result
    }
    
    /**
     * 计算每日收支流水
     */
    private fun calculateDailyCashflow(transactions: List<com.ccjizhang.data.model.Transaction>): JSONObject {
        val result = JSONObject()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 按日期分组
        val groupedByDate = transactions.groupBy { transaction -> dateFormat.format(transaction.date) }
        
        // 计算每天的收入和支出
        groupedByDate.forEach { (date, dailyTransactions) ->
            val dailyData = JSONObject()
            val income = dailyTransactions.filter { transaction -> transaction.isIncome }.sumOf { transaction -> transaction.amount }
            val expense = dailyTransactions.filter { transaction -> !transaction.isIncome }.sumOf { transaction -> transaction.amount }
            dailyData.put("income", income)
            dailyData.put("expense", expense)
            dailyData.put("net", income - expense)
            result.put(date, dailyData)
        }
        
        return result
    }
    
    /**
     * 计算支出最多的分类
     */
    private fun calculateTopExpenseCategories(transactions: List<com.ccjizhang.data.model.Transaction>, limit: Int): JSONObject {
        val result = JSONObject()
        val expenseTransactions = transactions.filter { transaction -> !transaction.isIncome }
        
        // 按分类ID分组
        val groupedByCategory = expenseTransactions.groupBy { transaction -> transaction.categoryId }
        
        // 计算每个分类的总金额并排序
        val sortedCategories = groupedByCategory
            .map { (categoryId, transactions) ->
                val categorySum = transactions.sumOf { transaction -> transaction.amount }
                Pair(categoryId.toString(), categorySum)
            }
            .sortedByDescending { it.second }
            .take(limit)
        
        // 添加到结果
        sortedCategories.forEach { (categoryId, sum) ->
            result.put(categoryId, sum)
        }
        
        return result
    }
    
    /**
     * 计算收入最多的分类
     */
    private fun calculateTopIncomeCategories(transactions: List<com.ccjizhang.data.model.Transaction>, limit: Int): JSONObject {
        val result = JSONObject()
        val incomeTransactions = transactions.filter { transaction -> transaction.isIncome }
        
        // 按分类ID分组
        val groupedByCategory = incomeTransactions.groupBy { transaction -> transaction.categoryId }
        
        // 计算每个分类的总金额并排序
        val sortedCategories = groupedByCategory
            .map { (categoryId, transactions) ->
                val categorySum = transactions.sumOf { transaction -> transaction.amount }
                Pair(categoryId.toString(), categorySum)
            }
            .sortedByDescending { it.second }
            .take(limit)
        
        // 添加到结果
        sortedCategories.forEach { (categoryId, sum) ->
            result.put(categoryId, sum)
        }
        
        return result
    }
    
    /**
     * 计算指定日期的总资产
     */
    private suspend fun calculateTotalAssetsAtDate(date: Date): Double {
        // 获取当前所有账户
        val currentAccounts = accountRepository.getAllAccountsSync()
        
        // 获取截止到指定日期的所有交易
        val pastTransactions = transactionDao.getTransactionsByDateRangeSync(
            Date(0), // 从最早时间
            date     // 到指定日期
        )
        
        // 计算每个账户在指定日期的余额
        var totalAssets = 0.0
        for (account in currentAccounts) {
            // 计算账户创建后的所有交易对该账户余额的影响
            val accountTransactions = pastTransactions.filter { transaction -> 
                transaction.accountId == account.id || transaction.toAccountId == account.id 
            }
            
            var accountBalance = 0.0
            for (transaction in accountTransactions) {
                when {
                    // 收入交易且目标是这个账户
                    transaction.isIncome && transaction.accountId == account.id -> 
                        accountBalance += transaction.amount
                    // 支出交易且源是这个账户
                    !transaction.isIncome && transaction.accountId == account.id -> 
                        accountBalance -= transaction.amount
                    // 转账交易，且这个账户是转出账户
                    transaction.toAccountId != null && transaction.accountId == account.id -> 
                        accountBalance -= transaction.amount
                    // 转账交易，且这个账户是转入账户
                    transaction.toAccountId != null && transaction.toAccountId == account.id -> 
                        accountBalance += transaction.amount
                }
            }
            
            // 将账户余额按汇率换算成统一货币，然后累加到总资产
            totalAssets += accountBalance * account.exchangeRate
        }
        
        return totalAssets
    }
    
    /**
     * 更新财务报告的详细信息
     */
    suspend fun updateReport(
        id: Long,
        description: String? = null,
        includeIncomeAnalysis: Boolean? = null,
        includeExpenseAnalysis: Boolean? = null,
        includeCategoryBreakdown: Boolean? = null,
        includeAccountBalances: Boolean? = null,
        includeBudgetComparison: Boolean? = null,
        includeFinancialHealth: Boolean? = null,
        type: Int? = null
    ) {
        // 首先获取现有报告
        val report = financialReportDao.getById(id) ?: return
        
        // 创建更新后的报告对象
        val updatedReport = report.copy(
            note = description ?: report.note,
            includeIncomeAnalysis = includeIncomeAnalysis ?: report.includeIncomeAnalysis,
            includeExpenseAnalysis = includeExpenseAnalysis ?: report.includeExpenseAnalysis,
            includeCategoryBreakdown = includeCategoryBreakdown ?: report.includeCategoryBreakdown,
            includeAccountBalances = includeAccountBalances ?: report.includeAccountBalances,
            includeBudgetComparison = includeBudgetComparison ?: report.includeBudgetComparison,
            includeFinancialHealth = includeFinancialHealth ?: report.includeFinancialHealth,
            type = type ?: report.type,
            updatedAt = Date()
        )
        
        // 更新报告
        financialReportDao.update(updatedReport)
    }
} 