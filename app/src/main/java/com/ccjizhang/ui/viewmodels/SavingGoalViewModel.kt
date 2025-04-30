package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.SavingGoal
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.SavingGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * 储蓄目标视图模型
 * 负责储蓄目标功能的数据处理和业务逻辑
 */
@HiltViewModel
class SavingGoalViewModel @Inject constructor(
    private val savingGoalRepository: SavingGoalRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // 所有账户，用于选择与储蓄目标关联的账户
    val accounts: Flow<List<Account>> = accountRepository.getAllAccounts()

    // 所有储蓄目标
    val allSavingGoals: Flow<List<SavingGoal>> = savingGoalRepository.getAllSavingGoals()

    // 活跃的储蓄目标
    val activeSavingGoals: Flow<List<SavingGoal>> = savingGoalRepository.getActiveSavingGoals()

    // 已完成的储蓄目标
    val completedSavingGoals: Flow<List<SavingGoal>> = savingGoalRepository.getCompletedSavingGoals()

    // 过期未完成的储蓄目标
    val expiredSavingGoals: Flow<List<SavingGoal>> = savingGoalRepository.getExpiredSavingGoals()

    // 即将完成的储蓄目标
    val nearCompletionGoals: Flow<List<SavingGoal>> = savingGoalRepository.getNearCompletionGoals()

    // 当前选中的标签页
    private val _selectedTab = MutableStateFlow(SavingGoalTab.ACTIVE)
    val selectedTab: StateFlow<SavingGoalTab> = _selectedTab

    // 当前选中的目标
    private val _selectedGoalId = MutableStateFlow<Long?>(null)
    val selectedGoalId: StateFlow<Long?> = _selectedGoalId

    // 根据当前选择的标签页显示相应的储蓄目标
    val currentTabGoals = combine(
        selectedTab,
        activeSavingGoals,
        completedSavingGoals,
        expiredSavingGoals
    ) { tab, active, completed, expired ->
        when (tab) {
            SavingGoalTab.ACTIVE -> active
            SavingGoalTab.COMPLETED -> completed
            SavingGoalTab.EXPIRED -> expired
            SavingGoalTab.ALL -> active + completed + expired
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 选中目标的详细信息
    val selectedGoalDetails: Flow<SavingGoalDetails?> = selectedGoalId
        .map { id ->
            if (id == null) return@map null
            val goal = savingGoalRepository.getSavingGoalById(id) ?: return@map null
            
            // 使用runBlocking或者suspendCoroutine来收集Flow<Account?>中的值
            val account = if (goal.accountId != null) {
                var result: Account? = null
                runBlocking {
                    accountRepository.getAccountById(goal.accountId).collect { accountData ->
                        result = accountData
                    }
                }
                result
            } else null
            
            val progress = calculateGoalProgress(goal)
            val timeProgress = calculateTimeProgress(goal)
            val isCompleted = isGoalCompleted(goal)
            val isExpired = isGoalExpired(goal)
            SavingGoalDetails(goal, account, progress, timeProgress, isCompleted, isExpired)
        }

    // 选择标签页
    fun selectTab(tab: SavingGoalTab) {
        _selectedTab.value = tab
    }

    // 选择储蓄目标
    fun selectGoal(id: Long?) {
        _selectedGoalId.value = id
    }

    // 添加新的储蓄目标
    suspend fun addSavingGoal(
        name: String,
        targetAmount: Double,
        accountId: Long?,
        startDate: Date,
        targetDate: Date,
        priority: Int,
        iconUri: String?,
        color: Int,
        note: String?,
        autoSaveAmount: Double?,
        autoSaveFrequencyDays: Int?
    ): Long {
        val savingGoal = SavingGoal(
            name = name,
            targetAmount = targetAmount,
            accountId = accountId,
            startDate = startDate,
            targetDate = targetDate,
            priority = priority,
            iconUri = iconUri,
            color = color,
            note = note,
            autoSaveAmount = autoSaveAmount,
            autoSaveFrequencyDays = autoSaveFrequencyDays
        )
        return savingGoalRepository.addSavingGoal(savingGoal)
    }

    // 更新储蓄目标
    fun updateSavingGoal(savingGoal: SavingGoal) {
        viewModelScope.launch {
            savingGoalRepository.updateSavingGoal(savingGoal)
        }
    }

    // 删除储蓄目标
    fun deleteSavingGoal(savingGoal: SavingGoal) {
        viewModelScope.launch {
            savingGoalRepository.deleteSavingGoal(savingGoal)
            if (_selectedGoalId.value == savingGoal.id) {
                _selectedGoalId.value = null
            }
        }
    }

    // 向储蓄目标存入资金
    fun depositToGoal(goalId: Long, amount: Double) {
        viewModelScope.launch {
            savingGoalRepository.depositToGoal(goalId, amount)
        }
    }

    // 从储蓄目标取出资金
    fun withdrawFromGoal(goalId: Long, amount: Double) {
        viewModelScope.launch {
            savingGoalRepository.withdrawFromGoal(goalId, amount)
        }
    }
    
    // 计算储蓄目标的完成进度百分比
    fun calculateGoalProgress(goal: SavingGoal): Float {
        if (goal.targetAmount <= 0) return 0f
        return (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }
    
    // 计算储蓄目标的时间进度百分比
    fun calculateTimeProgress(goal: SavingGoal): Float {
        val now = Date().time
        val start = goal.startDate.time
        val end = goal.targetDate.time
        
        if (end <= start) return 1f
        
        return ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }
    
    // 检查储蓄目标是否已完成
    fun isGoalCompleted(goal: SavingGoal): Boolean {
        return goal.currentAmount >= goal.targetAmount
    }
    
    // 检查储蓄目标是否已过期
    fun isGoalExpired(goal: SavingGoal): Boolean {
        return !isGoalCompleted(goal) && goal.targetDate.before(Date())
    }
}

// 储蓄目标标签页
enum class SavingGoalTab {
    ACTIVE, COMPLETED, EXPIRED, ALL
}

// 储蓄目标详细信息
data class SavingGoalDetails(
    val goal: SavingGoal,
    val account: Account?,
    val progress: Float,
    val timeProgress: Float,
    val isCompleted: Boolean,
    val isExpired: Boolean
) 