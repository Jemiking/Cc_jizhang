package com.ccjizhang.data.repository

import android.util.Log
import androidx.room.Transaction as RoomTransaction
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.db.dao.TransactionTagDao
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction as TransactionModel
import com.ccjizhang.data.model.TransactionTag
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 批量操作仓库
 * 专门处理批量数据操作，优化事务粒度和性能
 */
@Singleton
class BatchOperationRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val transactionTagDao: TransactionTagDao,
    private val accountRepository: AccountRepository,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "BatchOperationRepo"
        private const val BATCH_SIZE = 100 // 批处理大小
    }

    /**
     * 批量添加交易记录
     * 使用单一事务和批量插入优化性能
     * @param transactions 要添加的交易列表
     * @return 成功添加的交易数量
     */
    @RoomTransaction
    suspend fun batchAddTransactions(transactions: List<TransactionModel>): Int = withContext(Dispatchers.IO) {
        try {
            // 使用批量插入
            transactionDao.insertAll(transactions)

            // 更新所有相关账户的余额
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

            return@withContext transactions.size
        } catch (e: Exception) {
            Log.e(TAG, "批量添加交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "批量添加交易")
            return@withContext 0
        }
    }

    /**
     * 批量删除交易记录
     * 使用单一事务和批量删除优化性能
     * @param transactionIds 要删除的交易ID列表
     * @return 成功删除的交易数量
     */
    @RoomTransaction
    suspend fun batchDeleteTransactions(transactionIds: List<Long>): Int = withContext(Dispatchers.IO) {
        try {
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

                // 删除相关标签
                transactionTagDao.deleteByTransactionId(transaction.id)
            }

            // 批量更新账户余额
            for ((accountId, amountChange) in accountBalanceUpdates) {
                accountRepository.updateAccountBalance(accountId, amountChange)
            }

            return@withContext deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "批量删除交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "批量删除交易")
            return@withContext 0
        }
    }

    /**
     * 批量添加标签到多个交易
     * @param transactionIds 交易ID列表
     * @param tags 要添加的标签列表
     * @return 成功添加的标签数量
     */
    @RoomTransaction
    suspend fun batchAddTagsToTransactions(transactionIds: List<Long>, tags: List<String>): Int =
        withContext(Dispatchers.IO) {
            try {
                var addedCount = 0

                // 创建所有交易标签关系
                val transactionTags = mutableListOf<TransactionTag>()

                for (transactionId in transactionIds) {
                    for (tag in tags) {
                        transactionTags.add(TransactionTag(transactionId, tag))
                    }
                }

                // 批量插入标签
                transactionTagDao.insertAll(transactionTags)
                addedCount = transactionTags.size

                return@withContext addedCount
            } catch (e: Exception) {
                Log.e(TAG, "批量添加标签失败", e)
                return@withContext 0
            }
        }

    /**
     * 批量导入数据（分类、账户、交易）
     * 使用分批处理大量数据，避免事务过大
     * @param categories 分类列表
     * @param accounts 账户列表
     * @param transactions 交易列表
     * @return 导入结果统计
     */
    @RoomTransaction
    suspend fun batchImportData(
        categories: List<Category>? = null,
        accounts: List<Account>? = null,
        transactions: List<TransactionModel>? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val result = ImportResult()

        try {
            // 导入分类
            categories?.let {
                if (it.isNotEmpty()) {
                    // 分批处理
                    it.chunked(BATCH_SIZE).forEach { batch ->
                        categoryDao.insertAll(batch)
                        result.categoriesImported += batch.size
                    }
                }
            }

            // 导入账户
            accounts?.let {
                if (it.isNotEmpty()) {
                    // 分批处理
                    it.chunked(BATCH_SIZE).forEach { batch ->
                        accountDao.insertAll(batch)
                        result.accountsImported += batch.size
                    }
                }
            }

            // 导入交易
            transactions?.let {
                if (it.isNotEmpty()) {
                    // 分批处理
                    it.chunked(BATCH_SIZE).forEach { batch ->
                        transactionDao.insertAll(batch)
                        result.transactionsImported += batch.size
                    }

                    // 更新账户余额
                    updateAccountBalancesAfterImport()
                }
            }

            result.success = true
        } catch (e: Exception) {
            Log.e(TAG, "批量导入数据失败", e)
            result.errorMessage = e.message ?: "未知错误"
            databaseExceptionHandler.handleDatabaseException(e, "批量导入数据")
        }

        return@withContext result
    }

    /**
     * 批量清理数据
     * @param clearTransactions 是否清除交易记录
     * @param clearCategories 是否清除自定义分类
     * @param clearAccounts 是否清除账户
     * @param beforeDate 清除此日期之前的交易记录（如果为null则清除所有）
     * @return 清理结果统计
     */
    @RoomTransaction
    suspend fun batchCleanupData(
        clearTransactions: Boolean,
        clearCategories: Boolean,
        clearAccounts: Boolean,
        beforeDate: Date? = null
    ): CleanupResult = withContext(Dispatchers.IO) {
        val result = CleanupResult()

        try {
            // 清理交易记录
            if (clearTransactions) {
                if (beforeDate != null) {
                    result.transactionsDeleted = transactionDao.deleteTransactionsBeforeDate(beforeDate)
                } else {
                    result.transactionsDeleted = transactionDao.deleteAllTransactions()
                }
            }

            // 清理自定义分类
            if (clearCategories) {
                result.categoriesDeleted = categoryDao.deleteCustomCategories()
            }

            // 清理账户
            if (clearAccounts) {
                result.accountsDeleted = accountDao.deleteAllAccounts()
            }

            // 如果删除了交易，更新账户余额
            if (clearTransactions) {
                updateAccountBalancesAfterCleanup()
            }

            result.success = true
        } catch (e: Exception) {
            Log.e(TAG, "批量清理数据失败", e)
            result.errorMessage = e.message ?: "未知错误"
            databaseExceptionHandler.handleDatabaseException(e, "批量清理数据")
        }

        return@withContext result
    }

    /**
     * 导入后更新所有账户余额
     * 通过重新计算所有交易来更新账户余额
     */
    private suspend fun updateAccountBalancesAfterImport() {
        try {
            // 获取所有账户
            val accounts = accountDao.getAllAccountsSync()

            // 更新每个账户的余额
            for (account in accounts) {
                // 计算收入总额
                val totalIncome = transactionDao.getTotalIncomeByAccountSync(account.id) ?: 0.0

                // 计算支出总额
                val totalExpense = transactionDao.getTotalExpenseByAccountSync(account.id) ?: 0.0

                // 计算新余额
                val newBalance = totalIncome - totalExpense

                // 更新账户余额
                accountDao.update(account.copy(balance = newBalance))
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新账户余额失败", e)
        }
    }

    /**
     * 清理后更新所有账户余额
     */
    private suspend fun updateAccountBalancesAfterCleanup() {
        try {
            // 获取所有账户
            val accounts = accountDao.getAllAccountsSync()

            // 更新每个账户的余额
            for (account in accounts) {
                // 计算收入总额
                val totalIncome = transactionDao.getTotalIncomeByAccountSync(account.id) ?: 0.0

                // 计算支出总额
                val totalExpense = transactionDao.getTotalExpenseByAccountSync(account.id) ?: 0.0

                // 计算新余额
                val newBalance = totalIncome - totalExpense

                // 更新账户余额
                accountDao.update(account.copy(balance = newBalance))
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新账户余额失败", e)
        }
    }

    /**
     * 导入结果数据类
     */
    data class ImportResult(
        var categoriesImported: Int = 0,
        var accountsImported: Int = 0,
        var transactionsImported: Int = 0,
        var success: Boolean = false,
        var errorMessage: String = ""
    )

    /**
     * 清理结果数据类
     */
    data class CleanupResult(
        var transactionsDeleted: Int = 0,
        var categoriesDeleted: Int = 0,
        var accountsDeleted: Int = 0,
        var success: Boolean = false,
        var errorMessage: String = ""
    )
}
