package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Currency
import com.ccjizhang.data.service.CurrencyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 币种设置ViewModel
 * 用于管理币种设置界面的数据和业务逻辑
 */
@HiltViewModel
class CurrencySettingsViewModel @Inject constructor(
    private val currencyService: CurrencyService
) : ViewModel() {
    
    // 当前基准币种
    private val _baseCurrency = MutableStateFlow(currencyService.baseCurrency.value)
    val baseCurrency: StateFlow<Currency> = _baseCurrency.asStateFlow()
    
    // 所有可用币种
    private val _availableCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val availableCurrencies: StateFlow<List<Currency>> = _availableCurrencies.asStateFlow()
    
    // 汇率映射表
    private val _exchangeRates = MutableStateFlow<Map<Currency, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<Currency, Double>> = _exchangeRates.asStateFlow()
    
    init {
        // 初始化币种列表和汇率数据
        _availableCurrencies.value = Currency.values().toList()
        
        // 初始化汇率数据
        val rates = mutableMapOf<Currency, Double>()
        for (currency in _availableCurrencies.value) {
            rates[currency] = currencyService.getExchangeRate(currency)
        }
        _exchangeRates.value = rates
    }
    
    /**
     * 设置基准币种
     */
    fun setBaseCurrency(currency: Currency) {
        _baseCurrency.value = currency
    }
    
    /**
     * 更新币种汇率
     */
    fun updateExchangeRate(currency: Currency, rate: Double) {
        val newRates = _exchangeRates.value.toMutableMap()
        newRates[currency] = rate
        _exchangeRates.value = newRates
    }
    
    /**
     * 保存设置
     */
    fun saveSettings() {
        viewModelScope.launch {
            // 设置基准币种
            currencyService.setBaseCurrency(_baseCurrency.value)
            
            // 更新所有汇率
            for ((currency, rate) in _exchangeRates.value) {
                currencyService.updateExchangeRate(currency, rate)
            }
        }
    }
} 