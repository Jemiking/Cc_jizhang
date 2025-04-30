package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Currency
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.service.CurrencyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 多币种报表ViewModel
 * 负责提供多币种账户的统计和分析数据
 */
@HiltViewModel
class MultiCurrencyReportViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService
) : ViewModel() {

    // 所有账户列表
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    
    // 按币种分组的账户数据
    private val _currencyGroups = MutableStateFlow<Map<Currency, List<Account>>>(emptyMap())
    val currencyGroups: StateFlow<Map<Currency, List<Account>>> = _currencyGroups.asStateFlow()
    
    // 按币种统计的总金额
    private val _currencyTotals = MutableStateFlow<Map<Currency, Double>>(emptyMap())
    val currencyTotals: StateFlow<Map<Currency, Double>> = _currencyTotals.asStateFlow()
    
    // 基准币种的总资产
    private val _baseCurrencyTotal = MutableStateFlow(0.0)
    val baseCurrencyTotal: StateFlow<Double> = _baseCurrencyTotal.asStateFlow()
    
    // 基准币种
    val baseCurrency = currencyService.baseCurrency
    
    // 币种汇率更新状态
    val isUpdatingRates = currencyService.isUpdating
    val lastUpdateTime = currencyService.lastUpdateTime
    val updateResult = currencyService.updateResult
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAccounts()
    }
    
    /**
     * 加载账户数据
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.getAllAccounts().collect { accountList ->
                    _accounts.value = accountList.filter { it.includeInTotal }
                    
                    // 按币种分组并计算每种币种的总额
                    val groups = accountList.filter { it.includeInTotal }
                        .groupBy { it.currency }
                    _currencyGroups.value = groups
                    
                    // 计算每种币种的总额
                    val totals = groups.mapValues { (_, accounts) ->
                        accounts.sumOf { it.balance }
                    }
                    _currencyTotals.value = totals
                    
                    // 计算基准币种总额
                    val currentBaseCurrency = baseCurrency.value
                    val totalInBaseCurrency = accountList
                        .filter { it.includeInTotal }
                        .sumOf { account ->
                            currencyService.convert(
                                amount = account.balance,
                                fromCurrency = account.currency,
                                toCurrency = currentBaseCurrency
                            )
                        }
                    _baseCurrencyTotal.value = totalInBaseCurrency
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新汇率
     */
    fun updateExchangeRates() {
        viewModelScope.launch {
            try {
                currencyService.updateExchangeRates()
                // 更新后重新加载账户数据以反映新汇率
                loadAccounts()
            } catch (e: Exception) {
                // 错误处理在CurrencyService中已完成
            }
        }
    }
    
    /**
     * 设置基准币种
     */
    fun setBaseCurrency(currency: Currency) {
        currencyService.setBaseCurrency(currency)
        // 重新计算基准币种总额
        updateBaseCurrencyTotal()
    }
    
    /**
     * 更新基准币种总额
     */
    private fun updateBaseCurrencyTotal() {
        viewModelScope.launch {
            val accounts = _accounts.value
            val currentBaseCurrency = baseCurrency.value
            
            val totalInBaseCurrency = accounts
                .filter { it.includeInTotal }
                .sumOf { account ->
                    currencyService.convert(
                        amount = account.balance,
                        fromCurrency = account.currency,
                        toCurrency = currentBaseCurrency
                    )
                }
            
            _baseCurrencyTotal.value = totalInBaseCurrency
        }
    }
    
    /**
     * 格式化金额显示
     */
    fun formatCurrency(amount: Double, currency: Currency): String {
        return currencyService.formatCurrency(amount, currency)
    }
    
    /**
     * 获取某币种的汇率（相对于基准币种）
     */
    fun getExchangeRate(currency: Currency): Double {
        return currencyService.getExchangeRate(currency)
    }
    
    /**
     * 将金额从一种币种转换为另一种
     */
    fun convertCurrency(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double {
        return currencyService.convert(amount, fromCurrency, toCurrency)
    }
    
    /**
     * 获取所有支持的币种
     */
    fun getAllCurrencies(): List<Currency> {
        return currencyService.getAllCurrencies()
    }
    
    /**
     * 清除更新结果
     */
    fun clearUpdateResult() {
        currencyService.clearUpdateResult()
    }
} 