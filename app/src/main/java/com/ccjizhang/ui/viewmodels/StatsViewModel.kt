package com.ccjizhang.ui.viewmodels

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.CategoryType
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.absoluteValue

data class TimeRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val label: String
)

data class StatsPeriod(
    val yearMonth: YearMonth,
    val displayName: String
)

data class CategoryStatistics(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int,
    val color: Color
)

data class MonthlyTrend(
    val month: YearMonth,
    val displayLabel: String,
    val amount: Double,
    val isPast: Boolean
)

data class TrendItem(
    val label: String,
    val value: Double,
    val date: LocalDate
)

enum class StatsTab {
    EXPENSE, INCOME, NET
}

enum class TimeFilter {
    DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM
}

data class StatsUiState(
    val isLoading: Boolean = true,
    val selectedTab: StatsTab = StatsTab.EXPENSE,
    val selectedTimeFilter: TimeFilter = TimeFilter.MONTH,
    val currentPeriod: StatsPeriod = StatsPeriod(
        YearMonth.now(),
        YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy年M月"))
    ),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netAmount: Double = 0.0,
    val expenseVsLastPeriod: Double = 0.0,
    val incomeVsLastPeriod: Double = 0.0,
    val netVsLastPeriod: Double = 0.0,
    val categoryStats: List<CategoryStatistics> = emptyList(),
    val monthlyTrends: List<MonthlyTrend> = emptyList(),
    val savingsRate: Double = 0.0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
    }

    init {
        // 初始化时检查数据一致性
        checkDataConsistency()
    }

    private val _selectedTab = MutableStateFlow(StatsTab.EXPENSE)
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.MONTH)
    private val _currentPeriod = MutableStateFlow(
        StatsPeriod(
            YearMonth.now(),
            YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy年M月"))
        )
    )

    private val _timeRange: StateFlow<TimeRange> = _selectedTimeFilter.combine(_currentPeriod) { filter, period ->
        calculateTimeRange(filter, period.yearMonth)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, calculateTimeRange(TimeFilter.MONTH, YearMonth.now()))

    private val allTransactions: Flow<List<Transaction>> = _timeRange.flatMapLatest<TimeRange, List<Transaction>> { range ->
        transactionRepository.getTransactionsByDateRange(range.startDate, range.endDate)
    }

    val uiState: StateFlow<StatsUiState> = combine(
        _selectedTab,
        _selectedTimeFilter,
        _currentPeriod,
        allTransactions,
        _timeRange
    ) { tab, timeFilter, period, transactions, timeRange ->
        // 分类统计处理 - 使用isIncome字段而不是amount的正负值
        val expenseTransactions = transactions.filter { !it.isIncome }
        val incomeTransactions = transactions.filter { it.isIncome }

        val totalExpense = expenseTransactions.sumOf { Math.abs(it.amount) }
        val totalIncome = incomeTransactions.sumOf { Math.abs(it.amount) }
        val netAmount = totalIncome - totalExpense

        // 获取上期数据进行比较
        val previousTimeRange = calculatePreviousTimeRange(timeFilter, period.yearMonth)
        val previousTransactions = runBlocking {
            transactionRepository.getTransactionsByDateRangeSync(
                previousTimeRange.startDate,
                previousTimeRange.endDate
            )
        }
        val previousExpense = previousTransactions.filter { !it.isIncome }.sumOf { Math.abs(it.amount) }
        val previousIncome = previousTransactions.filter { it.isIncome }.sumOf { Math.abs(it.amount) }
        val previousNet = previousIncome - previousExpense

        val expenseVsLastPeriod = calculatePercentageChange(totalExpense, previousExpense)
        val incomeVsLastPeriod = calculatePercentageChange(totalIncome, previousIncome)
        val netVsLastPeriod = calculatePercentageChange(netAmount, previousNet)

        // 分类统计
        val categoryStats = runBlocking {
            when (tab) {
                StatsTab.EXPENSE -> calculateCategoryStats(expenseTransactions, CategoryType.EXPENSE)
                StatsTab.INCOME -> calculateCategoryStats(incomeTransactions, CategoryType.INCOME)
                StatsTab.NET -> emptyList() // 净收支不按分类统计
            }
        }

        // 月度趋势
        val monthlyTrends = calculateMonthlyTrendsBlocking(period.yearMonth, tab)

        // 储蓄率
        val savingsRate = if (totalIncome > 0) (totalIncome - totalExpense) / totalIncome * 100 else 0.0

        StatsUiState(
            isLoading = false,
            selectedTab = tab,
            selectedTimeFilter = timeFilter,
            currentPeriod = period,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netAmount = netAmount,
            expenseVsLastPeriod = expenseVsLastPeriod,
            incomeVsLastPeriod = incomeVsLastPeriod,
            netVsLastPeriod = netVsLastPeriod,
            categoryStats = categoryStats,
            monthlyTrends = monthlyTrends,
            savingsRate = savingsRate
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatsUiState(isLoading = true)
    )

    fun setTab(tab: StatsTab) {
        _selectedTab.value = tab
    }

    fun setTimeFilter(filter: TimeFilter) {
        _selectedTimeFilter.value = filter
    }



    fun navigateToPreviousPeriod() {
        viewModelScope.launch {
            val currentYearMonth = _currentPeriod.value.yearMonth
            val newYearMonth = when (_selectedTimeFilter.value) {
                TimeFilter.DAY -> currentYearMonth.minusDays(1)
                TimeFilter.WEEK -> currentYearMonth.minusWeeks(1)
                TimeFilter.MONTH -> currentYearMonth.minusMonths(1)
                TimeFilter.QUARTER -> currentYearMonth.minusMonths(3)
                TimeFilter.YEAR -> currentYearMonth.minusYears(1)
                TimeFilter.CUSTOM -> currentYearMonth // 自定义不变
            }
            updateCurrentPeriod(newYearMonth)
        }
    }

    fun navigateToNextPeriod() {
        viewModelScope.launch {
            val currentYearMonth = _currentPeriod.value.yearMonth
            // 限制不能超过当前日期
            val now = LocalDate.now()
            val nowYearMonth = YearMonth.from(now)

            val newYearMonth = when (_selectedTimeFilter.value) {
                TimeFilter.DAY -> {
                    val nextDay = currentYearMonth.plusDays(1)
                    if (nextDay.atDay(1).isAfter(now)) currentYearMonth else nextDay
                }
                TimeFilter.WEEK -> {
                    val nextWeek = currentYearMonth.plusWeeks(1)
                    if (nextWeek.atDay(1).isAfter(now)) currentYearMonth else nextWeek
                }
                TimeFilter.MONTH -> {
                    val nextMonth = currentYearMonth.plusMonths(1)
                    if (nextMonth.isAfter(nowYearMonth)) currentYearMonth else nextMonth
                }
                TimeFilter.QUARTER -> {
                    val nextQuarter = currentYearMonth.plusMonths(3)
                    if (nextQuarter.isAfter(nowYearMonth)) currentYearMonth else nextQuarter
                }
                TimeFilter.YEAR -> {
                    val nextYear = currentYearMonth.plusYears(1)
                    if (nextYear.isAfter(nowYearMonth)) currentYearMonth else nextYear
                }
                TimeFilter.CUSTOM -> currentYearMonth // 自定义不变
            }
            updateCurrentPeriod(newYearMonth)
        }
    }

    /**
     * 设置自定义时间范围
     */
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            // 确保结束日期不超过当前日期
            val now = LocalDate.now()
            val validEndDate = if (endDate.isAfter(now)) now else endDate

            // 使用开始日期的年月作为当前周期
            val yearMonth = YearMonth.from(startDate)

            // 更新时间过滤器为自定义
            _selectedTimeFilter.value = TimeFilter.CUSTOM

            // 更新当前周期
            _currentPeriod.value = StatsPeriod(
                yearMonth,
                "${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} 至 ${validEndDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
            )
        }
    }

    private fun YearMonth.minusDays(days: Long): YearMonth {
        return YearMonth.from(this.atDay(1).minusDays(days))
    }

    private fun YearMonth.plusDays(days: Long): YearMonth {
        return YearMonth.from(this.atDay(1).plusDays(days))
    }

    private fun YearMonth.minusWeeks(weeks: Long): YearMonth {
        return YearMonth.from(this.atDay(1).minusWeeks(weeks))
    }

    private fun YearMonth.plusWeeks(weeks: Long): YearMonth {
        return YearMonth.from(this.atDay(1).plusWeeks(weeks))
    }

    private fun updateCurrentPeriod(yearMonth: YearMonth) {
        _currentPeriod.value = StatsPeriod(
            yearMonth,
            formatPeriod(yearMonth, _selectedTimeFilter.value)
        )
    }

    private fun formatPeriod(yearMonth: YearMonth, timeFilter: TimeFilter): String {
        return when (timeFilter) {
            TimeFilter.DAY -> yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
            TimeFilter.WEEK -> "${yearMonth.year}年第${yearMonth.atDay(1).format(DateTimeFormatter.ofPattern("w"))}周"
            TimeFilter.MONTH -> yearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月"))
            TimeFilter.QUARTER -> {
                val quarter = (yearMonth.monthValue - 1) / 3 + 1
                "${yearMonth.year}年Q$quarter"
            }
            TimeFilter.YEAR -> "${yearMonth.year}年"
            TimeFilter.CUSTOM -> "自定义"
        }
    }

    private fun calculateTimeRange(filter: TimeFilter, yearMonth: YearMonth): TimeRange {
        val now = LocalDate.now()

        return when (filter) {
            TimeFilter.DAY -> {
                // 对于日筛选，使用当前月份的具体某一天
                // 如果是当前月，则使用当前日期，否则使用月份的第一天
                val date = if (yearMonth.equals(YearMonth.from(now))) {
                    now
                } else {
                    yearMonth.atDay(1)
                }
                TimeRange(date, date, date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")))
            }
            TimeFilter.WEEK -> {
                // 对于周筛选，使用当前月份的第一天所在的周
                val dayInMonth = if (yearMonth.equals(YearMonth.from(now))) {
                    now
                } else {
                    yearMonth.atDay(1)
                }

                val firstDayOfWeek = dayInMonth.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val lastDayOfWeek = firstDayOfWeek.plusDays(6)

                // 限制结束日期不超过当前日期
                val adjustedLastDay = if (lastDayOfWeek.isAfter(now)) now else lastDayOfWeek

                TimeRange(
                    firstDayOfWeek,
                    adjustedLastDay,
                    "${firstDayOfWeek.format(DateTimeFormatter.ofPattern("MM.dd"))}-${adjustedLastDay.format(DateTimeFormatter.ofPattern("MM.dd"))}"
                )
            }
            TimeFilter.MONTH -> {
                val firstDayOfMonth = yearMonth.atDay(1)
                val lastDayOfMonth = yearMonth.atEndOfMonth()

                // 限制结束日期不超过当前日期
                val adjustedLastDay = if (lastDayOfMonth.isAfter(now)) now else lastDayOfMonth

                TimeRange(
                    firstDayOfMonth,
                    adjustedLastDay,
                    yearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月"))
                )
            }
            TimeFilter.QUARTER -> {
                val quarter = (yearMonth.monthValue - 1) / 3
                val firstMonth = quarter * 3 + 1
                val firstDayOfQuarter = YearMonth.of(yearMonth.year, firstMonth).atDay(1)
                val lastDayOfQuarter = YearMonth.of(yearMonth.year, firstMonth + 2).atEndOfMonth()

                // 限制结束日期不超过当前日期
                val adjustedLastDay = if (lastDayOfQuarter.isAfter(now)) now else lastDayOfQuarter

                TimeRange(
                    firstDayOfQuarter,
                    adjustedLastDay,
                    "${yearMonth.year}年Q${quarter + 1}"
                )
            }
            TimeFilter.YEAR -> {
                val firstDayOfYear = LocalDate.of(yearMonth.year, 1, 1)
                val lastDayOfYear = LocalDate.of(yearMonth.year, 12, 31)

                // 限制结束日期不超过当前日期
                val adjustedLastDay = if (lastDayOfYear.isAfter(now)) now else lastDayOfYear

                TimeRange(
                    firstDayOfYear,
                    adjustedLastDay,
                    "${yearMonth.year}年"
                )
            }
            TimeFilter.CUSTOM -> {
                // 如果当前周期有自定义时间范围的显示名称，则使用它来解析时间范围
                val periodName = _currentPeriod.value.displayName
                if (periodName.contains("至")) {
                    try {
                        val dates = periodName.split("至")
                        val startDateStr = dates[0].trim()
                        val endDateStr = dates[1].trim()

                        val startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val endDate = LocalDate.parse(endDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                        return TimeRange(startDate, endDate, periodName)
                    } catch (e: Exception) {
                        // 解析失败，使用默认的最近30天
                    }
                }

                // 默认为最近30天
                val endDate = now
                val startDate = now.minusDays(29)
                TimeRange(
                    startDate,
                    endDate,
                    "${startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))} 至 ${endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
                )
            }
        }
    }

    private fun calculatePreviousTimeRange(filter: TimeFilter, yearMonth: YearMonth): TimeRange {
        val currentRange = calculateTimeRange(filter, yearMonth)
        val currentLength = java.time.temporal.ChronoUnit.DAYS.between(currentRange.startDate, currentRange.endDate) + 1

        val previousStart = currentRange.startDate.minusDays(currentLength)
        val previousEnd = currentRange.endDate.minusDays(currentLength)

        return TimeRange(previousStart, previousEnd, "上一${filter.name.lowercase()}")
    }

    private fun calculatePercentageChange(current: Double, previous: Double): Double {
        return if (previous == 0.0) {
            if (current == 0.0) 0.0 else 100.0
        } else {
            (current - previous) / previous * 100
        }
    }

    private suspend fun calculateCategoryStats(
        transactions: List<Transaction>,
        categoryType: CategoryType
    ): List<CategoryStatistics> {
        val categoryMap = mutableMapOf<Long, MutableList<Transaction>>()

        // 按分类ID对交易进行分组
        transactions.forEach { transaction ->
            val categoryId = transaction.categoryId ?: 0L
            if (!categoryMap.containsKey(categoryId)) {
                categoryMap[categoryId] = mutableListOf()
            }
            categoryMap[categoryId]?.add(transaction)
        }

        // 获取所有相关分类信息
        val categoryIds = categoryMap.keys.toList()
        val categories = categoryRepository.getCategoriesByIds(categoryIds)

        // 计算总金额 - 使用绝对值确保金额为正数
        val totalAmount = transactions.sumOf { Math.abs(it.amount) }

        // 为每个分类生成统计数据
        return categoryMap.map { (categoryId, categoryTransactions) ->
            val category = categories.find { it.id == categoryId }
            val categoryName = category?.name ?: "未分类"
            val categoryAmount = categoryTransactions.sumOf { Math.abs(it.amount) }
            val percentage = if (totalAmount > 0) (categoryAmount / totalAmount) * 100 else 0.0

            // 为分类分配颜色 (这里简化处理，实际应用中应该有一套颜色分配逻辑)
            val color = getColorForCategory(categoryId, categoryName)

            CategoryStatistics(
                categoryId = categoryId,
                categoryName = categoryName,
                amount = categoryAmount,
                percentage = percentage,
                transactionCount = categoryTransactions.size,
                color = color
            )
        }.sortedByDescending { it.amount }
    }

    private fun getColorForCategory(categoryId: Long, categoryName: String): Color {
        // 根据分类ID或名称返回一个固定的颜色
        // 实际应用中，可能会从数据库读取每个分类的颜色
        val colors = listOf(
            Color(0xFF4e2a84), // 紫色
            Color(0xFF43a047), // 绿色
            Color(0xFFfb8c00), // 橙色
            Color(0xFF5c6bc0), // 蓝色
            Color(0xFFec407a), // 粉色
            Color(0xFF9e9e9e)  // 灰色
        )

        return colors[(categoryId % colors.size).toInt()]
    }

    /**
     * 检查数据一致性，确保amount的正负值与isIncome字段一致
     * 这个函数会在后台检查数据库中的交易记录，并记录发现的不一致情况
     */
    private fun checkDataConsistency() {
        viewModelScope.launch {
            try {
                // 获取所有交易记录
                val allTransactions = transactionRepository.getAllTransactionsSync()

                // 检查不一致的记录
                val inconsistentTransactions = allTransactions.filter { transaction ->
                    // 如果是收入但金额为负，或者是支出但金额为正，则认为是不一致的
                    (transaction.isIncome && transaction.amount < 0) ||
                    (!transaction.isIncome && transaction.amount > 0)
                }

                // 记录不一致的情况
                if (inconsistentTransactions.isNotEmpty()) {
                    Log.w(TAG, "发现 ${inconsistentTransactions.size} 条不一致的交易记录")
                    inconsistentTransactions.forEach { transaction ->
                        Log.w(TAG, "交易ID: ${transaction.id}, 金额: ${transaction.amount}, 是否收入: ${transaction.isIncome}")
                    }

                    // 这里可以添加修复逻辑，但需要谨慎处理
                    // 例如：
                    // inconsistentTransactions.forEach { transaction ->
                    //     val correctedTransaction = transaction.copy(
                    //         amount = if (transaction.isIncome) Math.abs(transaction.amount) else -Math.abs(transaction.amount)
                    //     )
                    //     transactionRepository.update(correctedTransaction)
                    // }
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查数据一致性时出错: ${e.message}", e)
            }
        }
    }

    // 使用runBlocking处理挂起函数调用
    private fun calculateMonthlyTrendsBlocking(currentYearMonth: YearMonth, tab: StatsTab): List<MonthlyTrend> {
        val currentMonth = currentYearMonth.monthValue
        val currentYear = currentYearMonth.year

        return (1..12).map { month ->
            val yearMonth = YearMonth.of(currentYear, month)
            val monthlyTransactions = runBlocking {
                try {
                    transactionRepository.getTransactionsByMonthSync(yearMonth)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val amount = when (tab) {
                StatsTab.EXPENSE -> monthlyTransactions.filter { !it.isIncome }.sumOf { Math.abs(it.amount) }
                StatsTab.INCOME -> monthlyTransactions.filter { it.isIncome }.sumOf { Math.abs(it.amount) }
                StatsTab.NET -> {
                    // 对于净收支，我们需要确保正确计算收入和支出
                    // 收入总是正值，支出总是负值
                    val income = monthlyTransactions.filter { it.isIncome }.sumOf { Math.abs(it.amount) }
                    val expense = monthlyTransactions.filter { !it.isIncome }.sumOf { Math.abs(it.amount) }
                    // 净收支 = 收入 - 支出，可能为负值
                    income - expense
                }
            }

            MonthlyTrend(
                month = yearMonth,
                displayLabel = "${month}月",
                amount = amount,
                isPast = month <= currentMonth
            )
        }
    }
}