package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ccjizhang.data.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 定期交易数据访问对象
 */
@Dao
interface RecurringTransactionDao {
    
    /**
     * 插入新的定期交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransaction): Long
    
    /**
     * 更新定期交易
     */
    @Update
    suspend fun update(recurringTransaction: RecurringTransaction)
    
    /**
     * 删除定期交易
     */
    @Delete
    suspend fun delete(recurringTransaction: RecurringTransaction)
    
    /**
     * 根据ID获取单个定期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransaction?
    
    /**
     * 获取所有定期交易，按照下次执行日期排序
     */
    @Query("SELECT * FROM recurring_transactions ORDER BY nextExecutionDate ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * 获取活跃的定期交易，按照下次执行日期排序
     */
    @Query("SELECT * FROM recurring_transactions WHERE status = 0 ORDER BY nextExecutionDate ASC")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * 获取已暂停的定期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE status = 1 ORDER BY nextExecutionDate ASC")
    fun getPausedRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * 获取已完成的定期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE status = 2 ORDER BY lastExecutionDate DESC")
    fun getCompletedRecurringTransactions(): Flow<List<RecurringTransaction>>
    
    /**
     * 获取特定账户的定期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE fromAccountId = :accountId OR toAccountId = :accountId ORDER BY nextExecutionDate ASC")
    fun getRecurringTransactionsByAccountId(accountId: Long): Flow<List<RecurringTransaction>>
    
    /**
     * 获取特定分类的定期交易
     */
    @Query("SELECT * FROM recurring_transactions WHERE categoryId = :categoryId ORDER BY nextExecutionDate ASC")
    fun getRecurringTransactionsByCategoryId(categoryId: Long): Flow<List<RecurringTransaction>>
    
    /**
     * 获取今天需要执行的定期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE status = 0 
        AND date(nextExecutionDate) <= date(:currentDate)
        AND (endDate IS NULL OR date(endDate) >= date(:currentDate))
        AND (maxExecutions = 0 OR totalExecutions < maxExecutions)
    """)
    suspend fun getTransactionsDueToday(currentDate: Date = Date()): List<RecurringTransaction>
    
    /**
     * 获取未来7天内需要执行的定期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE status = 0 
        AND date(nextExecutionDate) <= date(:currentDate, '+7 days')
        AND date(nextExecutionDate) > date(:currentDate)
        AND (endDate IS NULL OR date(endDate) >= date(:currentDate))
        AND (maxExecutions = 0 OR totalExecutions < maxExecutions)
    """)
    fun getUpcomingTransactionsInNext7Days(currentDate: Date = Date()): Flow<List<RecurringTransaction>>
    
    /**
     * 更新定期交易状态
     */
    @Query("UPDATE recurring_transactions SET status = :status, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, updateTime: Date = Date())
    
    /**
     * 更新定期交易执行信息
     */
    @Transaction
    suspend fun updateAfterExecution(id: Long, nextDate: Date, currentDate: Date = Date()) {
        val transaction = getById(id) ?: return
        
        // 增加执行次数
        val newTotalExecutions = transaction.totalExecutions + 1
        
        // 检查是否达到最大执行次数
        val newStatus = if (transaction.maxExecutions > 0 && newTotalExecutions >= transaction.maxExecutions) {
            2 // 已完成
        } else {
            transaction.status
        }
        
        // 更新记录
        updateExecutionInfo(
            id = id,
            lastExecutionDate = currentDate,
            nextExecutionDate = nextDate,
            totalExecutions = newTotalExecutions,
            status = newStatus,
            updateTime = currentDate
        )
    }
    
    /**
     * 更新执行信息
     */
    @Query("""
        UPDATE recurring_transactions 
        SET lastExecutionDate = :lastExecutionDate,
            nextExecutionDate = :nextExecutionDate,
            totalExecutions = :totalExecutions,
            status = :status,
            updatedAt = :updateTime
        WHERE id = :id
    """)
    suspend fun updateExecutionInfo(
        id: Long,
        lastExecutionDate: Date,
        nextExecutionDate: Date,
        totalExecutions: Int,
        status: Int,
        updateTime: Date = Date()
    )
    
    /**
     * 获取需要提前通知的定期交易
     */
    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE notifyBeforeExecution = 1
        AND notifyDaysBefore IS NOT NULL
        AND status = 0
        AND date(nextExecutionDate, '-' || notifyDaysBefore || ' days') = date(:currentDate)
    """)
    suspend fun getTransactionsForNotification(currentDate: Date = Date()): List<RecurringTransaction>
} 