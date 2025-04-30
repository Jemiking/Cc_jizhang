package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.service.CurrencyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date

/**
 * 账户转账ViewModel
 */
@HiltViewModel
class AccountTransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService
) : ViewModel() {
    
    // 所有账户列表
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    
    // 源账户
    private val _sourceAccount = MutableStateFlow<Account?>(null)
    val sourceAccount: StateFlow<Account?> = _sourceAccount.asStateFlow()
    
    // 目标账户
    private val _targetAccount = MutableStateFlow<Account?>(null)
    val targetAccount: StateFlow<Account?> = _targetAccount.asStateFlow()
    
    // 转账金额
    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()
    
    // 操作状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    
    // 转账结果状态
    private val _transferResult = MutableStateFlow(false)
    val transferResult: StateFlow<Boolean> = _transferResult.asStateFlow()
    
    /**
     * 加载所有账户
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _accounts.value = accountRepository.getAllAccountsSync()
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("加载账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 设置源账户
     */
    fun setSourceAccount(accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAccountByIdSync(accountId)
            _sourceAccount.value = account
        }
    }
    
    /**
     * 设置目标账户
     */
    fun setTargetAccount(accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAccountByIdSync(accountId)
            _targetAccount.value = account
        }
    }
    
    /**
     * 设置转账金额
     */
    fun setAmount(newAmount: String) {
        _amount.value = newAmount
    }
    
    /**
     * 在两个账户之间转账
     * @param fromAccountId 源账户ID
     * @param toAccountId 目标账户ID 
     * @param amount 转账金额
     * @param note 备注信息
     * @param date 交易日期
     */
    fun transferBetweenAccounts(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String = "",
        date: Date = Date()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sourceAccount = accountRepository.getAccountByIdSync(fromAccountId)
                    ?: throw Exception("源账户不存在")
                val targetAccount = accountRepository.getAccountByIdSync(toAccountId)
                    ?: throw Exception("目标账户不存在")
                
                if (amount <= 0) {
                    throw Exception("转账金额必须大于0")
                }
                
                if (sourceAccount.balance < amount) {
                    throw Exception("源账户余额不足")
                }
                
                // 处理跨币种转账
                if (sourceAccount.currency != targetAccount.currency) {
                    // 将金额从源币种转换为目标币种
                    val convertedAmount = currencyService.convert(
                        amount = amount,
                        fromCurrency = sourceAccount.currency,
                        toCurrency = targetAccount.currency
                    )
                    
                    // 从源账户减去原始金额
                    accountRepository.updateAccountBalance(sourceAccount.id, -amount)
                    
                    // 向目标账户添加转换后的金额
                    accountRepository.updateAccountBalance(targetAccount.id, convertedAmount)
                } else {
                    // 相同币种直接转账
                    accountRepository.updateAccountBalance(sourceAccount.id, -amount)
                    accountRepository.updateAccountBalance(targetAccount.id, amount)
                }
                
                _operationResult.value = OperationResult.Success("转账成功")
                _transferResult.value = true
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("转账失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 执行转账
     */
    fun transferFunds() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val source = _sourceAccount.value ?: throw Exception("未选择源账户")
                val target = _targetAccount.value ?: throw Exception("未选择目标账户")
                val transferAmount = _amount.value.toDoubleOrNull() ?: throw Exception("无效的金额")
                
                if (transferAmount <= 0) {
                    throw Exception("转账金额必须大于0")
                }
                
                if (source.balance < transferAmount) {
                    throw Exception("源账户余额不足")
                }
                
                // 处理跨币种转账
                if (source.currency != target.currency) {
                    // 将金额从源币种转换为目标币种
                    val convertedAmount = currencyService.convert(
                        amount = transferAmount,
                        fromCurrency = source.currency,
                        toCurrency = target.currency
                    )
                    
                    // 从源账户减去原始金额
                    accountRepository.updateAccountBalance(source.id, -transferAmount)
                    
                    // 向目标账户添加转换后的金额
                    accountRepository.updateAccountBalance(target.id, convertedAmount)
                } else {
                    // 相同币种直接转账
                    accountRepository.updateAccountBalance(source.id, -transferAmount)
                    accountRepository.updateAccountBalance(target.id, transferAmount)
                }
                
                _operationResult.value = OperationResult.Success("转账成功")
                _transferResult.value = true
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("转账失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 格式化货币显示
     */
    fun formatCurrency(amount: Double, account: Account): String {
        return currencyService.formatCurrency(amount, account.currency)
    }
    
    /**
     * 清除操作结果
     */
    fun clearResult() {
        _operationResult.value = null
        _transferResult.value = false
    }

    /**
     * 操作结果类
     */
    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
} 