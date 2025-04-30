package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import com.ccjizhang.data.model.Transaction as ModelTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 带访问控制的交易数据访问对象
 * 实现细粒度的数据访问控制
 */
@Dao
interface TransactionWithAccessControlDao {

    /**
     * 获取所有交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getAllTransactionsWithAccessControl(currentUserId: Long, isAdmin: Boolean): Flow<List<ModelTransaction>>

    /**
     * 获取指定日期范围内的交易记录，带访问控制
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getTransactionsByDateRangeWithAccessControl(
        startDate: Date,
        endDate: Date,
        currentUserId: Long,
        isAdmin: Boolean
    ): Flow<List<ModelTransaction>>

    /**
     * 获取收入交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 1 AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getIncomeTransactionsWithAccessControl(currentUserId: Long, isAdmin: Boolean): Flow<List<ModelTransaction>>

    /**
     * 获取支出交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 0 AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getExpenseTransactionsWithAccessControl(currentUserId: Long, isAdmin: Boolean): Flow<List<ModelTransaction>>

    /**
     * 获取指定分类的交易记录，带访问控制
     * @param categoryId 分类ID
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getTransactionsByCategoryWithAccessControl(
        categoryId: Long,
        currentUserId: Long,
        isAdmin: Boolean
    ): Flow<List<ModelTransaction>>

    /**
     * 获取指定账户的交易记录，带访问控制
     * @param accountId 账户ID
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun getTransactionsByAccountWithAccessControl(
        accountId: Long,
        currentUserId: Long,
        isAdmin: Boolean
    ): Flow<List<ModelTransaction>>

    /**
     * 搜索交易记录，带访问控制
     * @param query 搜索关键词
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC")
    fun searchTransactionsWithAccessControl(
        query: String,
        currentUserId: Long,
        isAdmin: Boolean
    ): Flow<List<ModelTransaction>>

    /**
     * 获取交易记录总数，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE (:isAdmin = 1 OR createdBy = :currentUserId)")
    suspend fun getTransactionCountWithAccessControl(currentUserId: Long, isAdmin: Boolean): Int

    /**
     * 获取交易记录总金额，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE (:isAdmin = 1 OR createdBy = :currentUserId)")
    suspend fun getTotalAmountWithAccessControl(currentUserId: Long, isAdmin: Boolean): Double?

    /**
     * 获取收入交易总金额，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND (:isAdmin = 1 OR createdBy = :currentUserId)")
    suspend fun getTotalIncomeWithAccessControl(currentUserId: Long, isAdmin: Boolean): Double?

    /**
     * 获取支出交易总金额，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND (:isAdmin = 1 OR createdBy = :currentUserId)")
    suspend fun getTotalExpenseWithAccessControl(currentUserId: Long, isAdmin: Boolean): Double?

    /**
     * 分页获取所有交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllTransactionsPagedWithAccessControl(
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页获取收入交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 1 AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getIncomeTransactionsPagedWithAccessControl(
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页获取支出交易记录，带访问控制
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE isIncome = 0 AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpenseTransactionsPagedWithAccessControl(
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页获取指定日期范围的交易记录，带访问控制
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByDateRangePagedWithAccessControl(
        startDate: Date,
        endDate: Date,
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页获取指定分类的交易记录，带访问控制
     * @param categoryId 分类ID
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByCategoryPagedWithAccessControl(
        categoryId: Long,
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页获取指定账户的交易记录，带访问控制
     * @param accountId 账户ID
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsByAccountPagedWithAccessControl(
        accountId: Long,
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>

    /**
     * 分页搜索交易记录，带访问控制
     * @param query 搜索关键词
     * @param currentUserId 当前用户ID
     * @param isAdmin 是否为管理员
     * @param limit 每页数量
     * @param offset 偏移量
     */
    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' AND (:isAdmin = 1 OR createdBy = :currentUserId) ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun searchTransactionsPagedWithAccessControl(
        query: String,
        currentUserId: Long,
        isAdmin: Boolean,
        limit: Int,
        offset: Int
    ): List<ModelTransaction>
}
