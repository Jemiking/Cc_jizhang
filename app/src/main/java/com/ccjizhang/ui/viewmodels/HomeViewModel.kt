package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.BudgetRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 主页数据模型
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyNet: Double = 0.0,
    val lastMonthIncome: Double = 0.0,
    val lastMonthExpense: Double = 0.0,
    val incomeChangePercent: Double = 0.0,
    val expenseChangePercent: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val budgetAlerts: List<BudgetAlert> = emptyList()
)

/**
 * 即将到期的账单
 */
data class UpcomingBill(
    val id: Long,
    val title: String,
    val amount: Double,
    val dueDate: Date,
    val daysLeft: Int,
    val accountId: Long,
    val accountName: String
)

/**
 * 预算警报
 */
data class BudgetAlert(
    val id: Long,
    val name: String,
    val amount: Double,
    val spent: Double,
    val percentage: Double
)

/**
 * 主页ViewModel
 * 负责处理主页所需的数据和业务逻辑
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 初始化
    init {
        loadHomeData()
    }

    /**
     * 加载主页数据
     */
    fun loadHomeData() {
        viewModelScope.launch {
            println("HOME-DEBUG: 开始加载主页数据")
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 并行加载各类数据
                val accountJob = launch { loadAccountBalance() }
                val statsJob = launch { loadMonthlyStats() }
                val transactionsJob = launch { loadRecentTransactions() }
                val billsJob = launch { loadUpcomingBills() }
                val budgetJob = launch { loadBudgetAlerts() }

                // 等待所有任务完成
                accountJob.join()
                statsJob.join()
                transactionsJob.join()
                billsJob.join()
                budgetJob.join()

                println("HOME-DEBUG: 主页数据加载完成")
            } catch (e: Exception) {
                println("HOME-DEBUG: 主页数据加载失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 加载账户总余额
     */
    private suspend fun loadAccountBalance() {
        try {
            println("HOME-DEBUG: 开始加载账户总余额")
            val accounts = accountRepository.getAllAccounts().first()
            println("HOME-DEBUG: 加载到 ${accounts.size} 个账户")

            if (accounts.isEmpty()) {
                println("HOME-DEBUG: 警告 - 账户列表为空!")
            } else {
                accounts.forEachIndexed { index, account ->
                    println("HOME-DEBUG: 账户[$index]: id=${account.id}, name=${account.name}, balance=${account.balance}, includeInTotal=${account.includeInTotal}")
                }
            }

            val totalBalance = accounts.sumOf { it.balance }
            println("HOME-DEBUG: 计算的总余额: $totalBalance")

            _uiState.value = _uiState.value.copy(
                totalBalance = totalBalance,
                isLoading = false
            )
            println("HOME-DEBUG: 账户总余额加载完成")
        } catch (e: Exception) {
            println("HOME-DEBUG: 加载账户总余额失败: ${e.message}")
            e.printStackTrace()
            // 错误处理
        }
    }

    /**
     * 加载月度统计数据
     */
    private suspend fun loadMonthlyStats() {
        try {
            // 获取当月日期范围
            val (currentMonthStart, currentMonthEnd) = getCurrentMonthRange()
            val (lastMonthStart, lastMonthEnd) = getLastMonthRange()

            // 获取当月交易
            val currentMonthTransactions = transactionRepository
                .getTransactionsByDateRange(currentMonthStart, currentMonthEnd)
                .first()

            // 获取上月交易
            val lastMonthTransactions = transactionRepository
                .getTransactionsByDateRange(lastMonthStart, lastMonthEnd)
                .first()

            // 计算当月收支
            val monthlyIncome = currentMonthTransactions
                .filter { it.isIncome }
                .sumOf { it.amount }

            val monthlyExpense = currentMonthTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }

            // 计算上月收支
            val lastMonthIncome = lastMonthTransactions
                .filter { it.isIncome }
                .sumOf { it.amount }

            val lastMonthExpense = lastMonthTransactions
                .filter { !it.isIncome }
                .sumOf { it.amount }

            // 计算环比变化
            val incomeChangePercent = calculateChangePercent(monthlyIncome, lastMonthIncome)
            val expenseChangePercent = calculateChangePercent(monthlyExpense, lastMonthExpense)

            _uiState.value = _uiState.value.copy(
                monthlyIncome = monthlyIncome,
                monthlyExpense = monthlyExpense,
                monthlyNet = monthlyIncome - monthlyExpense,
                lastMonthIncome = lastMonthIncome,
                lastMonthExpense = lastMonthExpense,
                incomeChangePercent = incomeChangePercent,
                expenseChangePercent = expenseChangePercent,
                isLoading = false
            )
        } catch (e: Exception) {
            // 错误处理
        }
    }

    /**
     * 加载最近交易
     */
    private suspend fun loadRecentTransactions() {
        try {
            val recentTransactions = transactionRepository.getRecentTransactions(6).first()
            _uiState.value = _uiState.value.copy(
                recentTransactions = recentTransactions,
                isLoading = false
            )
        } catch (e: Exception) {
            // 错误处理
        }
    }

    /**
     * 加载即将到期的账单
     */
    private suspend fun loadUpcomingBills() {
        try {
            val accounts = accountRepository.getAllAccounts().first()
            val creditCards = accounts.filter { it.type == AccountType.CREDIT_CARD }

            val today = Calendar.getInstance()
            val upcomingBills = mutableListOf<UpcomingBill>()

            for (card in creditCards) {
                if (card.nextDueDate != null) {
                    val dueDate = Calendar.getInstance()
                    dueDate.time = card.nextDueDate!!

                    val daysLeft = ((dueDate.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()

                    // 只显示15天内到期的账单
                    if (daysLeft in 0..15) {
                        upcomingBills.add(
                            UpcomingBill(
                                id = card.id,
                                title = "信用卡还款",
                                amount = card.balance,
                                dueDate = card.nextDueDate!!,
                                daysLeft = daysLeft,
                                accountId = card.id,
                                accountName = card.name
                            )
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                upcomingBills = upcomingBills,
                isLoading = false
            )
        } catch (e: Exception) {
            // 错误处理
        }
    }

    /**
     * 加载预算警报
     */
    private suspend fun loadBudgetAlerts() {
        try {
            val budgets = budgetRepository.getActiveBudgets().first()
            val budgetAlerts = mutableListOf<BudgetAlert>()

            for (budget in budgets) {
                // 使用预算仓库的方法获取预算使用情况
                val (spent, total) = budgetRepository.getBudgetUsage(budget.id)
                val percentage = if (total > 0) (spent / total) * 100 else 0.0

                // 只显示使用超过70%的预算
                if (percentage >= 70) {
                    budgetAlerts.add(
                        BudgetAlert(
                            id = budget.id,
                            name = budget.name,
                            amount = total,
                            spent = spent,
                            percentage = percentage
                        )
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                budgetAlerts = budgetAlerts.sortedByDescending { it.percentage }.take(3),
                isLoading = false
            )
        } catch (e: Exception) {
            // 错误处理
        }
    }

    // 注意：我们现在直接使用BudgetRepository的getBudgetUsage方法，不再需要这个方法

    /**
     * 获取当月日期范围
     */
    private fun getCurrentMonthRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        // 当月第一天
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        // 当月最后一天
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    /**
     * 获取上月日期范围
     */
    private fun getLastMonthRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        // 上月第一天
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        // 上月最后一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time

        return Pair(startDate, endDate)
    }

    /**
     * 计算环比变化百分比
     */
    private fun calculateChangePercent(current: Double, previous: Double): Double {
        if (previous == 0.0) return if (current > 0) 100.0 else 0.0
        return ((current - previous) / previous) * 100
    }

    /**
     * 格式化金额
     */
    fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }

    /**
     * 格式化日期
     */
    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}
