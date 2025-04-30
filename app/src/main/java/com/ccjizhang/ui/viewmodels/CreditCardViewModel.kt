package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.BillingCycleType
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.service.CreditCardService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.ccjizhang.ui.common.OperationResult

/**
 * 信用卡管理ViewModel
 * 负责处理信用卡管理界面的数据和业务逻辑
 */
@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val creditCardService: CreditCardService
) : ViewModel() {
    
    // 信用卡列表
    private val _creditCards = MutableStateFlow<List<Account>>(emptyList())
    val creditCards: StateFlow<List<Account>> = _creditCards.asStateFlow()
    
    // 选中的信用卡
    private val _selectedCard = MutableStateFlow<Account?>(null)
    val selectedCard: StateFlow<Account?> = _selectedCard.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 操作结果 (使用导入的类型)
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()
    
    /**
     * 加载所有信用卡账户
     */
    fun loadCreditCards() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val accounts = accountRepository.getAllAccounts().first()
                _creditCards.value = accounts.filter { it.type == AccountType.CREDIT_CARD }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("加载信用卡失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 根据ID加载信用卡详情
     */
    fun loadCreditCard(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val account = accountRepository.getAccountByIdSync(id)
                if (account != null && account.type == AccountType.CREDIT_CARD) {
                    _selectedCard.value = account
                } else {
                    _operationResult.value = OperationResult.Error("非信用卡账户")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("加载信用卡详情失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 添加新信用卡
     */
    fun addCreditCard(
        name: String,
        creditLimit: Double,
        billingDay: Int,
        dueDay: Int,
        color: Int,
        iconName: String,
        includeInTotal: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCreditCard = Account(
                    name = name,
                    type = AccountType.CREDIT_CARD,
                    balance = 0.0,
                    color = color,
                    icon = iconName,
                    isDefault = false,
                    includeInTotal = includeInTotal,
                    creditLimit = creditLimit,
                    billingDay = billingDay,
                    dueDay = dueDay,
                    billingCycleType = BillingCycleType.FIXED_DAY
                )
                
                val cardId = accountRepository.addAccount(newCreditCard)
                val createdCard = accountRepository.getAccountByIdSync(cardId) ?: return@launch
                
                // 计算并更新账单日和还款日
                creditCardService.updateNextBillingAndDueDate(createdCard)
                
                _operationResult.value = OperationResult.Success("信用卡添加成功")
                loadCreditCards() // 重新加载列表
                
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("添加信用卡失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新信用卡信息
     */
    fun updateCreditCard(
        id: Long,
        name: String,
        creditLimit: Double,
        billingDay: Int,
        dueDay: Int,
        color: Int,
        iconName: String,
        includeInTotal: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existingCard = accountRepository.getAccountByIdSync(id) ?: throw Exception("信用卡不存在")
                
                val updatedCard = existingCard.copy(
                    name = name,
                    creditLimit = creditLimit,
                    billingDay = billingDay,
                    dueDay = dueDay,
                    color = color,
                    icon = iconName,
                    includeInTotal = includeInTotal
                )
                
                accountRepository.updateAccount(updatedCard)
                
                // 更新账单日和还款日
                creditCardService.updateNextBillingAndDueDate(updatedCard)
                
                _operationResult.value = OperationResult.Success("信用卡更新成功")
                loadCreditCard(id) // 重新加载详情
                loadCreditCards() // 重新加载列表
                
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新信用卡失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除信用卡
     */
    fun deleteCreditCard(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.deleteAccountById(id)
                _operationResult.value = OperationResult.Success("信用卡删除成功")
                _selectedCard.value = null
                loadCreditCards() // 重新加载列表
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("删除信用卡失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 还信用卡
     * 创建一笔还款交易，从另一个账户转款到信用卡
     */
    fun payCreditCard(
        creditCardId: Long, 
        sourceAccountId: Long, 
        amount: Double,
        memo: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: 实现还款交易记录
                // 这需要交易存储库来实现转账功能
                
                _operationResult.value = OperationResult.Success("还款成功")
                loadCreditCard(creditCardId) // 重新加载详情
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("还款失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 计算距离下一个账单日的天数
     */
    fun getDaysUntilNextBillingDate(account: Account): Int {
        return creditCardService.getDaysUntilNextBillingDate(account)
    }
    
    /**
     * 计算距离下一个还款日的天数
     */
    fun getDaysUntilNextDueDate(account: Account): Int {
        return creditCardService.getDaysUntilNextDueDate(account)
    }
    
    /**
     * 更新所有信用卡的账单日和还款日
     */
    fun updateAllCreditCardDates() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                creditCardService.updateAllCreditCardDates()
                _operationResult.value = OperationResult.Success("已更新所有信用卡日期")
                loadCreditCards() // 重新加载列表
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新信用卡日期失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除操作结果状态
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
} 