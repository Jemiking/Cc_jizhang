package com.ccjizhang.data.service

import android.util.Log
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Currency
import com.ccjizhang.data.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 总资产计算服务
 * 提供统一的总资产计算逻辑，确保应用中所有地方使用相同的计算方式
 */
@Singleton
class TotalBalanceService @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService
) {
    private val TAG = "TotalBalanceService"

    /**
     * 计算总资产（考虑includeInTotal和币种转换）
     * @param baseCurrency 基准币种，默认为人民币
     * @return 总资产金额
     */
    suspend fun calculateTotalBalance(baseCurrency: Currency = Currency.CNY): Double {
        val accounts = accountRepository.getAllAccounts().first()
        return calculateTotalBalanceFromAccounts(accounts, true, baseCurrency)
    }
    
    /**
     * 计算所有账户总资产（不考虑includeInTotal）
     * @param baseCurrency 基准币种，默认为人民币
     * @return 总资产金额
     */
    suspend fun calculateTotalBalanceAll(baseCurrency: Currency = Currency.CNY): Double {
        val accounts = accountRepository.getAllAccounts().first()
        return calculateTotalBalanceFromAccounts(accounts, false, baseCurrency)
    }
    
    /**
     * 从账户列表计算总资产
     * @param accounts 账户列表
     * @param respectIncludeInTotal 是否考虑includeInTotal属性
     * @param baseCurrency 基准币种
     * @return 总资产金额
     */
    private fun calculateTotalBalanceFromAccounts(
        accounts: List<Account>,
        respectIncludeInTotal: Boolean,
        baseCurrency: Currency
    ): Double {
        // 日志记录
        Log.d(TAG, "计算总资产，账户数量: ${accounts.size}, " +
                "考虑includeInTotal: $respectIncludeInTotal, 基准币种: $baseCurrency")
        
        // 过滤账户
        val accountsToCalculate = if (respectIncludeInTotal) {
            accounts.filter { it.includeInTotal }
        } else {
            accounts
        }
        
        Log.d(TAG, "过滤后账户数量: ${accountsToCalculate.size}")
        
        // 计算总余额
        var totalBalance = 0.0
        for (account in accountsToCalculate) {
            val convertedBalance = currencyService.convert(
                amount = account.balance,
                fromCurrency = account.currency,
                toCurrency = baseCurrency
            )
            Log.d(TAG, "账户[${account.name}] 余额: ${account.balance} " +
                    "${account.currency} -> $convertedBalance $baseCurrency")
            totalBalance += convertedBalance
        }
        
        Log.d(TAG, "最终计算的总余额: $totalBalance $baseCurrency")
        return totalBalance
    }
}
