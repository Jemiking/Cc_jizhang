package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.ccjizhang.data.model.CategoryType
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 统计详情页面的ViewModel
 */
@HiltViewModel
class StatisticsDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 趋势项数据类
    data class TrendItem(
        val label: String,
        val value: Double,
        val date: LocalDate
    )

    // UI状态
    data class StatisticsDetailUiState(
        val isLoading: Boolean = true,
        val title: String = "",
        val totalAmount: Double = 0.0,
        val timeRange: String = "本月",
        val tabType: StatsTab = StatsTab.EXPENSE,
        val isCategoryDetail: Boolean = false,
        val categoryId: Long = 0L,
        val categoryName: String = "",
        val transactions: List<Transaction> = emptyList(),
        val categoryStats: List<CategoryStatistics> = emptyList(),
        val trends: List<TrendItem> = emptyList(),
        val showExportSuccess: Boolean = false,
        val exportPath: String = ""
    )

    private val _uiState = MutableStateFlow(StatisticsDetailUiState())
    val uiState: StateFlow<StatisticsDetailUiState> = _uiState.asStateFlow()

    /**
     * 加载统计详情数据
     * @param type 统计类型，可以是"expense"、"income"、"net"或者"category_123"(分类ID)
     */
    fun loadStatisticsDetail(type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                when {
                    type == "expense" -> {
                        loadExpenseDetail()
                    }
                    type == "income" -> {
                        loadIncomeDetail()
                    }
                    type == "net" -> {
                        loadNetDetail()
                    }
                    type.startsWith("category_") -> {
                        val categoryId = type.substringAfter("category_").toLongOrNull() ?: 0L
                        loadCategoryDetail(categoryId)
                    }
                    else -> {
                        // 默认加载支出详情
                        loadExpenseDetail()
                    }
                }
            } catch (e: Exception) {
                // 处理异常
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = "加载失败",
                        transactions = emptyList()
                    )
                }
            }
        }
    }

    /**
     * 更新时间范围
     */
    fun updateTimeRange(timeRange: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(timeRange = timeRange, isLoading = true) }

            // 根据当前的详情类型重新加载数据
            val currentState = _uiState.value
            if (currentState.isCategoryDetail) {
                loadCategoryDetail(currentState.categoryId)
            } else {
                when (currentState.tabType) {
                    StatsTab.EXPENSE -> loadExpenseDetail()
                    StatsTab.INCOME -> loadIncomeDetail()
                    StatsTab.NET -> loadNetDetail()
                }
            }
        }
    }

    /**
     * 导出数据
     */
    fun exportData() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val fileName = generateExportFileName(currentState)
                val file = createExportFile(fileName)

                // 写入CSV数据
                FileWriter(file).use { writer ->
                    // 写入标题行
                    writer.append("日期,金额,分类,账户,备注\n")

                    // 写入交易数据
                    currentState.transactions.forEach { transaction ->
                        val date = transaction.date.toString()
                        val amount = transaction.amount.toString()
                        val category = "未分类" // 需要从categoryRepository获取
                        val account = "未知账户" // 需要从accountRepository获取
                        val note = transaction.note

                        writer.append("$date,$amount,$category,$account,$note\n")
                    }
                }

                // 更新UI状态，显示导出成功
                _uiState.update {
                    it.copy(
                        showExportSuccess = true,
                        exportPath = file.absolutePath
                    )
                }
            } catch (e: Exception) {
                // 处理导出失败
                _uiState.update {
                    it.copy(
                        showExportSuccess = false
                    )
                }
            }
        }
    }

    /**
     * 重置导出状态
     */
    fun resetExportState() {
        _uiState.update { it.copy(showExportSuccess = false) }
    }

    /**
     * 加载支出详情
     */
    private suspend fun loadExpenseDetail() {
        val dateRange = getDateRangeFromTimeRange(_uiState.value.timeRange)
        val transactions = transactionRepository.getTransactionsByDateRangeSync(
            dateRange.first, dateRange.second
        ).filter { it.amount < 0 }

        val totalExpense = transactions.sumOf { -it.amount }
        val categoryStats = calculateCategoryStats(transactions, CategoryType.EXPENSE)
        val trends = calculateTrends(dateRange.first, dateRange.second, StatsTab.EXPENSE)

        _uiState.update {
            it.copy(
                isLoading = false,
                title = "支出详情",
                totalAmount = totalExpense,
                tabType = StatsTab.EXPENSE,
                isCategoryDetail = false,
                transactions = transactions,
                categoryStats = categoryStats,
                trends = trends
            )
        }
    }

    /**
     * 加载收入详情
     */
    private suspend fun loadIncomeDetail() {
        val dateRange = getDateRangeFromTimeRange(_uiState.value.timeRange)
        val transactions = transactionRepository.getTransactionsByDateRangeSync(
            dateRange.first, dateRange.second
        ).filter { it.amount > 0 }

        val totalIncome = transactions.sumOf { it.amount }
        val categoryStats = calculateCategoryStats(transactions, CategoryType.INCOME)
        val trends = calculateTrends(dateRange.first, dateRange.second, StatsTab.INCOME)

        _uiState.update {
            it.copy(
                isLoading = false,
                title = "收入详情",
                totalAmount = totalIncome,
                tabType = StatsTab.INCOME,
                isCategoryDetail = false,
                transactions = transactions,
                categoryStats = categoryStats,
                trends = trends
            )
        }
    }

    /**
     * 加载净收支详情
     */
    private suspend fun loadNetDetail() {
        val dateRange = getDateRangeFromTimeRange(_uiState.value.timeRange)
        val transactions = transactionRepository.getTransactionsByDateRangeSync(
            dateRange.first, dateRange.second
        )

        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val netAmount = totalIncome - totalExpense
        val trends = calculateTrends(dateRange.first, dateRange.second, StatsTab.NET)

        _uiState.update {
            it.copy(
                isLoading = false,
                title = "净收支详情",
                totalAmount = netAmount,
                tabType = StatsTab.NET,
                isCategoryDetail = false,
                transactions = transactions,
                categoryStats = emptyList(),
                trends = trends
            )
        }
    }

    /**
     * 加载分类详情
     */
    private suspend fun loadCategoryDetail(categoryId: Long) {
        val dateRange = getDateRangeFromTimeRange(_uiState.value.timeRange)
        val transactions = transactionRepository.getTransactionsByDateRangeSync(
            dateRange.first, dateRange.second
        ).filter { it.categoryId == categoryId }

        val category = categoryRepository.getCategoryById(categoryId)
        val categoryName = "未知分类" // 如果无法获取分类名称，使用默认值
        val tabType = StatsTab.EXPENSE // 默认使用支出标签

        val totalAmount = if (tabType == StatsTab.EXPENSE) {
            transactions.sumOf { -it.amount }
        } else {
            transactions.sumOf { it.amount }
        }

        val trends = calculateTrendsForCategory(dateRange.first, dateRange.second, categoryId, tabType)

        _uiState.update {
            it.copy(
                isLoading = false,
                title = "$categoryName 详情",
                totalAmount = totalAmount,
                tabType = tabType,
                isCategoryDetail = true,
                categoryId = categoryId,
                categoryName = categoryName,
                transactions = transactions,
                categoryStats = emptyList(),
                trends = trends
            )
        }
    }

    /**
     * 根据时间范围字符串获取日期范围
     */
    private fun getDateRangeFromTimeRange(timeRange: String): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()

        return when (timeRange) {
            "本日" -> Pair(today, today)
            "本周" -> {
                val dayOfWeek = today.dayOfWeek.value
                val startOfWeek = today.minusDays(dayOfWeek - 1L)
                val endOfWeek = startOfWeek.plusDays(6)
                Pair(startOfWeek, endOfWeek)
            }
            "本月" -> {
                val yearMonth = YearMonth.from(today)
                Pair(yearMonth.atDay(1), yearMonth.atEndOfMonth())
            }
            "本季度" -> {
                val month = today.monthValue
                val quarterStartMonth = when {
                    month <= 3 -> 1
                    month <= 6 -> 4
                    month <= 9 -> 7
                    else -> 10
                }
                val startOfQuarter = LocalDate.of(today.year, quarterStartMonth, 1)
                val endOfQuarter = startOfQuarter.plusMonths(2).withDayOfMonth(
                    startOfQuarter.plusMonths(2).lengthOfMonth()
                )
                Pair(startOfQuarter, endOfQuarter)
            }
            "本年" -> {
                val startOfYear = LocalDate.of(today.year, 1, 1)
                val endOfYear = LocalDate.of(today.year, 12, 31)
                Pair(startOfYear, endOfYear)
            }
            "自定义" -> {
                // 默认显示最近30天
                Pair(today.minusDays(29), today)
            }
            else -> {
                // 默认显示本月
                val yearMonth = YearMonth.from(today)
                Pair(yearMonth.atDay(1), yearMonth.atEndOfMonth())
            }
        }
    }

    /**
     * 计算分类统计数据
     */
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

        // 计算总金额
        val totalAmount = if (categoryType == CategoryType.EXPENSE) {
            transactions.sumOf { -it.amount }
        } else {
            transactions.sumOf { it.amount }
        }

        // 为每个分类生成统计数据
        return categoryMap.map { (categoryId, categoryTransactions) ->
            val category = categories.find { it.id == categoryId }
            val categoryName = category?.name ?: "未分类"
            val categoryAmount = if (categoryType == CategoryType.EXPENSE) {
                categoryTransactions.sumOf { -it.amount }
            } else {
                categoryTransactions.sumOf { it.amount }
            }
            val percentage = if (totalAmount > 0) (categoryAmount / totalAmount) * 100 else 0.0

            // 为分类分配颜色
            val color = getColorForCategory(categoryId)

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

    /**
     * 计算趋势数据
     */
    private suspend fun calculateTrends(
        startDate: LocalDate,
        endDate: LocalDate,
        tabType: StatsTab
    ): List<TrendItem> {
        val transactions = transactionRepository.getTransactionsByDateRangeSync(startDate, endDate)
        val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

        // 根据日期范围的长度决定趋势数据的粒度
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1

        return if (daysBetween <= 31) {
            // 日粒度
            (0 until daysBetween).map { dayOffset ->
                val date = startDate.plusDays(dayOffset)
                val dateTransactions = transactions.filter {
                    it.date.time == date.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                }

                val value = when (tabType) {
                    StatsTab.EXPENSE -> dateTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                    StatsTab.INCOME -> dateTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    StatsTab.NET -> {
                        val income = dateTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                        val expense = dateTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                        income - expense
                    }
                }

                TrendItem(
                    label = date.format(dateFormatter),
                    value = value,
                    date = date
                )
            }
        } else if (daysBetween <= 90) {
            // 周粒度
            val weekCount = (daysBetween / 7).toInt() + 1
            (0 until weekCount).map { weekOffset ->
                val weekStart = startDate.plusWeeks(weekOffset.toLong())
                val weekEnd = minOf(weekStart.plusDays(6), endDate)
                val weekTransactions = transactions.filter {
                    val transactionDate = java.time.Instant.ofEpochMilli(it.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    transactionDate >= weekStart && transactionDate <= weekEnd
                }

                val value = when (tabType) {
                    StatsTab.EXPENSE -> weekTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                    StatsTab.INCOME -> weekTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    StatsTab.NET -> {
                        val income = weekTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                        val expense = weekTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                        income - expense
                    }
                }

                TrendItem(
                    label = "${weekStart.format(dateFormatter)}~${weekEnd.format(dateFormatter)}",
                    value = value,
                    date = weekStart
                )
            }
        } else {
            // 月粒度
            val startYearMonth = YearMonth.from(startDate)
            val endYearMonth = YearMonth.from(endDate)
            val monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(startYearMonth, endYearMonth) + 1

            (0 until monthsBetween).map { monthOffset ->
                val yearMonth = startYearMonth.plusMonths(monthOffset)
                val monthStart = yearMonth.atDay(1)
                val monthEnd = yearMonth.atEndOfMonth()
                val monthTransactions = transactions.filter {
                    val transactionDate = java.time.Instant.ofEpochMilli(it.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    transactionDate >= monthStart && transactionDate <= monthEnd
                }

                val value = when (tabType) {
                    StatsTab.EXPENSE -> monthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                    StatsTab.INCOME -> monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    StatsTab.NET -> {
                        val income = monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                        val expense = monthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                        income - expense
                    }
                }

                TrendItem(
                    label = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    value = value,
                    date = monthStart
                )
            }
        }
    }

    /**
     * 根据分类ID获取颜色
     */
    private fun getColorForCategory(categoryId: Long): Color {
        // 根据分类ID返回一个固定的颜色
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
     * 计算特定分类的趋势数据
     */
    private suspend fun calculateTrendsForCategory(
        startDate: LocalDate,
        endDate: LocalDate,
        categoryId: Long,
        tabType: StatsTab
    ): List<TrendItem> {
        val transactions = transactionRepository.getTransactionsByDateRangeSync(startDate, endDate)
            .filter { it.categoryId == categoryId }
        val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

        // 根据日期范围的长度决定趋势数据的粒度
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1

        return if (daysBetween <= 31) {
            // 日粒度
            (0 until daysBetween).map { dayOffset ->
                val date = startDate.plusDays(dayOffset)
                val dateTransactions = transactions.filter {
                    val transactionDate = java.time.Instant.ofEpochMilli(it.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    transactionDate == date
                }

                val value = if (tabType == StatsTab.EXPENSE) {
                    dateTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                } else {
                    dateTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                }

                TrendItem(
                    label = date.format(dateFormatter),
                    value = value,
                    date = date
                )
            }
        } else if (daysBetween <= 90) {
            // 周粒度
            val weekCount = (daysBetween / 7).toInt() + 1
            (0 until weekCount).map { weekOffset ->
                val weekStart = startDate.plusWeeks(weekOffset.toLong())
                val weekEnd = minOf(weekStart.plusDays(6), endDate)
                val weekTransactions = transactions.filter {
                    val transactionDate = java.time.Instant.ofEpochMilli(it.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    transactionDate >= weekStart && transactionDate <= weekEnd
                }

                val value = if (tabType == StatsTab.EXPENSE) {
                    weekTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                } else {
                    weekTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                }

                TrendItem(
                    label = "${weekStart.format(dateFormatter)}~${weekEnd.format(dateFormatter)}",
                    value = value,
                    date = weekStart
                )
            }
        } else {
            // 月粒度
            val startYearMonth = YearMonth.from(startDate)
            val endYearMonth = YearMonth.from(endDate)
            val monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(startYearMonth, endYearMonth) + 1

            (0 until monthsBetween).map { monthOffset ->
                val yearMonth = startYearMonth.plusMonths(monthOffset)
                val monthStart = yearMonth.atDay(1)
                val monthEnd = yearMonth.atEndOfMonth()
                val monthTransactions = transactions.filter {
                    val transactionDate = java.time.Instant.ofEpochMilli(it.date.time)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    transactionDate >= monthStart && transactionDate <= monthEnd
                }

                val value = if (tabType == StatsTab.EXPENSE) {
                    monthTransactions.filter { it.amount < 0 }.sumOf { -it.amount }
                } else {
                    monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                }

                TrendItem(
                    label = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    value = value,
                    date = monthStart
                )
            }
        }
    }

    /**
     * 生成导出文件名
     */
    private fun generateExportFileName(state: StatisticsDetailUiState): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val prefix = when {
            state.isCategoryDetail -> "分类_${state.categoryName}"
            state.tabType == StatsTab.EXPENSE -> "支出"
            state.tabType == StatsTab.INCOME -> "收入"
            state.tabType == StatsTab.NET -> "净收支"
            else -> "统计"
        }

        return "${prefix}_${state.timeRange}_${timestamp}.csv"
    }

    /**
     * 创建导出文件
     */
    private fun createExportFile(fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val ccjizhangDir = File(downloadsDir, "CCJiZhang")

        if (!ccjizhangDir.exists()) {
            ccjizhangDir.mkdirs()
        }

        val file = File(ccjizhangDir, fileName)
        return file
    }
}
