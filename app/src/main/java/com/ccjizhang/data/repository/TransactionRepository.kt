package com.ccjizhang.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.Transaction as RoomTransaction
import com.ccjizhang.data.db.dao.CategoryStat
import com.ccjizhang.data.db.dao.DailyStat
import com.ccjizhang.data.db.dao.MonthlyStat
import com.ccjizhang.data.db.dao.NetAmountStat
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.db.dao.TransactionSummary
import com.ccjizhang.data.db.dao.WeeklyStat
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionType
import com.ccjizhang.data.paging.TransactionFilterType
import com.ccjizhang.data.paging.TransactionPagingSource
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易数据仓库
 * 负责提供交易数据和处理业务逻辑
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "TransactionRepository"
    }
    // 获取所有交易
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
        .catch { e ->
            Log.e(TAG, "获取交易列表失败", e)
            if (e is Exception) {
                databaseExceptionHandler.handleDatabaseException(e, "获取交易列表")
            }
            emit(emptyList())
        }

    /**
     * 分页获取交易数据
     * 使用 Paging 3 库实现高效分页加载
     * @param pageSize 每页数量
     * @param filterType 筛选类型（全部、收入、支出）
     * @return 分页数据流
     */
    fun getTransactionsPaged(
        pageSize: Int = 20,
        filterType: TransactionFilterType = TransactionFilterType.ALL
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactionDao = transactionDao,
                    filterType = filterType
                )
            }
        ).flow
    }

    /**
     * 按日期范围分页获取交易
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByDateRangePaged(
        startDate: Date,
        endDate: Date,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactionDao = transactionDao,
                    startDate = startDate,
                    endDate = endDate
                )
            }
        ).flow
    }

    /**
     * 按分类分页获取交易
     * @param categoryId 分类ID
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByCategoryPaged(
        categoryId: Long,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactionDao = transactionDao,
                    categoryId = categoryId
                )
            }
        ).flow
    }

    /**
     * 按账户分页获取交易
     * @param accountId 账户ID
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByAccountPaged(
        accountId: Long,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactionDao = transactionDao,
                    accountId = accountId
                )
            }
        ).flow
    }

    /**
     * 搜索交易并分页返回结果
     * @param query 搜索关键词
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun searchTransactionsPaged(
        query: String,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionPagingSource(
                    transactionDao = transactionDao,
                    searchQuery = query
                )
            }
        ).flow
    }

    // 按ID获取交易
    fun getTransactionById(id: Long): Flow<Transaction?> = transactionDao.getTransactionById(id)

    // 获取收入交易
    fun getIncomeTransactions(): Flow<List<Transaction>> = transactionDao.getIncomeTransactions()

    // 获取支出交易
    fun getExpenseTransactions(): Flow<List<Transaction>> = transactionDao.getExpenseTransactions()

    // 按日期范围获取交易
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    // 按日期范围获取交易（同步版本）
    suspend fun getTransactionsByDateRangeSync(startDate: Date, endDate: Date): List<Transaction> =
        transactionDao.getTransactionsByDateRangeSync(startDate, endDate)

    // 获取所有交易（同步版本）
    suspend fun getAllTransactionsSync(): List<Transaction> =
        transactionDao.getAllTransactionsSync()

    // 按LocalDate日期范围获取交易
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getTransactionsByDateRange(startDateConverted, endDateConverted)
    }

    // 按LocalDate日期范围获取交易（同步版本）
    suspend fun getTransactionsByDateRangeSync(startDate: LocalDate, endDate: LocalDate): List<Transaction> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getTransactionsByDateRangeSync(startDateConverted, endDateConverted)
    }

    // 按YearMonth获取特定月份的交易（同步版本）
    suspend fun getTransactionsByMonthSync(yearMonth: YearMonth): List<Transaction> {
        val year = yearMonth.year.toString()
        val month = String.format("%02d", yearMonth.monthValue)
        return transactionDao.getTransactionsByYearMonthSync(year, month)
    }

    // 按账户获取交易
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccount(accountId)

    // 按分类获取交易
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)

    /**
     * 获取最近的交易记录
     * @param limit 限制返回的记录数
     * @return 最近的交易记录列表流
     */
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)

    // 获取总支出
    fun getTotalExpense(): Flow<Double?> = transactionDao.getTotalExpense()

    // 获取总收入
    fun getTotalIncome(): Flow<Double?> = transactionDao.getTotalIncome()

    // 获取特定分类和日期范围的金额总和
    fun getAmountByCategoryAndDateRange(categoryId: Long, startDate: Date, endDate: Date): Flow<Double?> =
        transactionDao.getAmountByCategoryAndDateRange(categoryId, startDate, endDate)

    /**
     * 简单关键字搜索交易记录
     * @param query 搜索关键字
     * @return 符合条件的交易列表流
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        // 尝试将查询转换为数值，以支持金额搜索
        val amountQuery = query.toDoubleOrNull() ?: -1.0
        return transactionDao.searchTransactions(query, amountQuery)
    }

    /**
     * 高级搜索交易记录
     * 支持多条件组合查询
     */
    fun advancedSearchTransactions(
        query: String = "",
        startDate: Date? = null,
        endDate: Date? = null,
        isIncome: Boolean? = null,
        accountId: Long = 0L,
        categoryId: Long = 0L
    ): Flow<List<Transaction>> {
        return transactionDao.advancedSearchTransactions(
            query, startDate, endDate, isIncome, accountId, categoryId
        )
    }

    /**
     * 添加交易并更新账户余额
     * 使用事务确保数据一致性
     * 增强版：增加重试机制和更强的错误处理
     */
    @RoomTransaction
    suspend fun addTransactionWithBalanceUpdate(transaction: Transaction): Long {
        // 最大重试次数
        val maxRetries = 3
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                // 插入交易记录
                val transactionId = transactionDao.insert(transaction)

                // 更新账户余额
                val amountChange = if (transaction.isIncome) transaction.amount else -transaction.amount
                accountRepository.updateAccountBalance(transaction.accountId, amountChange)

                // 成功添加，返回交易ID
                return transactionId
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "添加交易失败，尝试次数: ${retryCount + 1}/$maxRetries", e)

                // 尝试处理数据库异常
                val handled = databaseExceptionHandler.handleDatabaseException(e, "添加交易")

                if (handled) {
                    // 如果异常已处理（数据库已修复），则重试
                    retryCount++
                    // 等待一小段时间再重试
                    kotlinx.coroutines.delay(500L * retryCount)
                } else {
                    // 如果不是可处理的数据库异常，直接抛出
                    throw e
                }
            }
        }

        // 如果所有重试都失败，记录错误并返回-1
        Log.e(TAG, "添加交易失败，已达到最大重试次数", lastException)
        return -1L
    }

    /**
     * 添加转账交易并更新两个账户余额
     * 使用事务确保数据一致性
     */
    @RoomTransaction
    suspend fun addTransferTransaction(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        date: Date,
        note: String = ""
    ): Pair<Long, Long> {
        // 创建支出交易（从账户）
        val outTransaction = Transaction(
            amount = amount,
            categoryId = null,
            accountId = fromAccountId,
            date = date,
            note = "转账至其他账户: $note",
            isIncome = false,
            toAccountId = toAccountId
        )

        // 创建收入交易（至账户）
        val inTransaction = Transaction(
            amount = amount,
            categoryId = null,
            accountId = toAccountId,
            date = date,
            note = "从其他账户转入: $note",
            isIncome = true,
            toAccountId = null
        )

        // 插入交易并更新账户余额
        val outId = transactionDao.insert(outTransaction)
        accountRepository.updateAccountBalance(fromAccountId, -amount)

        val inId = transactionDao.insert(inTransaction)
        accountRepository.updateAccountBalance(toAccountId, amount)

        return Pair(outId, inId)
    }

    /**
     * 更新交易并调整账户余额
     * 使用事务确保数据一致性
     */
    @RoomTransaction
    suspend fun updateTransactionWithBalanceUpdate(
        oldTransaction: Transaction,
        newTransaction: Transaction
    ) {
        // 如果账户ID未变化
        if (oldTransaction.accountId == newTransaction.accountId) {
            // 计算差额
            val oldAmount = if (oldTransaction.isIncome) oldTransaction.amount else -oldTransaction.amount
            val newAmount = if (newTransaction.isIncome) newTransaction.amount else -newTransaction.amount
            val amountDifference = newAmount - oldAmount

            // 更新交易
            transactionDao.update(newTransaction)

            // 更新账户余额
            accountRepository.updateAccountBalance(newTransaction.accountId, amountDifference)
        } else {
            // 账户ID变化，需要更新两个账户
            // 回滚旧账户余额
            val oldAmountChange = if (oldTransaction.isIncome) -oldTransaction.amount else oldTransaction.amount
            accountRepository.updateAccountBalance(oldTransaction.accountId, oldAmountChange)

            // 更新新账户余额
            val newAmountChange = if (newTransaction.isIncome) newTransaction.amount else -newTransaction.amount
            accountRepository.updateAccountBalance(newTransaction.accountId, newAmountChange)

            // 更新交易
            transactionDao.update(newTransaction)
        }
    }

    /**
     * 删除交易并回滚账户余额
     * 使用事务确保数据一致性
     */
    @RoomTransaction
    suspend fun deleteTransactionWithBalanceUpdate(transaction: Transaction) {
        // 回滚账户余额
        val amountChange = if (transaction.isIncome) -transaction.amount else transaction.amount
        accountRepository.updateAccountBalance(transaction.accountId, amountChange)

        // 删除交易
        transactionDao.delete(transaction)
    }

    /**
     * 按ID删除交易并回滚账户余额
     * 使用事务确保数据一致性
     */
    @RoomTransaction
    suspend fun deleteTransactionByIdWithBalanceUpdate(id: Long) {
        try {
            // 获取交易信息
            val transaction = transactionDao.getTransactionByIdSync(id) ?: return

            // 回滚账户余额
            val amountChange = if (transaction.isIncome) -transaction.amount else transaction.amount
            accountRepository.updateAccountBalance(transaction.accountId, amountChange)

            // 删除交易
            transactionDao.deleteById(id)
        } catch (e: Exception) {
            Log.e(TAG, "删除交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "删除交易")
            throw e
        }
    }

    /**
     * 验证并修复账户余额与交易记录的一致性
     * 返回是否进行了修复操作
     */
    @RoomTransaction
    suspend fun verifyAndFixAccountBalances(): Boolean {
        try {
            var hasFixed = false

            // 获取所有账户
            val accounts = accountRepository.getAllAccountsSync()

            for (account in accounts) {
                // 计算账户中所有交易的净额
                val income = transactionDao.getTotalIncomeByAccountSync(account.id) ?: 0.0
                val expense = transactionDao.getTotalExpenseByAccountSync(account.id) ?: 0.0
                val calculatedBalance = income - expense

                // 检查是否与账户余额一致
                if (Math.abs(calculatedBalance - account.balance) > 0.01) { // 使用0.01作为浮点数比较的容差
                    // 更新账户余额
                    accountRepository.updateAccountTo(account.id, calculatedBalance)
                    hasFixed = true
                }
            }

            return hasFixed
        } catch (e: Exception) {
            Log.e(TAG, "验证账户余额失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "验证账户余额")
            return false
        }
    }

    /**
     * 修复引用了无效分类的交易
     * 将这些交易的分类设置为默认分类或null
     * @return 修复的交易数量
     */
    @RoomTransaction
    suspend fun fixTransactionsWithInvalidCategory(): Int {
        try {
            // 获取所有引用了无效分类的交易
            val orphanedTransactions = transactionDao.getTransactionsWithInvalidCategory()
            if (orphanedTransactions.isEmpty()) {
                return 0
            }

            // 修复这些交易，将分类设置为null
            orphanedTransactions.forEach { transaction ->
                transactionDao.update(transaction.copy(categoryId = null))
            }

            return orphanedTransactions.size
        } catch (e: Exception) {
            Log.e(TAG, "修复无效分类交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "修复无效分类交易")
            return 0
        }
    }

    /**
     * 修复引用了无效账户的交易
     * 将这些交易的账户设置为默认账户或删除交易
     * @return 修复的交易数量
     */
    @RoomTransaction
    suspend fun fixTransactionsWithInvalidAccount(): Int {
        try {
            // 获取所有引用了无效账户的交易
            val orphanedTransactions = transactionDao.getTransactionsWithInvalidAccount()
            if (orphanedTransactions.isEmpty()) {
                return 0
            }

            // 获取默认账户
            val defaultAccount = accountRepository.getDefaultAccountSync()

            if (defaultAccount != null) {
                // 如果有默认账户，将交易移动到默认账户
                orphanedTransactions.forEach { transaction ->
                    transactionDao.update(transaction.copy(accountId = defaultAccount.id))
                }
            } else {
                // 如果没有默认账户，删除这些交易
                orphanedTransactions.forEach { transaction ->
                    transactionDao.delete(transaction)
                }
            }

            return orphanedTransactions.size
        } catch (e: Exception) {
            Log.e(TAG, "修复无效账户交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "修复无效账户交易")
            return 0
        }
    }

    // 统计分析相关方法

    /**
     * 获取按分类的统计信息
     */
    fun getCategoryStatsByDateRange(startDate: LocalDate, endDate: LocalDate, isIncome: Boolean): Flow<List<CategoryStat>> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getCategoryStatsByDateRange(startDateConverted, endDateConverted, isIncome)
    }

    /**
     * 获取按年份的月度统计
     */
    fun getMonthlyStatsByYear(year: String): Flow<List<MonthlyStat>> {
        return transactionDao.getMonthlyStatsByYear(year)
    }

    /**
     * 获取净资产变化趋势
     */
    fun getNetAmountTrend(): Flow<List<NetAmountStat>> {
        return transactionDao.getNetAmountTrend()
    }

    /**
     * 获取日期范围内的交易汇总
     */
    fun getSummaryByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<TransactionSummary> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getSummaryByDateRange(startDateConverted, endDateConverted)
    }

    /**
     * 获取日期范围内的每日支出趋势
     */
    fun getDailyExpenseTrend(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyStat>> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getDailyExpenseTrend(startDateConverted, endDateConverted)
    }

    /**
     * 获取日期范围内的每周统计数据
     */
    fun getWeeklyStatsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<WeeklyStat>> {
        val startDateConverted = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val endDateConverted = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusSeconds(1).toInstant())
        return transactionDao.getWeeklyStatsByDateRange(startDateConverted, endDateConverted)
    }

    /**
     * 统计分析：按分类获取支出统计
     */
    fun getExpenseStatsByCategory(startDate: Date? = null, endDate: Date? = null): Flow<List<CategoryStat>> {
        return transactionDao.getExpenseStatsByCategory(startDate, endDate)
    }

    /**
     * 获取特定标签的所有交易ID
     */
    fun getTransactionsByTag(tag: String): Flow<List<Long>> {
        return transactionDao.getTransactionIdsByTag(tag)
    }

    /**
     * 获取所有交易的类型分布统计（收入、支出）
     */
    fun getTransactionTypeStats(startDate: Date? = null, endDate: Date? = null): Flow<TransactionSummary> {
        return transactionDao.getTransactionTypeStats(startDate, endDate)
    }

    // 兼容旧接口，但不再推荐使用
    @Deprecated("使用addTransactionWithBalanceUpdate替代以确保账户余额同步更新", ReplaceWith("addTransactionWithBalanceUpdate(transaction)"))
    suspend fun addTransaction(transaction: Transaction): Long {
        return addTransactionWithBalanceUpdate(transaction)
    }

    // 兼容旧接口，但不再推荐使用
    @Deprecated("使用updateTransactionWithBalanceUpdate替代以确保账户余额同步更新", ReplaceWith("updateTransactionWithBalanceUpdate(transaction)"))
    suspend fun updateTransaction(transaction: Transaction) {
        val oldTransaction = transactionDao.getTransactionByIdSync(transaction.id) ?: return
        updateTransactionWithBalanceUpdate(oldTransaction, transaction)
    }

    // 兼容旧接口，但不再推荐使用
    @Deprecated("使用deleteTransactionWithBalanceUpdate替代以确保账户余额同步更新", ReplaceWith("deleteTransactionWithBalanceUpdate(transaction)"))
    suspend fun deleteTransaction(transaction: Transaction) {
        deleteTransactionWithBalanceUpdate(transaction)
    }

    // 兼容旧接口，但不再推荐使用
    @Deprecated("使用deleteTransactionByIdWithBalanceUpdate替代以确保账户余额同步更新", ReplaceWith("deleteTransactionByIdWithBalanceUpdate(id)"))
    suspend fun deleteTransactionById(id: Long) {
        deleteTransactionByIdWithBalanceUpdate(id)
    }
}