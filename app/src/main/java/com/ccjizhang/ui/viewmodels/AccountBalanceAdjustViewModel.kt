package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.service.CurrencyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 账户余额调整ViewModel
 * 负责处理账户余额调整功能的业务逻辑
 */
@HiltViewModel
class AccountBalanceAdjustViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService
) : ViewModel() {
    
    // 所有账户列表
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()
    
    // 选中的账户
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()
    
    // 调整金额
    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()
    
    // 操作状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 操作结果
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    
    // 添加 adjustmentResult 属性，用于标记余额调整是否成功
    private val _adjustmentResult = MutableStateFlow(false)
    val adjustmentResult: StateFlow<Boolean> = _adjustmentResult.asStateFlow()
    
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
     * 根据ID加载特定账户
     */
    fun loadAccount(accountId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = accountRepository.getAccountByIdSync(accountId)
                _selectedAccount.value = account
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("加载账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 设置选中的账户
     */
    fun setSelectedAccount(accountId: Long) {
        viewModelScope.launch {
            val account = accountRepository.getAccountByIdSync(accountId)
            _selectedAccount.value = account
        }
    }
    
    /**
     * 设置调整金额
     */
    fun setAmount(newAmount: String) {
        _amount.value = newAmount
    }
    
    /**
     * 直接设置账户余额
     */
    fun setAccountBalance(accountId: Long, newBalance: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.updateAccountTo(accountId, newBalance)
                _adjustmentResult.value = true
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("设置余额失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 调整账户余额（增加或减少）
     */
    fun adjustAccountBalance(accountId: Long, adjustAmount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = accountRepository.getAccountByIdSync(accountId)
                if (account != null) {
                    val newBalance = account.balance + adjustAmount
                    accountRepository.updateAccountTo(accountId, newBalance)
                    _adjustmentResult.value = true
                } else {
                    _operationResult.value = OperationResult.Error("账户不存在")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("调整余额失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 调整账户余额（旧方法，保持兼容性）
     */
    fun adjustBalance() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = _selectedAccount.value ?: throw Exception("未选择账户")
                val adjustAmount = _amount.value.toDoubleOrNull() ?: throw Exception("无效的金额")
                
                // 直接设置新余额
                accountRepository.updateAccountTo(account.id, adjustAmount)
                
                _operationResult.value = OperationResult.Success("余额已调整")
                _adjustmentResult.value = true
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("余额调整失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 重置调整结果状态
     */
    fun resetAdjustmentResult() {
        _adjustmentResult.value = false
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
    }
    
    /**
     * 操作结果类
     */
    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
} 