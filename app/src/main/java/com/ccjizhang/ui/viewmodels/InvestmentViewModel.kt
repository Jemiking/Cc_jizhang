package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Investment
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.InvestmentRepository
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
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.catch

/**
 * 投资与理财产品视图模型
 * 负责理财产品跟踪功能的数据处理和业务逻辑
 */
@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    // 所有账户，用于选择与投资关联的账户
    val accounts: Flow<List<Account>> = accountRepository.getAllAccounts()

    // 所有投资记录
    val allInvestments: Flow<List<Investment>> = investmentRepository.getAllInvestments()

    // 活跃的投资记录
    val activeInvestments: Flow<List<Investment>> = investmentRepository.getActiveInvestments()

    // 已完成的投资记录
    val completedInvestments: Flow<List<Investment>> = investmentRepository.getCompletedInvestments()

    // 即将到期的投资记录
    val upcomingMaturityInvestments: Flow<List<Investment>> = investmentRepository.getUpcomingMaturityInvestments()

    // 总投资价值
    val totalInvestmentValue: Flow<Double> = investmentRepository.getTotalInvestmentValue().map { it ?: 0.0 }

    // 总投资收益
    val totalInvestmentReturn: Flow<Double> = investmentRepository.getTotalInvestmentReturn().map { it ?: 0.0 }

    // 当前选中的标签页
    private val _selectedTab = MutableStateFlow(InvestmentTab.ACTIVE)
    val selectedTab: StateFlow<InvestmentTab> = _selectedTab

    // 当前选中的投资ID
    private val _selectedInvestmentId = MutableStateFlow<Long?>(null)
    val selectedInvestmentId: StateFlow<Long?> = _selectedInvestmentId

    // 当前选中的类型过滤
    private val _selectedTypeFilter = MutableStateFlow<Int?>(null)
    val selectedTypeFilter: StateFlow<Int?> = _selectedTypeFilter

    // 当前选中的风险等级过滤
    private val _selectedRiskFilter = MutableStateFlow<Int?>(null)
    val selectedRiskFilter: StateFlow<Int?> = _selectedRiskFilter

    // 搜索查询文本
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Loading state
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Selected investment details state
    private val _selectedInvestmentDetails = MutableStateFlow<Investment?>(null)
    val selectedInvestmentDetails: StateFlow<Investment?> = _selectedInvestmentDetails

    // 根据当前选择的标签页和过滤条件显示相应的投资记录
    val filteredInvestments = combine(
        selectedTab,
        activeInvestments,
        completedInvestments,
        upcomingMaturityInvestments,
        selectedTypeFilter,
        selectedRiskFilter
    ) { params ->
        // 将参数数组转换为命名变量
        val tab = params[0] as InvestmentTab
        val active = params[1] as List<Investment>
        val completed = params[2] as List<Investment>
        val upcoming = params[3] as List<Investment>
        val typeFilter = params[4] as Int?
        val riskFilter = params[5] as Int?
        
        // 根据标签页选择基础数据集
        val baseList: List<Investment> = when (tab) {
            InvestmentTab.ACTIVE -> active
            InvestmentTab.COMPLETED -> completed
            InvestmentTab.UPCOMING -> upcoming
            InvestmentTab.ALL -> active + completed
        }

        // 应用类型过滤
        val typeFiltered = if (typeFilter != null) {
            baseList.filter { investment -> 
                when (typeFilter) {
                    0 -> investment.type == Investment.Type.DEPOSIT
                    1 -> investment.type == Investment.Type.STOCK
                    2 -> investment.type == Investment.Type.FUND
                    3 -> investment.type == Investment.Type.BOND
                    else -> investment.type == Investment.Type.OTHER
                }
            }
        } else {
            baseList
        }

        // 应用风险等级过滤
        val riskFiltered = if (riskFilter != null) {
            typeFiltered.filter { investment -> investment.riskLevel == riskFilter }
        } else {
            typeFiltered
        }

        // 应用搜索过滤
        if (searchQuery.value.isNotBlank()) {
            riskFiltered.filter { investment ->
                investment.name.contains(searchQuery.value, ignoreCase = true) ||
                investment.institution?.contains(searchQuery.value, ignoreCase = true) == true ||
                investment.productCode?.contains(searchQuery.value, ignoreCase = true) == true ||
                investment.note?.contains(searchQuery.value, ignoreCase = true) == true
            }
        } else {
            riskFiltered
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        // No longer updating _uiState here as it's removed
        // filteredInvestments can be collected directly in the UI if needed for list display
    }

    // 选择标签页
    fun selectTab(tab: InvestmentTab) {
        _selectedTab.value = tab
    }

    // 选择投资
    fun selectInvestment(id: Long?) {
        _selectedInvestmentId.value = id
    }

    // 加载并选择单个投资的详细信息
    fun selectInvestmentDetails(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _selectedInvestmentDetails.value = null // Clear previous details
            try {
                val details = investmentRepository.getInvestmentById(id)
                _selectedInvestmentDetails.value = details
            } catch (e: Exception) {
                _errorMessage.value = "加载投资详情失败: ${e.message}"
                // Log the error e.g., using Timber.e(e, "Failed to load investment details")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 设置类型过滤
    fun setTypeFilter(type: Int?) {
        _selectedTypeFilter.value = type
    }

    // 设置风险等级过滤
    fun setRiskFilter(riskLevel: Int?) {
        _selectedRiskFilter.value = riskLevel
    }

    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 添加新的投资记录
    suspend fun addInvestment(
        name: String,
        type: Investment.Type,
        initialAmount: Double,
        currentValue: Double,
        accountId: Long?,
        institution: String?,
        productCode: String?,
        expectedAnnualReturn: Double?,
        riskLevel: Int?,
        startDate: Date,
        endDate: Date?,
        note: String?,
        autoUpdateFrequencyDays: Int?
    ): Long {
        val investment = Investment(
            name = name,
            type = type,
            initialAmount = initialAmount,
            currentValue = currentValue,
            accountId = accountId,
            institution = institution,
            productCode = productCode,
            expectedAnnualReturn = expectedAnnualReturn,
            riskLevel = riskLevel,
            startDate = startDate,
            endDate = endDate,
            note = note,
            autoUpdateFrequencyDays = autoUpdateFrequencyDays
        )
        return investmentRepository.addInvestment(investment)
    }

    // 更新投资记录
    fun updateInvestment(investment: Investment) {
        viewModelScope.launch {
            investmentRepository.updateInvestment(investment)
        }
    }

    // 删除投资记录
    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            investmentRepository.deleteInvestment(investment)
            if (_selectedInvestmentId.value == investment.id) {
                _selectedInvestmentId.value = null
            }
        }
    }

    // 更新投资价值
    fun updateInvestmentValue(id: Long, newValue: Double) {
        viewModelScope.launch {
            investmentRepository.updateInvestmentValue(id, newValue)
        }
    }

    // 更新投资状态
    fun updateInvestmentStatus(id: Long, status: Int) {
        viewModelScope.launch {
            investmentRepository.updateInvestmentStatus(id, status)
        }
    }

    // 计算投资的年化收益率
    fun calculateAnnualReturn(investment: Investment): Double {
        return investmentRepository.calculateAnnualReturn(investment)
    }

    // 获取投资类型名称
    fun getInvestmentTypeName(type: Investment.Type): String {
        return when (type) {
            Investment.Type.DEPOSIT -> "存款"
            Investment.Type.STOCK -> "股票"
            Investment.Type.FUND -> "基金"
            Investment.Type.BOND -> "债券"
            Investment.Type.OTHER -> "其他"
        }
    }

    // 获取风险等级名称
    fun getRiskLevelName(riskLevel: Int?): String {
        return when (riskLevel) {
            0 -> "低风险"
            1 -> "中低风险"
            2 -> "中风险"
            3 -> "中高风险"
            4 -> "高风险"
            null -> "未设置"
            else -> "未知"
        }
    }

    // 获取状态名称
    fun getStatusName(status: Int): String {
        return when (status) {
            0 -> "活跃"
            1 -> "已赎回"
            2 -> "已到期"
            3 -> "已转出"
            else -> "未知"
        }
    }

    // 获取到期日期描述
    fun getMaturityDateDescription(investment: Investment): String {
        val endDate = investment.endDate ?: return "无固定期限"
        
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
        val formattedDate = dateFormat.format(endDate)
        
        val now = Calendar.getInstance()
        val maturityDate = Calendar.getInstance().apply { time = endDate }
        
        // 计算与今天的差值（天数）
        val diffInMillis = maturityDate.timeInMillis - now.timeInMillis
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        return when {
            diffInDays < 0 -> "已过期 ($formattedDate)"
            diffInDays == 0 -> "今天到期"
            diffInDays < 30 -> "还有 $diffInDays 天到期"
            else -> formattedDate
        }
    }

    /**
     * 计算投资的预期收益
     */
    data class ProjectedReturns(
        val oneYear: Double,
        val threeYears: Double,
        val fiveYears: Double
    )

    /**
     * 计算投资的预期收益
     */
    fun calculateProjectedReturns(investment: Investment): ProjectedReturns {
        // 使用 expectedAnnualReturn 代替不存在的 returnRate
        // 如果 expectedAnnualReturn 为空，则默认为 0
        val rate = investment.expectedAnnualReturn?.div(100.0) ?: 0.0
        val currentValue = investment.currentValue
        
        val oneYear = currentValue * (1 + rate)
        val threeYears = currentValue * Math.pow(1 + rate, 3.0)
        val fiveYears = currentValue * Math.pow(1 + rate, 5.0)
        
        return ProjectedReturns(
            oneYear = oneYear,
            threeYears = threeYears,
            fiveYears = fiveYears
        )
    }

    // 获取账户名称
    fun getAccountName(accountId: Long): String {
        var accountName = "未知账户"
        runBlocking { 
            try {
                accountRepository.getAccountById(accountId).collect { account ->
                    if (account != null) {
                        accountName = account.name
                    }
                }
            } catch (e: Exception) {
                // 处理异常
            }
        }
        return accountName
    }

    // 根据ID获取投资记录
    suspend fun getInvestmentById(id: Long): Investment? {
        return investmentRepository.getInvestmentById(id)
    }
}

// 投资标签页
enum class InvestmentTab {
    ACTIVE, COMPLETED, UPCOMING, ALL
} 