package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.RecurringTransactionDao
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 定期交易数据仓库
 * 负责定期交易的管理与自动创建
 */
@Singleton
class RecurringTransactionRepository @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao,
    private val transactionRepository: TransactionRepository
) {
    
    /**
     * 获取所有定期交易
     */
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions()
    }
    
    /**
     * 获取活跃的定期交易
     */
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getActiveRecurringTransactions()
    }
    
    /**
     * 获取已暂停的定期交易
     */
    fun getPausedRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getPausedRecurringTransactions()
    }
    
    /**
     * 获取已完成的定期交易
     */
    fun getCompletedRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getCompletedRecurringTransactions()
    }
    
    /**
     * 获取特定账户的定期交易
     */
    fun getRecurringTransactionsByAccountId(accountId: Long): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getRecurringTransactionsByAccountId(accountId)
    }
    
    /**
     * 获取特定分类的定期交易
     */
    fun getRecurringTransactionsByCategoryId(categoryId: Long): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getRecurringTransactionsByCategoryId(categoryId)
    }
    
    /**
     * 获取未来7天内需要执行的定期交易
     */
    fun getUpcomingTransactionsInNext7Days(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getUpcomingTransactionsInNext7Days()
    }
    
    /**
     * 根据ID获取定期交易
     */
    suspend fun getRecurringTransactionById(id: Long): RecurringTransaction? {
        return recurringTransactionDao.getById(id)
    }
    
    /**
     * 添加新的定期交易
     */
    suspend fun addRecurringTransaction(recurringTransaction: RecurringTransaction): Long {
        return recurringTransactionDao.insert(recurringTransaction)
    }
    
    /**
     * 更新定期交易
     */
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.update(recurringTransaction)
    }
    
    /**
     * 删除定期交易
     */
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        recurringTransactionDao.delete(recurringTransaction)
    }
    
    /**
     * 更新定期交易状态
     */
    suspend fun updateStatus(id: Long, status: Int) {
        recurringTransactionDao.updateStatus(id, status)
    }
    
    /**
     * 处理今天需要执行的定期交易
     * 自动创建交易记录并更新下次执行日期
     */
    suspend fun processDueTransactions() {
        val dueTransactions = recurringTransactionDao.getTransactionsDueToday()
        val currentDate = Date()
        
        dueTransactions.forEach { recurringTransaction ->
            // 创建实际交易记录
            createTransactionFromRecurringTransaction(recurringTransaction)
            
            // 计算下一次执行日期
            val nextExecutionDate = calculateNextExecutionDate(recurringTransaction)
            
            // 更新定期交易的执行信息
            recurringTransactionDao.updateAfterExecution(
                id = recurringTransaction.id,
                nextDate = nextExecutionDate,
                currentDate = currentDate
            )
        }
    }
    
    /**
     * 从定期交易创建实际的交易记录
     */
    private suspend fun createTransactionFromRecurringTransaction(recurringTransaction: RecurringTransaction) {
        val transaction = Transaction(
            id = 0, // 新记录，ID为0
            amount = recurringTransaction.amount,
            categoryId = recurringTransaction.categoryId,
            accountId = recurringTransaction.fromAccountId,
            date = Date(), // 今天的日期
            note = recurringTransaction.note ?: "",
            isIncome = recurringTransaction.type == 1, // 假定type=1为收入
            toAccountId = recurringTransaction.toAccountId
        )
        
        transactionRepository.addTransaction(transaction)
    }
    
    /**
     * 计算下一次执行日期
     */
    private fun calculateNextExecutionDate(recurringTransaction: RecurringTransaction): Date {
        val calendar = Calendar.getInstance().apply {
            time = recurringTransaction.nextExecutionDate
        }
        
        when (recurringTransaction.recurrenceType) {
            0 -> { // 每天
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            1 -> { // 每周
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            2 -> { // 每两周
                calendar.add(Calendar.WEEK_OF_YEAR, 2)
            }
            3 -> { // 每月
                calendar.add(Calendar.MONTH, 1)
            }
            4 -> { // 每季度
                calendar.add(Calendar.MONTH, 3)
            }
            5 -> { // 每年
                calendar.add(Calendar.YEAR, 1)
            }
            6 -> { // 自定义天数
                recurringTransaction.customRecurrenceDays?.let { days ->
                    calendar.add(Calendar.DAY_OF_MONTH, days)
                }
            }
        }
        
        return calendar.time
    }
    
    /**
     * 获取需要提前通知的定期交易
     */
    suspend fun getTransactionsForNotification(): List<RecurringTransaction> {
        return recurringTransactionDao.getTransactionsForNotification()
    }
    
    /**
     * 暂停定期交易
     */
    suspend fun pauseRecurringTransaction(id: Long) {
        recurringTransactionDao.updateStatus(id, 1) // 1表示暂停
    }
    
    /**
     * 恢复定期交易
     */
    suspend fun resumeRecurringTransaction(id: Long) {
        recurringTransactionDao.updateStatus(id, 0) // 0表示活跃
    }
} 