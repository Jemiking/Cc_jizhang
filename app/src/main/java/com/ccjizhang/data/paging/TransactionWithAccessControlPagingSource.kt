package com.ccjizhang.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ccjizhang.data.db.dao.TransactionWithAccessControlDao
import com.ccjizhang.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 带访问控制的交易分页数据源
 * 使用 Paging 库实现高效的分页加载，同时支持访问控制
 */
class TransactionWithAccessControlPagingSource(
    private val transactionWithAccessControlDao: TransactionWithAccessControlDao,
    private val currentUserId: Long,
    private val isAdmin: Boolean,
    private val filterType: TransactionFilterType = TransactionFilterType.ALL,
    private val startDate: Date? = null,
    private val endDate: Date? = null,
    private val searchQuery: String = "",
    private val categoryId: Long = 0L,
    private val accountId: Long = 0L
) : PagingSource<Int, Transaction>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        return try {
            // 获取当前页码，如果为null则默认为0（第一页）
            val page = params.key ?: 0
            
            // 计算偏移量
            val offset = page * params.loadSize
            
            // 根据筛选条件获取交易数据
            val transactions = withContext(Dispatchers.IO) {
                when {
                    // 如果有搜索关键词，使用搜索查询
                    searchQuery.isNotEmpty() -> {
                        transactionWithAccessControlDao.searchTransactionsPagedWithAccessControl(
                            query = searchQuery,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            limit = params.loadSize,
                            offset = offset
                        )
                    }
                    
                    // 如果有日期范围，使用日期范围查询
                    startDate != null && endDate != null -> {
                        transactionWithAccessControlDao.getTransactionsByDateRangePagedWithAccessControl(
                            startDate = startDate,
                            endDate = endDate,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            limit = params.loadSize,
                            offset = offset
                        )
                    }
                    
                    // 如果有分类ID，使用分类查询
                    categoryId > 0 -> {
                        transactionWithAccessControlDao.getTransactionsByCategoryPagedWithAccessControl(
                            categoryId = categoryId,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            limit = params.loadSize,
                            offset = offset
                        )
                    }
                    
                    // 如果有账户ID，使用账户查询
                    accountId > 0 -> {
                        transactionWithAccessControlDao.getTransactionsByAccountPagedWithAccessControl(
                            accountId = accountId,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            limit = params.loadSize,
                            offset = offset
                        )
                    }
                    
                    // 根据筛选类型查询
                    else -> when (filterType) {
                        TransactionFilterType.ALL -> {
                            transactionWithAccessControlDao.getAllTransactionsPagedWithAccessControl(
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                limit = params.loadSize,
                                offset = offset
                            )
                        }
                        TransactionFilterType.INCOME -> {
                            transactionWithAccessControlDao.getIncomeTransactionsPagedWithAccessControl(
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                limit = params.loadSize,
                                offset = offset
                            )
                        }
                        TransactionFilterType.EXPENSE -> {
                            transactionWithAccessControlDao.getExpenseTransactionsPagedWithAccessControl(
                                currentUserId = currentUserId,
                                isAdmin = isAdmin,
                                limit = params.loadSize,
                                offset = offset
                            )
                        }
                    }
                }
            }
            
            // 计算上一页和下一页的页码
            val prevKey = if (page > 0) page - 1 else null
            val nextKey = if (transactions.size == params.loadSize) page + 1 else null
            
            // 返回加载结果
            LoadResult.Page(
                data = transactions,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            // 如果加载失败，返回错误结果
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        // 获取最近访问的页面位置作为刷新键
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
