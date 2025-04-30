package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import com.ccjizhang.data.db.relations.TransactionWithTags
import com.ccjizhang.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.time.LocalDate
import java.time.YearMonth

/**
 * 交易数据访问对象
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: com.ccjizhang.data.model.Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<com.ccjizhang.data.model.Transaction>)

    @Update
    suspend fun update(transaction: com.ccjizhang.data.model.Transaction)

    @Delete
    suspend fun delete(transaction: com.ccjizhang.data.model.Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除所有交易记录
     * @return 删除的记录数量
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int

    /**
     * 删除指定日期之前的交易记录
     * @param date 日期阈值
     * @return 删除的记录数量
     */
    @Query("DELETE FROM transactions WHERE date < :date")
    suspend fun deleteTransactionsBeforeDate(date: Date): Int

    /**
     * 获取指定日期之前的交易记录
     * @param date 日期阈值
     * @return 交易记录列表
     */
    @Query("SELECT * FROM transactions WHERE date < :date ORDER BY date DESC")
    fun getTransactionsBeforeDate(date: Date): Flow<List<Transaction>>

    /**
     * 根据ID列表删除交易记录
     * @param ids 要删除的交易ID列表
     * @return 删除的记录数量
     */
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    /**
     * 同步获取所有交易记录
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>

    /**
     * 获取所有交易记录
     * 优化查询，只选择必要的列
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdSync(id: Long): Transaction?

    /**
     * 获取所有收入交易
     * 优化查询，只选择必要的列
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE isIncome = 1 ORDER BY date DESC")
    fun getIncomeTransactions(): Flow<List<Transaction>>

    /**
     * 获取所有支出交易
     * 优化查询，只选择必要的列
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE isIncome = 0 ORDER BY date DESC")
    fun getExpenseTransactions(): Flow<List<Transaction>>

    /**
     * 按日期范围获取交易
     * 优化查询，只选择必要的列
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsByDateRangeSync(startDate: Date, endDate: Date): List<Transaction>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>

    /**
     * 获取所有使用指定标签的交易ID
     */
    @Query("SELECT t.id FROM transactions t INNER JOIN transaction_tags tt ON t.id = tt.transactionId WHERE tt.tag = :tag")
    fun getTransactionIdsByTag(tag: String): Flow<List<Long>>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND accountId = :accountId")
    suspend fun getTotalExpenseByAccountSync(accountId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND accountId = :accountId")
    suspend fun getTotalIncomeByAccountSync(accountId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    fun getAmountByCategoryAndDateRange(categoryId: Long, startDate: Date, endDate: Date): Flow<Double?>

    /**
     * 获取带有标签的交易
     */
    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getTransactionWithTags(transactionId: Long): Flow<TransactionWithTags?>

    /**
     * 获取所有带有标签的交易
     */
    @RoomTransaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactionsWithTags(): Flow<List<TransactionWithTags>>

    /**
     * 获取含有特定标签的所有交易
     */
    @RoomTransaction
    @Query("SELECT t.* FROM transactions t INNER JOIN transaction_tags tt ON t.id = tt.transactionId WHERE tt.tag = :tag ORDER BY t.date DESC")
    fun getTransactionsByTag(tag: String): Flow<List<TransactionWithTags>>

    /**
     * 根据关键词搜索交易记录
     * 支持搜索交易备注和金额
     */
    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' OR amount = :amountQuery ORDER BY date DESC")
    fun searchTransactions(query: String, amountQuery: Double = -1.0): Flow<List<Transaction>>

    /**
     * 综合搜索交易记录
     * 支持多条件组合查询：关键词、日期范围、收入/支出类型、账户、分类
     */
    @Query("SELECT * FROM transactions WHERE " +
           "(:query = '' OR note LIKE '%' || :query || '%') AND " +
           "(:startDate IS NULL OR date >= :startDate) AND " +
           "(:endDate IS NULL OR date <= :endDate) AND " +
           "(:isIncome IS NULL OR isIncome = :isIncome) AND " +
           "((:accountId = 0) OR (accountId = :accountId)) AND " +
           "((:categoryId = 0) OR (categoryId = :categoryId)) " +
           "ORDER BY date DESC")
    fun advancedSearchTransactions(
        query: String = "",
        startDate: Date? = null,
        endDate: Date? = null,
        isIncome: Boolean? = null,
        accountId: Long = 0,
        categoryId: Long = 0
    ): Flow<List<Transaction>>

    /**
     * 按分类获取特定日期范围内各类型交易的金额和数量统计
     */
    @Query("SELECT categoryId, SUM(amount) as totalAmount, COUNT(id) as count " +
           "FROM transactions " +
           "WHERE date BETWEEN :startDate AND :endDate AND isIncome = :isIncome " +
           "GROUP BY categoryId")
    fun getCategoryStatsByDateRange(startDate: Date, endDate: Date, isIncome: Boolean): Flow<List<CategoryStat>>

    /**
     * 获取年份月份内的所有交易
     */
    @Query("SELECT * FROM transactions " +
           "WHERE strftime('%Y', date/1000, 'unixepoch') = :year " +
           "AND strftime('%m', date/1000, 'unixepoch') = :month")
    suspend fun getTransactionsByYearMonthSync(year: String, month: String): List<Transaction>

    /**
     * 按月份分组获取统计数据
     */
    @Query("SELECT strftime('%Y-%m', date/1000, 'unixepoch') as month, " +
           "SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) as totalExpense, " +
           "SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) as totalIncome " +
           "FROM transactions " +
           "WHERE strftime('%Y', date/1000, 'unixepoch') = :year " +
           "GROUP BY month " +
           "ORDER BY month")
    fun getMonthlyStatsByYear(year: String): Flow<List<MonthlyStat>>

    /**
     * 获取净资产变化趋势
     */
    @Query("SELECT strftime('%Y-%m', date/1000, 'unixepoch') as month, " +
           "SUM(CASE WHEN isIncome = 1 THEN amount ELSE -amount END) as netAmount " +
           "FROM transactions " +
           "GROUP BY month " +
           "ORDER BY month")
    fun getNetAmountTrend(): Flow<List<NetAmountStat>>

    /**
     * 获取特定日期范围内的支出和收入汇总
     */
    @Query("SELECT " +
           "SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) as totalExpense, " +
           "SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) as totalIncome " +
           "FROM transactions " +
           "WHERE date BETWEEN :startDate AND :endDate")
    fun getSummaryByDateRange(startDate: Date, endDate: Date): Flow<TransactionSummary>

    /**
     * 获取每日支出趋势
     */
    @Query("SELECT strftime('%Y-%m-%d', date/1000, 'unixepoch') as day, " +
           "SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) as totalExpense " +
           "FROM transactions " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY day " +
           "ORDER BY day")
    fun getDailyExpenseTrend(startDate: Date, endDate: Date): Flow<List<DailyStat>>

    /**
     * 获取按周统计的支出趋势
     */
    @Query("SELECT strftime('%Y-%W', date/1000, 'unixepoch') as week, " +
           "SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) as totalExpense, " +
           "SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) as totalIncome " +
           "FROM transactions " +
           "WHERE date BETWEEN :startDate AND :endDate " +
           "GROUP BY week " +
           "ORDER BY week")
    fun getWeeklyStatsByDateRange(startDate: Date, endDate: Date): Flow<List<WeeklyStat>>

    /**
     * 按分类获取支出统计
     */
    @Query("SELECT categoryId, SUM(amount) as totalAmount, COUNT(id) as count " +
           "FROM transactions " +
           "WHERE isIncome = 0 " +
           "AND (:startDate IS NULL OR date >= :startDate) " +
           "AND (:endDate IS NULL OR date <= :endDate) " +
           "GROUP BY categoryId")
    fun getExpenseStatsByCategory(startDate: Date? = null, endDate: Date? = null): Flow<List<CategoryStat>>

    /**
     * 获取收入/支出类型统计
     */
    @Query("SELECT " +
           "SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) as totalExpense, " +
           "SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) as totalIncome " +
           "FROM transactions " +
           "WHERE (:startDate IS NULL OR date >= :startDate) " +
           "AND (:endDate IS NULL OR date <= :endDate)")
    fun getTransactionTypeStats(startDate: Date? = null, endDate: Date? = null): Flow<TransactionSummary>

    /**
     * 获取最近的交易记录
     * @param limit 限制返回的记录数
     * 优化查询，只选择必要的列并使用LIMIT
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    /**
     * 分页获取所有交易
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllTransactionsPaged(limit: Int, offset: Int): List<Transaction>

    /**
     * 分页获取收入交易
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE isIncome = 1 ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getIncomeTransactionsPaged(limit: Int, offset: Int): List<Transaction>

    /**
     * 分页获取支出交易
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE isIncome = 0 ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpenseTransactionsPaged(limit: Int, offset: Int): List<Transaction>

    /**
     * 分页获取指定日期范围的交易
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByDateRangePaged(startDate: Date, endDate: Date, limit: Int, offset: Int): List<Transaction>

    /**
     * 分页获取指定分类的交易
     * @param categoryId 分类ID
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByCategoryPaged(categoryId: Long, limit: Int, offset: Int): List<Transaction>

    /**
     * 分页获取指定账户的交易
     * @param accountId 账户ID
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE accountId = :accountId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByAccountPaged(accountId: Long, limit: Int, offset: Int): List<Transaction>

    /**
     * 分页搜索交易
     * @param query 搜索关键词
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT id, amount, categoryId, accountId, date, note, isIncome, location, imageUri, toAccountId, createdBy, createdAt, updatedAt, isPrivate FROM transactions WHERE note LIKE '%' || :query || '%' OR amount = :query ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun searchTransactionsPaged(query: String, limit: Int, offset: Int): List<Transaction>

    /**
     * 获取引用了无效分类的交易
     * 即分类存在但在分类表中不存在的交易
     */
    @Query("SELECT t.* FROM transactions t LEFT JOIN categories c ON t.categoryId = c.id WHERE t.categoryId IS NOT NULL AND c.id IS NULL")
    suspend fun getTransactionsWithInvalidCategory(): List<Transaction>

    /**
     * 获取引用了无效账户的交易
     * 即账户存在但在账户表中不存在的交易
     */
    @Query("SELECT t.* FROM transactions t LEFT JOIN accounts a ON t.accountId = a.id WHERE a.id IS NULL")
    suspend fun getTransactionsWithInvalidAccount(): List<Transaction>
}

/**
 * 分类统计数据类
 */
data class CategoryStat(
    val categoryId: Long,
    val totalAmount: Double,
    val count: Int
)

/**
 * 月度统计数据类
 */
data class MonthlyStat(
    val month: String,
    val totalExpense: Double,
    val totalIncome: Double
)

/**
 * 净资产变化统计数据类
 */
data class NetAmountStat(
    val month: String,
    val netAmount: Double
)

/**
 * 交易汇总数据类
 */
data class TransactionSummary(
    val totalExpense: Double,
    val totalIncome: Double
)

/**
 * 每日统计数据类
 */
data class DailyStat(
    val day: String,
    val totalExpense: Double
)

/**
 * 每周统计数据类
 */
data class WeeklyStat(
    val week: String,
    val totalExpense: Double,
    val totalIncome: Double
)

/**
 * 交易类型统计数据类
 */
data class TransactionTypeStat(
    val type: String,
    val totalAmount: Double,
    val count: Int
)