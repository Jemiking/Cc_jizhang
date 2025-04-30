package com.ccjizhang.data.service

import com.ccjizhang.data.model.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * 货币服务
 * 提供币种转换、格式化和相关功能
 */
@Singleton
class CurrencyService @Inject constructor() {
    
    private val TAG = "CurrencyService"
    
    // 当前基准币种
    private val _baseCurrency = MutableStateFlow(Currency.CNY)
    val baseCurrency: StateFlow<Currency> = _baseCurrency.asStateFlow()
    
    // 汇率更新状态
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()
    
    // 上次更新时间
    private val _lastUpdateTime = MutableStateFlow<Date?>(null)
    val lastUpdateTime: StateFlow<Date?> = _lastUpdateTime.asStateFlow()
    
    // 更新结果
    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()
    
    // 币种汇率映射表（相对于基准币种）
    private val exchangeRates = mutableMapOf<Currency, Double>().apply {
        // 初始化汇率（相对于CNY）
        put(Currency.CNY, 1.0)       // 人民币
        put(Currency.USD, 0.14)      // 美元
        put(Currency.EUR, 0.13)      // 欧元
        put(Currency.GBP, 0.11)      // 英镑
        put(Currency.JPY, 20.86)     // 日元
        put(Currency.HKD, 1.09)      // 港币
        put(Currency.KRW, 186.63)    // 韩元
        put(Currency.CAD, 0.19)      // 加元
        put(Currency.AUD, 0.21)      // 澳元
        put(Currency.SGD, 0.19)      // 新加坡元
    }
    
    /**
     * 设置基准币种
     */
    fun setBaseCurrency(currency: Currency) {
        _baseCurrency.value = currency
    }
    
    /**
     * 获取所有支持的币种
     */
    fun getAllCurrencies(): List<Currency> {
        return Currency.values().toList()
    }
    
    /**
     * 更新币种汇率
     */
    fun updateExchangeRate(currency: Currency, rate: Double) {
        exchangeRates[currency] = rate
    }
    
    /**
     * 获取币种汇率（相对于基准币种）
     */
    fun getExchangeRate(currency: Currency): Double {
        return exchangeRates[currency] ?: 1.0
    }
    
    /**
     * 币种金额转换
     * 
     * @param amount 要转换的金额
     * @param fromCurrency 源币种
     * @param toCurrency 目标币种
     * @return 转换后的金额
     */
    fun convert(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double {
        if (fromCurrency == toCurrency) return amount
        
        // 获取两个币种相对于基准币种的汇率
        val fromRate = exchangeRates[fromCurrency] ?: 1.0
        val toRate = exchangeRates[toCurrency] ?: 1.0
        
        // 计算转换金额
        return amount * (toRate / fromRate)
    }
    
    /**
     * 格式化货币显示
     * 
     * @param amount 金额
     * @param currency 币种
     * @return 格式化的货币字符串
     */
    fun formatCurrency(amount: Double, currency: Currency): String {
        val locale = when (currency) {
            Currency.CNY -> Locale.CHINA
            Currency.USD -> Locale.US
            Currency.EUR -> Locale.GERMANY
            Currency.GBP -> Locale.UK
            Currency.JPY -> Locale.JAPAN
            else -> Locale.getDefault()
        }
        
        val format = NumberFormat.getCurrencyInstance(locale)
        return format.format(amount)
    }
    
    /**
     * 自动更新汇率
     * 使用免费的汇率API获取最新汇率数据
     */
    suspend fun updateExchangeRates() = withContext(Dispatchers.IO) {
        if (_isUpdating.value) return@withContext
        
        _isUpdating.value = true
        _updateResult.value = null
        
        try {
            // 使用免费的汇率API
            val apiUrl = "https://open.er-api.com/v6/latest/${_baseCurrency.value.code}"
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            try {
                connection.connectTimeout = 10000 // 10秒超时
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseExchangeRateResponse(response)
                    
                    _lastUpdateTime.value = Date()
                    _updateResult.value = UpdateResult.Success("汇率更新成功")
                    
                    Log.d(TAG, "汇率更新成功")
                } else {
                    _updateResult.value = UpdateResult.Error("API请求失败: $responseCode")
                    Log.e(TAG, "API请求失败: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            _updateResult.value = UpdateResult.Error("更新汇率失败: ${e.message}")
            Log.e(TAG, "更新汇率失败", e)
        } finally {
            _isUpdating.value = false
        }
    }
    
    /**
     * 解析汇率API响应
     */
    private fun parseExchangeRateResponse(response: String) {
        try {
            val jsonObject = JSONObject(response)
            val rates = jsonObject.getJSONObject("rates")
            
            // 更新所有支持的币种汇率
            for (currency in Currency.values()) {
                if (rates.has(currency.code)) {
                    val rate = rates.getDouble(currency.code)
                    val baseRate = rates.getDouble(_baseCurrency.value.code)
                    // 计算相对于基准币种的汇率
                    val relativeRate = baseRate / rate
                    exchangeRates[currency] = relativeRate
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析汇率数据失败", e)
            throw e
        }
    }
    
    /**
     * 检查是否需要更新汇率
     * @return 如果上次更新时间超过24小时，返回true
     */
    fun shouldUpdateRates(): Boolean {
        val lastUpdate = _lastUpdateTime.value ?: return true
        val now = Date()
        val diffMillis = now.time - lastUpdate.time
        return diffMillis > TimeUnit.HOURS.toMillis(24)
    }
    
    /**
     * 清除更新结果
     */
    fun clearUpdateResult() {
        _updateResult.value = null
    }
    
    /**
     * 汇率更新结果
     */
    sealed class UpdateResult {
        data class Success(val message: String) : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
} 