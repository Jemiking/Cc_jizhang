package com.ccjizhang.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.data.model.AccountSortType
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.Currency
import com.ccjizhang.data.repository.AccountCategoryRepository
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.service.CurrencyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.theme.CategoryBlue
import com.ccjizhang.ui.theme.CategoryOrange
import com.ccjizhang.ui.theme.CategoryPink
import androidx.compose.ui.graphics.toArgb
import com.ccjizhang.ui.common.OperationResult

/**
 * 账户数据ViewModel
 * 负责提供账户数据和处理相关业务逻辑
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountCategoryRepository: AccountCategoryRepository,
    private val currencyService: CurrencyService
) : ViewModel() {

    // 所有账户列表
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    // 所有账户分类
    private val _accountCategories = MutableStateFlow<List<AccountCategory>>(emptyList())
    val accountCategories: StateFlow<List<AccountCategory>> = _accountCategories.asStateFlow()

    // 按分类分组的账户
    private val _accountsGroupedByCategory = MutableStateFlow<Map<Long?, List<Account>>>(emptyMap())
    val accountsGroupedByCategory: StateFlow<Map<Long?, List<Account>>> = _accountsGroupedByCategory.asStateFlow()

    // 排序方式
    private val _sortType = MutableStateFlow(AccountSortType.CUSTOM)
    val sortType: StateFlow<AccountSortType> = _sortType.asStateFlow()

    // 当前选中的账户
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    // 总余额数据流
    private val _totalBalance = MutableStateFlow<Double>(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 操作结果状态
    private val _operationResult = MutableStateFlow<OperationResult?>(null)
    val operationResult: StateFlow<OperationResult?> = _operationResult.asStateFlow()

    // 当前基准币种
    val baseCurrency = currencyService.baseCurrency

    // 支持的所有币种
    private val _availableCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val availableCurrencies: StateFlow<List<Currency>> = _availableCurrencies.asStateFlow()

    init {
        _availableCurrencies.value = currencyService.getAllCurrencies()
    }

    /**
     * 加载账户数据
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载账户分类
                accountCategoryRepository.getAllCategories().collect { categories ->
                    _accountCategories.value = categories
                }

                // 根据排序方式加载账户
                accountRepository.getSortedAccounts(_sortType.value).collect { accountList ->
                    _accounts.value = accountList
                    _totalBalance.value = calculateTotalBalanceInBaseCurrency(accountList)
                }

                // 加载按分类分组的账户
                accountRepository.getAccountsGroupedByCategory().collect { groupedAccounts ->
                    _accountsGroupedByCategory.value = groupedAccounts
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("加载账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 计算总余额（转换为基准币种）
     */
    private fun calculateTotalBalanceInBaseCurrency(accounts: List<Account>): Double {
        val currentBaseCurrency = baseCurrency.value
        return accounts.filter { it.includeInTotal }
            .sumOf { account ->
                currencyService.convert(
                    amount = account.balance,
                    fromCurrency = account.currency,
                    toCurrency = currentBaseCurrency
                )
            }
    }

    /**
     * 通过ID获取账户
     */
    fun getAccountById(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.getAccountById(id).collectLatest { account ->
                    _selectedAccount.value = account
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("获取账户详情失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加新账户
     */
    fun addAccount(account: Account) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.addAccount(account)
                _operationResult.value = OperationResult.Success("添加账户成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("添加账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新账户
     */
    fun updateAccount(account: Account) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.updateAccount(account)
                _operationResult.value = OperationResult.Success("更新账户成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除账户
     */
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.deleteAccountById(id)
                _operationResult.value = OperationResult.Success("删除账户成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("删除账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 格式化金额显示
     */
    fun formatCurrency(amount: Double, currency: Currency): String {
        return currencyService.formatCurrency(amount, currency)
    }

    /**
     * 设置基准币种
     */
    fun setBaseCurrency(currency: Currency) {
        currencyService.setBaseCurrency(currency)
        // 更新总余额
        viewModelScope.launch {
            val accounts = _accounts.value
            _totalBalance.value = calculateTotalBalanceInBaseCurrency(accounts)
        }
    }

    /**
     * 将金额从一种币种转换为另一种
     */
    fun convertCurrency(amount: Double, fromCurrency: Currency, toCurrency: Currency): Double {
        return currencyService.convert(amount, fromCurrency, toCurrency)
    }

    /**
     * 清除操作结果状态
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }

    /**
     * 设置排序方式
     */
    fun setSortType(type: AccountSortType) {
        _sortType.value = type
        loadAccounts()
    }

    /**
     * 更新账户分类
     */
    fun updateAccountCategory(accountId: Long, categoryId: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.updateAccountCategory(accountId, categoryId)
                _operationResult.value = OperationResult.Success("更新账户分类成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新账户分类失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新账户显示顺序
     */
    fun updateAccountOrder(accountId: Long, displayOrder: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.updateAccountOrder(accountId, displayOrder)
                _operationResult.value = OperationResult.Success("更新账户顺序成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新账户顺序失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 批量更新账户显示顺序
     */
    fun updateAccountOrders(accountOrders: Map<Long, Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRepository.updateAccountOrders(accountOrders)
                _operationResult.value = OperationResult.Success("更新账户顺序成功")
            } catch (e: Exception) {
                _operationResult.value = OperationResult.Error("更新账户顺序失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取所有可用图标
     */
    fun getAvailableIcons(): List<Pair<ImageVector, Color>> {
        return listOf(
            Pair(Icons.Default.AccountBalance, PrimaryDark),
            Pair(Icons.Default.AccountBalanceWallet, CategoryBlue),
            Pair(Icons.Default.CreditCard, CategoryOrange),
            Pair(Icons.Default.Payment, CategoryPink)
        )
    }
}

/**
 * 用于UI展示的账户数据类，包含账户信息和图标
 */
data class AccountWithIcon(
    val account: Account,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
)