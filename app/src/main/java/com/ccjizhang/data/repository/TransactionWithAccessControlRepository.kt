package com.ccjizhang.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ccjizhang.data.db.dao.TransactionWithAccessControlDao
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.paging.TransactionFilterType
import com.ccjizhang.data.paging.TransactionWithAccessControlPagingSource
import com.ccjizhang.utils.AccessControlHelper
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 带访问控制的交易数据仓库
 * 负责提供带访问控制的交易数据和处理业务逻辑
 */
@Singleton
class TransactionWithAccessControlRepository @Inject constructor(
    private val transactionWithAccessControlDao: TransactionWithAccessControlDao,
    private val accessControlHelper: AccessControlHelper,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "TransactionWithAccessControlRepository"
    }
    
    /**
     * 获取所有交易，带访问控制
     */
    fun getAllTransactionsWithAccessControl(): Flow<List<Transaction>> = flow {
        try {
            val currentUserId = accessControlHelper.getCurrentUserId()
            val isAdmin = accessControlHelper.isCurrentUserAdmin()
            
            emitAll(
                transactionWithAccessControlDao.getAllTransactionsWithAccessControl(currentUserId, isAdmin)
                    .catch { e ->
                        Log.e(TAG, "获取交易列表失败", e)
                        if (e is Exception) {
                            databaseExceptionHandler.handleDatabaseException(e, "获取交易列表")
                        }
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取交易列表失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取交易列表")
            emit(emptyList())
        }
    }
    
    /**
     * 按日期范围获取交易，带访问控制
     */
    fun getTransactionsByDateRangeWithAccessControl(
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>> = flow {
        try {
            val currentUserId = accessControlHelper.getCurrentUserId()
            val isAdmin = accessControlHelper.isCurrentUserAdmin()
            
            emitAll(
                transactionWithAccessControlDao.getTransactionsByDateRangeWithAccessControl(
                    startDate, endDate, currentUserId, isAdmin
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "按日期范围获取交易失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "按日期范围获取交易")
            emit(emptyList())
        }
    }
    
    /**
     * 分页获取交易数据，带访问控制
     * 使用 Paging 3 库实现高效分页加载
     * @param pageSize 每页数量
     * @param filterType 筛选类型（全部、收入、支出）
     * @return 分页数据流
     */
    fun getTransactionsPagedWithAccessControl(
        pageSize: Int = 20,
        filterType: TransactionFilterType = TransactionFilterType.ALL
    ): Flow<PagingData<Transaction>> {
        val currentUserId = accessControlHelper.getCurrentUserId()
        val isAdmin = accessControlHelper.isCurrentUserAdmin()
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionWithAccessControlPagingSource(
                    transactionWithAccessControlDao = transactionWithAccessControlDao,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    filterType = filterType
                )
            }
        ).flow
    }
    
    /**
     * 按日期范围分页获取交易，带访问控制
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByDateRangePagedWithAccessControl(
        startDate: Date,
        endDate: Date,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        val currentUserId = accessControlHelper.getCurrentUserId()
        val isAdmin = accessControlHelper.isCurrentUserAdmin()
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionWithAccessControlPagingSource(
                    transactionWithAccessControlDao = transactionWithAccessControlDao,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    startDate = startDate,
                    endDate = endDate
                )
            }
        ).flow
    }
    
    /**
     * 按分类分页获取交易，带访问控制
     * @param categoryId 分类ID
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByCategoryPagedWithAccessControl(
        categoryId: Long,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        val currentUserId = accessControlHelper.getCurrentUserId()
        val isAdmin = accessControlHelper.isCurrentUserAdmin()
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionWithAccessControlPagingSource(
                    transactionWithAccessControlDao = transactionWithAccessControlDao,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    categoryId = categoryId
                )
            }
        ).flow
    }
    
    /**
     * 按账户分页获取交易，带访问控制
     * @param accountId 账户ID
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun getTransactionsByAccountPagedWithAccessControl(
        accountId: Long,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        val currentUserId = accessControlHelper.getCurrentUserId()
        val isAdmin = accessControlHelper.isCurrentUserAdmin()
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionWithAccessControlPagingSource(
                    transactionWithAccessControlDao = transactionWithAccessControlDao,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    accountId = accountId
                )
            }
        ).flow
    }
    
    /**
     * 搜索交易并分页返回结果，带访问控制
     * @param query 搜索关键词
     * @param pageSize 每页数量
     * @return 分页数据流
     */
    fun searchTransactionsPagedWithAccessControl(
        query: String,
        pageSize: Int = 20
    ): Flow<PagingData<Transaction>> {
        val currentUserId = accessControlHelper.getCurrentUserId()
        val isAdmin = accessControlHelper.isCurrentUserAdmin()
        
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                TransactionWithAccessControlPagingSource(
                    transactionWithAccessControlDao = transactionWithAccessControlDao,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    searchQuery = query
                )
            }
        ).flow
    }
    
    /**
     * 获取交易记录总数，带访问控制
     */
    suspend fun getTransactionCountWithAccessControl(): Int {
        return try {
            val currentUserId = accessControlHelper.getCurrentUserId()
            val isAdmin = accessControlHelper.isCurrentUserAdmin()
            
            transactionWithAccessControlDao.getTransactionCountWithAccessControl(currentUserId, isAdmin)
        } catch (e: Exception) {
            Log.e(TAG, "获取交易记录总数失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取交易记录总数")
            0
        }
    }
    
    /**
     * 获取收入交易总金额，带访问控制
     */
    suspend fun getTotalIncomeWithAccessControl(): Double {
        return try {
            val currentUserId = accessControlHelper.getCurrentUserId()
            val isAdmin = accessControlHelper.isCurrentUserAdmin()
            
            transactionWithAccessControlDao.getTotalIncomeWithAccessControl(currentUserId, isAdmin) ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "获取收入交易总金额失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取收入交易总金额")
            0.0
        }
    }
    
    /**
     * 获取支出交易总金额，带访问控制
     */
    suspend fun getTotalExpenseWithAccessControl(): Double {
        return try {
            val currentUserId = accessControlHelper.getCurrentUserId()
            val isAdmin = accessControlHelper.isCurrentUserAdmin()
            
            transactionWithAccessControlDao.getTotalExpenseWithAccessControl(currentUserId, isAdmin) ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "获取支出交易总金额失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取支出交易总金额")
            0.0
        }
    }
}
