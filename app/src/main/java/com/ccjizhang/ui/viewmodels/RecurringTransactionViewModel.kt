package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * 定期交易视图模型
 * 负责定期交易自动化功能的数据处理和业务逻辑
 */
@HiltViewModel
class RecurringTransactionViewModel @Inject constructor(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // 所有账户，用于选择交易关联的账户
    val accounts: Flow<List<Account>> = accountRepository.getAllAccounts()

    // 支出分类，用于选择交易关联的分类
    val expenseCategories: Flow<List<Category>> = categoryRepository.getExpenseCategories()

    // 收入分类，用于选择交易关联的分类
    val incomeCategories: Flow<List<Category>> = categoryRepository.getIncomeCategories()

    // 所有定期交易
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = 
        recurringTransactionRepository.getAllRecurringTransactions()

    // 活跃的定期交易
    val activeRecurringTransactions: Flow<List<RecurringTransaction>> = 
        recurringTransactionRepository.getActiveRecurringTransactions()

    // 已暂停的定期交易
    val pausedRecurringTransactions: Flow<List<RecurringTransaction>> = 
        recurringTransactionRepository.getPausedRecurringTransactions()

    // 已完成的定期交易
    val completedRecurringTransactions: Flow<List<RecurringTransaction>> = 
        recurringTransactionRepository.getCompletedRecurringTransactions()

    // 未来7天内需要执行的定期交易
    val upcomingTransactions: Flow<List<RecurringTransaction>> = 
        recurringTransactionRepository.getUpcomingTransactionsInNext7Days()

    // 当前选中的标签页
    private val _selectedTab = MutableStateFlow(RecurringTransactionTab.ACTIVE)
    val selectedTab: StateFlow<RecurringTransactionTab> = _selectedTab

    // 当前选中的定期交易ID
    private val _selectedTransactionId = MutableStateFlow<Long?>(null)
    val selectedTransactionId: StateFlow<Long?> = _selectedTransactionId

    // Selected transaction details state
    private val _selectedTransactionDetails = MutableStateFlow<RecurringTransaction?>(null)
    val selectedTransactionDetails: StateFlow<RecurringTransaction?> = _selectedTransactionDetails

    // Error message state (Optional, but good practice)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 根据当前选择的标签页显示相应的定期交易
    val currentTabTransactions = combine(
        selectedTab,
        activeRecurringTransactions,
        pausedRecurringTransactions,
        completedRecurringTransactions,
        upcomingTransactions
    ) { tab, active, paused, completed, upcoming ->
        when (tab) {
            RecurringTransactionTab.ACTIVE -> active
            RecurringTransactionTab.PAUSED -> paused
            RecurringTransactionTab.COMPLETED -> completed
            RecurringTransactionTab.UPCOMING -> upcoming
            RecurringTransactionTab.ALL -> active + paused + completed
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 选择标签页
    fun selectTab(tab: RecurringTransactionTab) {
        _selectedTab.value = tab
    }

    // 选择定期交易
    fun selectTransaction(id: Long?) {
        _selectedTransactionId.value = id
        // Optionally load details when ID is selected
        // if (id != null) { selectTransactionDetails(id) }
    }

    // 加载并选择单个定期交易的详细信息
    fun selectTransactionDetails(id: Long) {
        viewModelScope.launch {
            _errorMessage.value = null // Clear previous error
            _selectedTransactionDetails.value = null // Clear previous details
            try {
                val details = recurringTransactionRepository.getRecurringTransactionById(id)
                _selectedTransactionDetails.value = details
            } catch (e: Exception) {
                _errorMessage.value = "加载定期交易详情失败: ${e.message}"
                // Log the error e.g., using Timber.e(e, "Failed to load recurring transaction details")
            }
        }
    }

    // 添加新的定期交易
    suspend fun addRecurringTransaction(
        type: Int,
        amount: Double,
        description: String,
        categoryId: Long?,
        fromAccountId: Long,
        toAccountId: Long?,
        firstExecutionDate: Date,
        endDate: Date?,
        recurrenceType: Int,
        customRecurrenceDays: Int?,
        specificRecurrenceDay: String?,
        weekdayMask: Int?,
        maxExecutions: Int,
        note: String?,
        notifyBeforeExecution: Boolean,
        notifyDaysBefore: Int?
    ): Long {
        // 计算下一次执行日期（默认为首次执行日期）
        val nextExecutionDate = firstExecutionDate

        val recurringTransaction = RecurringTransaction(
            type = type,
            amount = amount,
            description = description,
            categoryId = categoryId,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            firstExecutionDate = firstExecutionDate,
            endDate = endDate,
            recurrenceType = recurrenceType,
            customRecurrenceDays = customRecurrenceDays,
            specificRecurrenceDay = specificRecurrenceDay,
            weekdayMask = weekdayMask,
            nextExecutionDate = nextExecutionDate,
            maxExecutions = maxExecutions,
            note = note,
            notifyBeforeExecution = notifyBeforeExecution,
            notifyDaysBefore = notifyDaysBefore
        )
        return recurringTransactionRepository.addRecurringTransaction(recurringTransaction)
    }

    // 更新定期交易
    fun updateRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringTransactionRepository.updateRecurringTransaction(recurringTransaction)
        }
    }

    // 删除定期交易
    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringTransactionRepository.deleteRecurringTransaction(recurringTransaction)
            if (_selectedTransactionId.value == recurringTransaction.id) {
                _selectedTransactionId.value = null
            }
        }
    }

    // 暂停定期交易
    fun pauseRecurringTransaction(id: Long) {
        viewModelScope.launch {
            recurringTransactionRepository.pauseRecurringTransaction(id)
        }
    }

    // 恢复定期交易
    fun resumeRecurringTransaction(id: Long) {
        viewModelScope.launch {
            recurringTransactionRepository.resumeRecurringTransaction(id)
        }
    }

    // 计算定期交易的下一个执行日期描述
    fun getNextExecutionDateDescription(transaction: RecurringTransaction): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.time = transaction.nextExecutionDate

        // 计算与今天的差值
        val diffInMillis = calendar.timeInMillis - today.timeInMillis
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffInDays < 0 -> "已过期"
            diffInDays == 0 -> "今天"
            diffInDays == 1 -> "明天"
            diffInDays < 7 -> "${diffInDays}天后"
            diffInDays < 30 -> "${diffInDays / 7}周后"
            else -> "${calendar.get(Calendar.YEAR)}年${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日"
        }
    }

    // 获取重复类型描述
    fun getRecurrenceTypeDescription(transaction: RecurringTransaction): String {
        return when (transaction.recurrenceType) {
            0 -> "每天"
            1 -> "每周"
            2 -> "每两周"
            3 -> "每月"
            4 -> "每季度"
            5 -> "每年"
            6 -> "每${transaction.customRecurrenceDays}天"
            else -> "未知"
        }
    }
}

// 定期交易标签页
enum class RecurringTransactionTab {
    ACTIVE, PAUSED, COMPLETED, UPCOMING, ALL
} 