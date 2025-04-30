package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.FinancialReport
import com.ccjizhang.data.model.Period
import com.ccjizhang.data.repository.FinancialReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import com.ccjizhang.ui.common.OperationResult

/**
 * UI状态类
 */
data class FinancialReportUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reports: List<FinancialReport> = emptyList()
)

/**
 * 财务报告视图模型
 * 负责财务报告生成功能的数据处理和业务逻辑
 */
@HiltViewModel
class FinancialReportViewModel @Inject constructor(
    private val financialReportRepository: FinancialReportRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(FinancialReportUiState())
    val uiState: StateFlow<FinancialReportUiState> = _uiState

    // 所有财务报告
    val allFinancialReports: Flow<List<FinancialReport>> = financialReportRepository.getAllFinancialReports()

    // 按类型筛选的报告
    val monthlyReports: Flow<List<FinancialReport>> = financialReportRepository.getFinancialReportsByType(0)
    val quarterlyReports: Flow<List<FinancialReport>> = financialReportRepository.getFinancialReportsByType(1)
    val yearlyReports: Flow<List<FinancialReport>> = financialReportRepository.getFinancialReportsByType(2)
    val customReports: Flow<List<FinancialReport>> = financialReportRepository.getFinancialReportsByType(3)

    // 最近生成的报告
    val recentReports: Flow<List<FinancialReport>> = financialReportRepository.getRecentFinancialReports(5)

    // 当前选中的标签页
    private val _selectedTab = MutableStateFlow(FinancialReportTab.ALL)
    val selectedTab: StateFlow<FinancialReportTab> = _selectedTab

    // 当前选中的报告ID
    private val _selectedReportId = MutableStateFlow<Long?>(null)
    val selectedReportId: StateFlow<Long?> = _selectedReportId

    // 搜索查询文本
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 搜索结果
    val searchResults: Flow<List<FinancialReport>> = _searchQuery.map { query ->
        if (query.isBlank()) emptyList()
        else financialReportRepository.searchFinancialReports(query).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        ).value
    }

    // 根据当前选择的标签页显示相应的财务报告
    val currentTabReports = combine(
        selectedTab,
        allFinancialReports,
        monthlyReports,
        quarterlyReports,
        yearlyReports,
        customReports,
        recentReports,
        searchResults
    ) { args ->
        val tab = args[0] as FinancialReportTab
        val all = args[1] as List<FinancialReport>
        val monthly = args[2] as List<FinancialReport>
        val quarterly = args[3] as List<FinancialReport>
        val yearly = args[4] as List<FinancialReport>
        val custom = args[5] as List<FinancialReport>
        val recent = args[6] as List<FinancialReport>
        val search = args[7] as List<FinancialReport>
        
        when {
            searchQuery.value.isNotBlank() -> search
            tab == FinancialReportTab.ALL -> all
            tab == FinancialReportTab.MONTHLY -> monthly
            tab == FinancialReportTab.QUARTERLY -> quarterly
            tab == FinancialReportTab.YEARLY -> yearly
            tab == FinancialReportTab.CUSTOM -> custom
            tab == FinancialReportTab.RECENT -> recent
            else -> all
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 当前年份筛选
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    // 当前月份筛选（1-12）
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    // 当前季度筛选（1-4）
    private val _selectedQuarter = MutableStateFlow((Calendar.getInstance().get(Calendar.MONTH) / 3) + 1)
    val selectedQuarter: StateFlow<Int> = _selectedQuarter

    // 生成报告的状态
    private val _generatingState = MutableStateFlow<GeneratingState>(GeneratingState.Idle)
    val generatingState: StateFlow<GeneratingState> = _generatingState

    // 选择的开始日期和结束日期（自定义报告）
    private val _customStartDate = MutableStateFlow<Date?>(null)
    val customStartDate: StateFlow<Date?> = _customStartDate

    private val _customEndDate = MutableStateFlow<Date?>(null)
    val customEndDate: StateFlow<Date?> = _customEndDate

    // 选择标签页
    fun selectTab(tab: FinancialReportTab) {
        _selectedTab.value = tab
    }

    // 选择报告
    fun selectReport(id: Long?) {
        _selectedReportId.value = id
    }

    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 设置年份筛选
    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    // 设置月份筛选
    fun setSelectedMonth(month: Int) {
        _selectedMonth.value = month
    }

    // 设置季度筛选
    fun setSelectedQuarter(quarter: Int) {
        _selectedQuarter.value = quarter
    }

    // 设置自定义日期范围
    fun setCustomDateRange(startDate: Date?, endDate: Date?) {
        _customStartDate.value = startDate
        _customEndDate.value = endDate
    }

    // 生成月度报告
    fun generateMonthlyReport() {
        viewModelScope.launch {
            try {
                _generatingState.value = GeneratingState.Generating
                val reportId = financialReportRepository.generateMonthlyReport(
                    selectedYear.value,
                    selectedMonth.value
                )
                _generatingState.value = GeneratingState.Success(reportId)
                selectReport(reportId)
            } catch (e: Exception) {
                _generatingState.value = GeneratingState.Error(e.message ?: "生成报告失败")
                _uiState.value = _uiState.value.copy(errorMessage = "生成报告失败: ${e.message}")
            }
        }
    }

    // 生成季度报告
    fun generateQuarterlyReport() {
        viewModelScope.launch {
            try {
                _generatingState.value = GeneratingState.Generating
                val reportId = financialReportRepository.generateQuarterlyReport(
                    selectedYear.value,
                    selectedQuarter.value
                )
                _generatingState.value = GeneratingState.Success(reportId)
                selectReport(reportId)
            } catch (e: Exception) {
                _generatingState.value = GeneratingState.Error(e.message ?: "生成报告失败")
                _uiState.value = _uiState.value.copy(errorMessage = "生成报告失败: ${e.message}")
            }
        }
    }

    // 生成年度报告
    fun generateYearlyReport() {
        viewModelScope.launch {
            try {
                _generatingState.value = GeneratingState.Generating
                val reportId = financialReportRepository.generateYearlyReport(
                    selectedYear.value
                )
                _generatingState.value = GeneratingState.Success(reportId)
                selectReport(reportId)
            } catch (e: Exception) {
                _generatingState.value = GeneratingState.Error(e.message ?: "生成报告失败")
                _uiState.value = _uiState.value.copy(errorMessage = "生成报告失败: ${e.message}")
            }
        }
    }

    // 生成自定义报告
    fun generateCustomReport(title: String) {
        viewModelScope.launch {
            try {
                val startDate = customStartDate.value
                val endDate = customEndDate.value

                if (startDate == null || endDate == null) {
                    _generatingState.value = GeneratingState.Error("请选择开始和结束日期")
                    _uiState.value = _uiState.value.copy(errorMessage = "请选择开始和结束日期")
                    return@launch
                }

                if (startDate.after(endDate)) {
                    _generatingState.value = GeneratingState.Error("开始日期不能晚于结束日期")
                    _uiState.value = _uiState.value.copy(errorMessage = "开始日期不能晚于结束日期")
                    return@launch
                }

                _generatingState.value = GeneratingState.Generating
                val reportId = financialReportRepository.generateCustomReport(
                    title,
                    startDate,
                    endDate
                )
                _generatingState.value = GeneratingState.Success(reportId)
                selectReport(reportId)
            } catch (e: Exception) {
                _generatingState.value = GeneratingState.Error(e.message ?: "生成报告失败")
                _uiState.value = _uiState.value.copy(errorMessage = "生成报告失败: ${e.message}")
            }
        }
    }

    // 删除报告
    fun deleteReport(report: FinancialReport) {
        viewModelScope.launch {
            financialReportRepository.deleteFinancialReport(report)
            if (_selectedReportId.value == report.id) {
                _selectedReportId.value = null
            }
        }
    }

    // 更新报告状态
    fun updateReportStatus(id: Long, status: Int) {
        viewModelScope.launch {
            financialReportRepository.updateStatus(id, status)
        }
    }

    // 更新PDF文件URI
    fun updatePdfUri(id: Long, pdfUri: String?) {
        viewModelScope.launch {
            financialReportRepository.updatePdfUri(id, pdfUri)
        }
    }

    // 更新分享链接
    fun updateShareUrl(id: Long, shareUrl: String?) {
        viewModelScope.launch {
            financialReportRepository.updateShareUrl(id, shareUrl)
        }
    }

    // 获取报告类型名称
    fun getReportTypeName(type: Int): String {
        return when (type) {
            0 -> "月度报告"
            1 -> "季度报告"
            2 -> "年度报告"
            3 -> "自定义报告"
            else -> "未知类型"
        }
    }

    // 获取状态名称
    fun getStatusName(status: Int): String {
        return when (status) {
            0 -> "草稿"
            1 -> "已完成"
            2 -> "已分享"
            else -> "未知"
        }
    }

    // 重置生成状态
    fun resetGeneratingState() {
        _generatingState.value = GeneratingState.Idle
    }

    // 生成报告
    fun generateReport(
        title: String,
        description: String,
        period: Period,
        startDate: LocalDate,
        endDate: LocalDate,
        includeIncomeAnalysis: Boolean,
        includeExpenseAnalysis: Boolean,
        includeCategoryBreakdown: Boolean,
        includeAccountBalances: Boolean,
        includeBudgetComparison: Boolean,
        includeFinancialHealth: Boolean,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 将LocalDate转换为Date
                val startDateAsDate = Date(startDate.toEpochDay() * 24 * 60 * 60 * 1000)
                val endDateAsDate = Date(endDate.toEpochDay() * 24 * 60 * 60 * 1000)
                
                // 根据period获取对应的类型值
                val typeValue = when(period) {
                    Period.MONTHLY -> 0
                    Period.QUARTERLY -> 1
                    Period.YEARLY -> 2
                    Period.CUSTOM -> 3
                }
                
                // 使用Repository创建报告
                val reportId = financialReportRepository.generateCustomReport(
                    title = title,
                    startDate = startDateAsDate,
                    endDate = endDateAsDate
                )
                
                // 更新报告的其他属性
                financialReportRepository.updateReport(
                    id = reportId,
                    includeIncomeAnalysis = includeIncomeAnalysis,
                    includeExpenseAnalysis = includeExpenseAnalysis,
                    includeCategoryBreakdown = includeCategoryBreakdown,
                    includeAccountBalances = includeAccountBalances,
                    includeBudgetComparison = includeBudgetComparison,
                    includeFinancialHealth = includeFinancialHealth,
                    description = description,
                    type = typeValue
                )
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(reportId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "生成报告失败: ${e.message}"
                )
                onError(e.message ?: "生成报告失败")
            }
        }
    }

    // 重置UI状态
    fun resetUiState() {
        _uiState.value = FinancialReportUiState()
    }

    // 根据 ID 获取单个财务报告的 Flow
    fun getReportByIdFlow(reportId: Long): Flow<FinancialReport?> {
        // 假设 repository 提供了 getFinancialReportById(id: Long): Flow<FinancialReport?>
        // 如果没有，可能需要从 allFinancialReports 过滤
        // return financialReportRepository.getFinancialReportById(reportId)
        // 替代方案：从 allFinancialReports 中查找
        return allFinancialReports.map { reports ->
            reports.find { it.id == reportId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    // 更新UI状态中的报告列表
    init {
        viewModelScope.launch {
            currentTabReports.collect { reports ->
                _uiState.value = _uiState.value.copy(reports = reports)
            }
        }
    }
}

// 财务报告标签页
enum class FinancialReportTab {
    ALL, MONTHLY, QUARTERLY, YEARLY, CUSTOM, RECENT
}

// 报告生成状态
sealed class GeneratingState {
    object Idle : GeneratingState()
    object Generating : GeneratingState()
    data class Success(val reportId: Long) : GeneratingState()
    data class Error(val message: String) : GeneratingState()
} 