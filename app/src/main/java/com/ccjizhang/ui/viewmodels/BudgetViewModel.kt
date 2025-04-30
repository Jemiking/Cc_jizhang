package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 预算数据ViewModel
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // 所有预算列表
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    // 当前月份的预算
    private val _currentMonthBudgets = MutableStateFlow<List<Budget>>(emptyList())
    val currentMonthBudgets: StateFlow<List<Budget>> = _currentMonthBudgets.asStateFlow()

    // 当前选中的预算
    private val _selectedBudget = MutableStateFlow<Budget?>(null)
    val selectedBudget: StateFlow<Budget?> = _selectedBudget.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 初始化 - 加载预算数据
    init {
        loadBudgets()
    }

    /**
     * 加载预算数据
     */
    fun loadBudgets() {
        viewModelScope.launch {
            _isLoading.value = true

            // 从Repository获取数据
            budgetRepository.getAllBudgets().collectLatest { budgetList ->
                _budgets.value = budgetList

                // 更新当月预算
                updateCurrentMonthBudgets()

                // 加载所有预算的使用情况
                loadAllBudgetsUsage()

                _isLoading.value = false
            }
        }
    }

    /**
     * 更新当月预算列表
     */
    private fun updateCurrentMonthBudgets() {
        viewModelScope.launch {
            budgetRepository.getCurrentBudgets().collectLatest { currentBudgets ->
                _currentMonthBudgets.value = currentBudgets
            }
        }
    }

    /**
     * 通过ID获取预算
     */
    fun getBudgetById(id: Long): Budget? {
        return _budgets.value.find { it.id == id }
    }

    /**
     * 加载预算详情
     */
    fun loadBudgetDetails(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true

            // 从Repository获取预算详情
            budgetRepository.getBudgetById(id).collectLatest { budget ->
                _selectedBudget.value = budget

                // 加载预算使用情况
                loadBudgetUsage(id)

                _isLoading.value = false
            }
        }
    }

    /**
     * 添加新预算
     */
    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            // 调用Repository保存预算
            budgetRepository.addBudget(budget)

            // 重新加载预算列表
            loadBudgets()
        }
    }

    /**
     * 更新预算
     */
    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            // 调用Repository更新预算
            budgetRepository.updateBudget(budget)

            // 重新加载预算列表
            loadBudgets()

            // 如果当前选中的预算被更新，也更新选中状态
            if (_selectedBudget.value?.id == budget.id) {
                _selectedBudget.value = budget
            }
        }
    }

    /**
     * 删除预算
     */
    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            // 调用Repository删除预算
            budgetRepository.deleteBudgetById(id)

            // 重新加载预算列表
            loadBudgets()

            // 如果删除的是当前选中的预算，清除选中状态
            if (_selectedBudget.value?.id == id) {
                _selectedBudget.value = null
            }
        }
    }

    // 预算使用情况缓存
    private val _budgetUsageCache = MutableStateFlow<Map<Long, Pair<Double, Double>>>(emptyMap())
    val budgetUsageCache: StateFlow<Map<Long, Pair<Double, Double>>> = _budgetUsageCache.asStateFlow()

    // 分类预算使用情况缓存
    private val _categoryBudgetUsageCache = MutableStateFlow<Map<Pair<Long, Long>, Pair<Double, Double>>>(emptyMap())

    /**
     * 加载预算使用情况
     */
    fun loadBudgetUsage(budgetId: Long) {
        viewModelScope.launch {
            val usage = budgetRepository.getBudgetUsage(budgetId)
            val currentCache = _budgetUsageCache.value.toMutableMap()
            currentCache[budgetId] = usage
            _budgetUsageCache.value = currentCache
        }
    }

    /**
     * 加载所有预算的使用情况
     */
    fun loadAllBudgetsUsage() {
        viewModelScope.launch {
            val currentBudgets = _budgets.value
            val usageMap = mutableMapOf<Long, Pair<Double, Double>>()

            for (budget in currentBudgets) {
                val usage = budgetRepository.getBudgetUsage(budget.id)
                usageMap[budget.id] = usage
            }

            _budgetUsageCache.value = usageMap
        }
    }

    /**
     * 获取特定类别的预算使用情况
     * 返回：已使用金额/总预算金额
     */
    fun getBudgetUsageForCategory(budgetId: Long, categoryId: Long): Pair<Double, Double> {
        // 先从缓存中查找
        val cacheKey = Pair(budgetId, categoryId)
        val cachedValue = _categoryBudgetUsageCache.value[cacheKey]
        if (cachedValue != null) {
            return cachedValue
        }

        // 如果缓存中没有，则异步加载并返回默认值
        viewModelScope.launch {
            val usage = budgetRepository.getBudgetUsageForCategory(budgetId, categoryId)
            val currentCache = _categoryBudgetUsageCache.value.toMutableMap()
            currentCache[cacheKey] = usage
            _categoryBudgetUsageCache.value = currentCache
        }

        return Pair(0.0, 0.0)
    }

    /**
     * 获取预算已使用金额
     */
    fun getUsedAmount(budgetId: Long): Double {
        // 先从缓存中查找
        val cachedValue = _budgetUsageCache.value[budgetId]
        if (cachedValue != null) {
            return cachedValue.first
        }

        // 如果缓存中没有，则异步加载并返回默认值
        loadBudgetUsage(budgetId)
        return 0.0
    }

    /**
     * 获取预算使用百分比
     */
    fun getBudgetUsagePercentage(budgetId: Long): Float {
        // 先从缓存中查找
        val cachedValue = _budgetUsageCache.value[budgetId]
        if (cachedValue != null) {
            val (used, total) = cachedValue
            return if (total > 0) (used / total).toFloat() else 0f
        }

        // 如果缓存中没有，则异步加载并返回默认值
        loadBudgetUsage(budgetId)
        return 0f
    }

    /**
     * 检查预算是否超支
     */
    fun isBudgetOverspent(budgetId: Long): Boolean {
        // 先从缓存中查找
        val cachedValue = _budgetUsageCache.value[budgetId]
        if (cachedValue != null) {
            val (used, total) = cachedValue
            return used > total
        }

        // 如果缓存中没有，则异步加载并返回默认值
        loadBudgetUsage(budgetId)
        return false
    }
}