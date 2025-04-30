package com.ccjizhang.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ccjizhang.data.db.TransactionManager
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
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 优化的交易仓库
 * 使用 BaseRepository 和 TransactionManager 优化事务管理
 */
@Singleton
class OptimizedTransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountRepository: AccountRepository,
    transactionManager: TransactionManager,
    databaseExceptionHandler: DatabaseExceptionHandler
) : BaseRepository(transactionManager, databaseExceptionHandler) {

    companion object {
        private const val TAG = "OptimizedTransactionRepo"
    }

    /**
     * 获取所有交易
     * 使用优化的事务管理和异常处理
     */
    fun getAllTransactions(): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.getAllTransactions()
        }
    }

    /**
     * 分页获取交易数据
     * 使用 Paging 3 库实现高效分页加载
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
     * 获取收入交易
     */
    fun getIncomeTransactions(): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.getIncomeTransactions()
        }
    }

    /**
     * 获取支出交易
     */
    fun getExpenseTransactions(): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.getExpenseTransactions()
        }
    }

    /**
     * 按日期范围获取交易
     */
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.getTransactionsByDateRange(startDate, endDate)
        }
    }

    /**
     * 按ID获取交易
     */
    fun getTransactionById(id: Long): Flow<Transaction?> {
        return executeFlowOperation {
            transactionDao.getTransactionById(id)
        }
    }

    /**
     * 添加交易并更新账户余额
     * 使用优化的事务管理
     */
    suspend fun addTransactionWithBalanceUpdate(transaction: Transaction): Long {
        return executeInTransaction {
            // 添加交易
            val transactionId = transactionDao.insert(transaction)

            // 更新账户余额
            updateAccountBalance(transaction)

            transactionId
        }
    }

    /**
     * 更新交易并更新账户余额
     * 使用优化的事务管理
     */
    suspend fun updateTransactionWithBalanceUpdate(transaction: Transaction) {
        executeInTransaction {
            // 获取原交易
            val oldTransaction = transactionDao.getTransactionByIdSync(transaction.id)

            if (oldTransaction != null) {
                // 撤销原交易对账户余额的影响
                revertAccountBalance(oldTransaction)

                // 更新交易
                transactionDao.update(transaction)

                // 应用新交易对账户余额的影响
                updateAccountBalance(transaction)
            } else {
                // 如果原交易不存在，直接添加新交易
                transactionDao.insert(transaction)
                updateAccountBalance(transaction)
            }
        }
    }

    /**
     * 删除交易并更新账户余额
     * 使用优化的事务管理
     */
    suspend fun deleteTransactionByIdWithBalanceUpdate(id: Long) {
        executeInTransaction {
            // 获取要删除的交易
            val transaction = transactionDao.getTransactionByIdSync(id)

            if (transaction != null) {
                // 撤销交易对账户余额的影响
                revertAccountBalance(transaction)

                // 删除交易
                transactionDao.deleteById(id)
            }
        }
    }

    /**
     * 批量添加交易
     * 使用优化的批量事务管理
     */
    suspend fun batchAddTransactions(transactions: List<Transaction>): Int {
        return executeInTransaction {
            // 批量添加交易
            transactionDao.insertAll(transactions)

            // 批量更新账户余额
            val accountBalanceUpdates = mutableMapOf<Long, Double>()

            // 计算每个账户的余额变化
            transactions.forEach { transaction ->
                val amountChange = if (transaction.isIncome) transaction.amount else -transaction.amount
                accountBalanceUpdates[transaction.accountId] =
                    (accountBalanceUpdates[transaction.accountId] ?: 0.0) + amountChange.toDouble()

                // 如果是转账交易，还需要处理目标账户
                transaction.toAccountId?.let { toAccountId ->
                    accountBalanceUpdates[toAccountId] =
                        (accountBalanceUpdates[toAccountId] ?: 0.0) + transaction.amount.toDouble()
                }
            }

            // 批量更新账户余额
            for ((accountId, amountChange) in accountBalanceUpdates) {
                accountRepository.updateAccountBalance(accountId, amountChange)
            }

            transactions.size
        }
    }

    /**
     * 批量删除交易
     * 使用优化的批量事务管理
     */
    suspend fun batchDeleteTransactions(transactionIds: List<Long>): Int {
        return executeInTransaction {
            var deletedCount = 0

            // 获取所有要删除的交易
            val transactions = transactionIds.mapNotNull { id ->
                transactionDao.getTransactionByIdSync(id)
            }

            // 计算每个账户的余额变化
            val accountBalanceUpdates = mutableMapOf<Long, Double>()

            transactions.forEach { transaction ->
                // 计算余额变化（删除时需要反向调整）
                val amountChange = if (transaction.isIncome) -transaction.amount else transaction.amount
                accountBalanceUpdates[transaction.accountId] =
                    (accountBalanceUpdates[transaction.accountId] ?: 0.0) + amountChange.toDouble()

                // 如果是转账交易，还需要处理目标账户
                transaction.toAccountId?.let { toAccountId ->
                    accountBalanceUpdates[toAccountId] =
                        (accountBalanceUpdates[toAccountId] ?: 0.0) - transaction.amount.toDouble()
                }

                // 删除交易
                transactionDao.deleteById(transaction.id)
                deletedCount++
            }

            // 批量更新账户余额
            for ((accountId, amountChange) in accountBalanceUpdates) {
                accountRepository.updateAccountBalance(accountId, amountChange)
            }

            deletedCount
        }
    }

    /**
     * 获取交易统计信息
     */
    fun getSummaryByDateRange(startDate: Date, endDate: Date): Flow<TransactionSummary> {
        return executeFlowOperation {
            transactionDao.getSummaryByDateRange(startDate, endDate)
        }
    }

    /**
     * 获取按分类统计的交易
     */
    fun getCategoryStatsByDateRange(startDate: Date, endDate: Date, isIncome: Boolean): Flow<List<CategoryStat>> {
        return executeFlowOperation {
            transactionDao.getCategoryStatsByDateRange(startDate, endDate, isIncome)
        }
    }

    /**
     * 获取支出分类统计
     */
    fun getExpenseStatsByCategory(startDate: Date? = null, endDate: Date? = null): Flow<List<CategoryStat>> {
        return executeFlowOperation {
            transactionDao.getExpenseStatsByCategory(startDate, endDate)
        }
    }

    /**
     * 获取每日支出趋势
     */
    fun getDailyExpenseTrend(startDate: Date, endDate: Date): Flow<List<DailyStat>> {
        return executeFlowOperation {
            transactionDao.getDailyExpenseTrend(startDate, endDate)
        }
    }

    /**
     * 获取每周统计数据
     */
    fun getWeeklyStatsByDateRange(startDate: Date, endDate: Date): Flow<List<WeeklyStat>> {
        return executeFlowOperation {
            transactionDao.getWeeklyStatsByDateRange(startDate, endDate)
        }
    }

    /**
     * 获取每月统计数据
     */
    fun getMonthlyStatsByYear(year: String): Flow<List<MonthlyStat>> {
        return executeFlowOperation {
            transactionDao.getMonthlyStatsByYear(year)
        }
    }

    /**
     * 获取净额变化趋势
     */
    fun getNetAmountTrend(): Flow<List<NetAmountStat>> {
        return executeFlowOperation {
            transactionDao.getNetAmountTrend()
        }
    }

    /**
     * 获取最近的交易记录
     */
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.getRecentTransactions(limit)
        }
    }

    /**
     * 搜索交易
     */
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return executeFlowOperation {
            transactionDao.searchTransactions(query)
        }
    }

    /**
     * 更新账户余额
     * 根据交易类型和金额更新相关账户的余额
     */
    private suspend fun updateAccountBalance(transaction: Transaction) {
        if (transaction.isIncome) {
            // 收入交易，增加账户余额
            accountRepository.updateAccountBalance(transaction.accountId, transaction.amount.toDouble())
        } else {
            // 支出交易，减少账户余额
            accountRepository.updateAccountBalance(transaction.accountId, -transaction.amount.toDouble())
        }

        // 如果是转账交易，还需要处理目标账户
        transaction.toAccountId?.let { toAccountId ->
            // 转账交易，增加目标账户余额
            accountRepository.updateAccountBalance(toAccountId, transaction.amount.toDouble())
        }
    }

    /**
     * 撤销账户余额更新
     * 撤销交易对账户余额的影响
     */
    private suspend fun revertAccountBalance(transaction: Transaction) {
        if (transaction.isIncome) {
            // 撤销收入交易，减少账户余额
            accountRepository.updateAccountBalance(transaction.accountId, -transaction.amount.toDouble())
        } else {
            // 撤销支出交易，增加账户余额
            accountRepository.updateAccountBalance(transaction.accountId, transaction.amount.toDouble())
        }

        // 如果是转账交易，还需要处理目标账户
        transaction.toAccountId?.let { toAccountId ->
            // 撤销转账交易，减少目标账户余额
            accountRepository.updateAccountBalance(toAccountId, -transaction.amount.toDouble())
        }
    }
}
