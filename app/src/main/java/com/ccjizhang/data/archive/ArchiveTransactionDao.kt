package com.ccjizhang.data.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ccjizhang.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 归档交易数据访问对象
 * 用于访问归档数据库中的交易记录
 */
@Dao
interface ArchiveTransactionDao {
    
    /**
     * 插入交易记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long
    
    /**
     * 批量插入交易记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)
    
    /**
     * 获取所有交易记录
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /**
     * 获取指定日期范围内的交易记录
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>
    
    /**
     * 获取收入交易记录
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 1 ORDER BY date DESC")
    fun getIncomeTransactions(): Flow<List<Transaction>>
    
    /**
     * 获取支出交易记录
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 0 ORDER BY date DESC")
    fun getExpenseTransactions(): Flow<List<Transaction>>
    
    /**
     * 获取指定分类的交易记录
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    
    /**
     * 获取指定账户的交易记录
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    
    /**
     * 搜索交易记录
     */
    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    /**
     * 获取交易记录总数
     */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    /**
     * 获取交易记录总金额
     */
    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getTotalAmount(): Double?
    
    /**
     * 获取收入交易总金额
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    suspend fun getTotalIncome(): Double?
    
    /**
     * 获取支出交易总金额
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    suspend fun getTotalExpense(): Double?
}
