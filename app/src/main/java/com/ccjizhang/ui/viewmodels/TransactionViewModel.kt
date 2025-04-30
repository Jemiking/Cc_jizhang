package com.ccjizhang.ui.viewmodels

import android.net.Uri
import android.location.Address
import android.location.Geocoder
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.paging.TransactionFilterType
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.BatchOperationRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * 高级搜索条件数据类
 */
data class TransactionSearchCriteria(
    val query: String = "",          // 搜索关键词
    val startDate: Date? = null,     // 开始日期
    val endDate: Date? = null,       // 结束日期
    val isIncome: Boolean? = null,   // 是否为收入
    val accountId: Long = 0L,        // 账户ID
    val categoryId: Long = 0L        // 分类ID
)

/**
 * 交易数据ViewModel
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val batchOperationRepository: BatchOperationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 所有交易列表
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // 分页交易数据
    private val _transactionsPaged = MutableStateFlow<PagingData<Transaction>>(PagingData.empty())
    val transactionsPaged: StateFlow<PagingData<Transaction>> = _transactionsPaged.asStateFlow()

    // 当前选中的交易
    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    // 交易过滤状态
    private val _filterType = MutableStateFlow<com.ccjizhang.data.paging.TransactionFilterType>(com.ccjizhang.data.paging.TransactionFilterType.ALL)
    val filterType: StateFlow<com.ccjizhang.data.paging.TransactionFilterType> = _filterType.asStateFlow()

    // 当前搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 高级搜索条件
    private val _searchCriteria = MutableStateFlow(TransactionSearchCriteria())
    val searchCriteria: StateFlow<TransactionSearchCriteria> = _searchCriteria.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 是否在搜索模式
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode: StateFlow<Boolean> = _isSearchMode.asStateFlow()

    // 当前选择的图片URI
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    // 当前位置信息
    private val _locationAddress = MutableStateFlow<String>("")
    val locationAddress: StateFlow<String> = _locationAddress.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 初始化 - 加载数据
    init {
        loadTransactions()
    }

    /**
     * 加载交易数据
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true

            // 从Repository加载数据
            transactionRepository.getAllTransactions().collectLatest { transactions ->
                _transactions.value = transactions
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载分页交易数据
     * 使用 Paging 3 库实现高效分页加载
     * @param filterType 筛选类型（全部、收入、支出）
     */
    fun loadTransactionsPaged(filterType: TransactionFilterType = TransactionFilterType.ALL) {
        viewModelScope.launch {
            _isLoading.value = true

            // 使用 Repository 的分页方法加载数据
            // cachedIn 确保分页数据在 ViewModel 生命周期内缓存
            transactionRepository.getTransactionsPaged(filterType = filterType)
                .cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _transactionsPaged.value = pagingData
                    _isLoading.value = false
                }
        }
    }

    /**
     * 按日期范围加载分页交易数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    fun loadTransactionsByDateRangePaged(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.getTransactionsByDateRangePaged(startDate, endDate)
                .cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _transactionsPaged.value = pagingData
                    _isLoading.value = false
                }
        }
    }

    /**
     * 按分类加载分页交易数据
     * @param categoryId 分类ID
     */
    fun loadTransactionsByCategoryPaged(categoryId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.getTransactionsByCategoryPaged(categoryId)
                .cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _transactionsPaged.value = pagingData
                    _isLoading.value = false
                }
        }
    }

    /**
     * 按账户加载分页交易数据
     * @param accountId 账户ID
     */
    fun loadTransactionsByAccountPaged(accountId: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.getTransactionsByAccountPaged(accountId)
                .cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _transactionsPaged.value = pagingData
                    _isLoading.value = false
                }
        }
    }

    /**
     * 搜索交易并分页返回结果
     * @param query 搜索关键词
     */
    fun searchTransactionsPaged(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.searchTransactionsPaged(query)
                .cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _transactionsPaged.value = pagingData
                    _isLoading.value = false
                }
        }
    }

    /**
     * 设置简单搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // 如果搜索词为空，退出搜索模式并加载所有交易
            _isSearchMode.value = false
            loadFilteredTransactions()
        } else {
            // 否则进入搜索模式并执行搜索
            _isSearchMode.value = true
            searchTransactions(query)
        }
    }

    /**
     * 简单关键词搜索
     */
    private fun searchTransactions(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.searchTransactions(query).collectLatest { results ->
                _transactions.value = results
                _isLoading.value = false
            }
        }
    }

    /**
     * 设置高级搜索条件
     */
    fun setSearchCriteria(criteria: TransactionSearchCriteria) {
        _searchCriteria.value = criteria
        _isSearchMode.value = true
        advancedSearchTransactions(criteria)
    }

    /**
     * 高级搜索
     */
    private fun advancedSearchTransactions(criteria: TransactionSearchCriteria) {
        viewModelScope.launch {
            _isLoading.value = true

            transactionRepository.advancedSearchTransactions(
                query = criteria.query,
                startDate = criteria.startDate,
                endDate = criteria.endDate,
                isIncome = criteria.isIncome,
                accountId = criteria.accountId,
                categoryId = criteria.categoryId
            ).collectLatest { results ->
                _transactions.value = results
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除搜索条件，回到默认视图
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchCriteria.value = TransactionSearchCriteria()
        _isSearchMode.value = false
        loadFilteredTransactions()
    }

    /**
     * 加载交易详情
     */
    fun loadTransactionDetails(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            // 从Repository加载交易详情
            transactionRepository.getTransactionById(id).collectLatest { transaction ->
                _selectedTransaction.value = transaction
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加新交易
     */
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                // 保存交易到数据库并自动更新账户余额
                val id = transactionRepository.addTransactionWithBalanceUpdate(transaction)

                if (id > 0) {
                    // 成功添加交易
                    Log.i("TransactionViewModel", "成功添加交易，ID: $id")
                } else {
                    // 添加交易失败
                    Log.e("TransactionViewModel", "添加交易失败，返回的ID为负数")
                    setError("添加交易失败，请重试")
                }

                // 重新加载交易列表
                loadTransactions()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "添加交易时发生异常", e)
                setError("添加交易时发生错误: ${e.message}")
            }
        }
    }

    /**
     * 更新交易
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // 获取原交易
            transactionRepository.getTransactionById(transaction.id).collectLatest { original ->
                if (original != null) {
                    // 更新交易并自动调整账户余额
                    transactionRepository.updateTransactionWithBalanceUpdate(original, transaction)

                    // 如果当前选中的交易被更新，刷新详情
                    if (_selectedTransaction.value?.id == transaction.id) {
                        _selectedTransaction.value = transaction
                    }

                    // 重新加载交易列表
                    loadTransactions()
                }
            }
        }
    }

    /**
     * 删除交易
     */
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            // 删除交易并自动恢复账户余额
            transactionRepository.deleteTransactionByIdWithBalanceUpdate(id)

            // 如果删除的是当前选中的交易，清除选中状态
            if (_selectedTransaction.value?.id == id) {
                _selectedTransaction.value = null
            }

            // 重新加载交易列表
            loadTransactions()
        }
    }

    /**
     * 批量删除交易
     * 使用优化的批量操作实现
     */
    fun batchDeleteTransactions(ids: List<Long>) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // 使用批量操作仓库进行批量删除，单一事务内完成
                val deletedCount = batchOperationRepository.batchDeleteTransactions(ids)
                Log.i("TransactionViewModel", "批量删除交易成功，共删除 $deletedCount 条记录")

                // 如果当前选中的交易在被删除列表中，清除选中状态
                if (_selectedTransaction.value != null && ids.contains(_selectedTransaction.value?.id)) {
                    _selectedTransaction.value = null
                }

                // 重新加载交易列表
                loadTransactions()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "批量删除交易失败", e)
                setError("批量删除交易失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 批量复制交易（创建相同交易的副本）
     * 使用优化的批量操作实现
     */
    fun batchCopyTransactions(ids: List<Long>) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // 获取所有要复制的交易
                val transactionsToCopy = mutableListOf<Transaction>()

                ids.forEach { id ->
                    transactionRepository.getTransactionById(id).collectLatest { transaction ->
                        if (transaction != null) {
                            // 创建交易副本（ID为0以便自动生成新ID）
                            val copy = transaction.copy(id = 0L)
                            transactionsToCopy.add(copy)
                        }
                    }
                }

                // 使用批量操作仓库进行批量添加，单一事务内完成
                val addedCount = batchOperationRepository.batchAddTransactions(transactionsToCopy)
                Log.i("TransactionViewModel", "批量复制交易成功，共添加 $addedCount 条记录")

                // 重新加载交易列表
                loadTransactions()
            } catch (e: Exception) {
                Log.e("TransactionViewModel", "批量复制交易失败", e)
                setError("批量复制交易失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 设置筛选类型
     * @param usePaging 是否使用分页加载
     */
    fun setFilterType(type: com.ccjizhang.data.paging.TransactionFilterType, usePaging: Boolean = false) {
        _filterType.value = type

        if (usePaging) {
            // 使用分页加载
            loadTransactionsPaged(type)
        } else {
            // 使用传统加载
            loadFilteredTransactions()
        }
    }

    /**
     * 加载筛选后的交易列表
     */
    private fun loadFilteredTransactions() {
        viewModelScope.launch {
            _isLoading.value = true

            when (_filterType.value) {
                com.ccjizhang.data.paging.TransactionFilterType.ALL -> {
                    transactionRepository.getAllTransactions().collectLatest {
                        _transactions.value = it
                        _isLoading.value = false
                    }
                }
                com.ccjizhang.data.paging.TransactionFilterType.EXPENSE -> {
                    transactionRepository.getExpenseTransactions().collectLatest {
                        _transactions.value = it
                        _isLoading.value = false
                    }
                }
                com.ccjizhang.data.paging.TransactionFilterType.INCOME -> {
                    transactionRepository.getIncomeTransactions().collectLatest {
                        _transactions.value = it
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    /**
     * 设置所选图片URI
     */
    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * 从图片URI创建交易附件字符串用于存储
     */
    private fun getImageUriForStorage(uri: Uri?): String {
        return uri?.toString() ?: ""
    }

    /**
     * 设置位置信息
     */
    fun setLocationAddress(address: String) {
        _locationAddress.value = address
    }

    /**
     * 从经纬度获取地址信息
     * 使用新的 API 替代已弃用的 getFromLocation 方法
     */
    fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(context, java.util.Locale.getDefault())

            // 使用新的 API
            val addressList = mutableListOf<Address>()
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                addressList.addAll(addresses)
            }

            // 等待一小段时间以确保回调有时间执行
            kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(500) }

            if (addressList.isNotEmpty()) {
                val address = addressList[0]
                val sb = StringBuilder()

                // 获取地址详情
                if (address.featureName != null) sb.append(address.featureName).append(", ")
                if (address.subLocality != null) sb.append(address.subLocality).append(", ")
                if (address.locality != null) sb.append(address.locality).append(", ")
                if (address.adminArea != null) sb.append(address.adminArea)

                // 如果没有获取到详细地址，只返回经纬度坐标
                return if (sb.isNotEmpty()) sb.toString() else "$latitude, $longitude"
            }

            return "$latitude, $longitude"
        } catch (e: Exception) {
            Log.e("TransactionViewModel", "获取地址信息失败", e)
            return "$latitude, $longitude"
        }
    }

    /**
     * 使用当前选中的图片和位置创建或更新交易
     */
    fun addTransactionWithAttachments(transaction: Transaction): Transaction {
        val imageUri = _selectedImageUri.value
        val locationInfo = _locationAddress.value

        return transaction.copy(
            imageUri = getImageUriForStorage(imageUri),
            location = locationInfo
        )
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 设置错误信息
     */
    private fun setError(message: String) {
        _errorMessage.value = message
    }
}

// 使用 com.ccjizhang.data.paging.TransactionFilterType 替代原来的枚举
// 不再需要在这里定义 TransactionFilterType