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
import kotlinx.coroutines.flow.first
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

        // 检查数据库中的账户状态
        viewModelScope.launch {
            checkDatabaseAccounts()
        }
    }

    /**
     * 检查数据库中的账户状态
     */
    private suspend fun checkDatabaseAccounts() {
        try {
            println("DEBUG: 开始检查数据库中的账户状态")

            // 直接从数据库获取所有账户
            val allAccounts = accountRepository.getAllAccountsSync()
            println("DEBUG: 数据库中共有 ${allAccounts.size} 个账户")

            if (allAccounts.isEmpty()) {
                println("DEBUG: 警告 - 数据库中没有账户数据!")
            } else {
                allAccounts.forEachIndexed { index, account ->
                    println("DEBUG: 数据库账户[$index]: id=${account.id}, name=${account.name}, balance=${account.balance}, includeInTotal=${account.includeInTotal}")
                }

                // 检查includeInTotal属性
                val includedAccounts = allAccounts.filter { it.includeInTotal }
                println("DEBUG: 数据库中includeInTotal=true的账户数量: ${includedAccounts.size}/${allAccounts.size}")

                // 计算总余额
                val totalBalance = allAccounts.sumOf { it.balance }
                println("DEBUG: 数据库中所有账户的总余额: $totalBalance")
            }

            println("DEBUG: 数据库账户状态检查完成")
        } catch (e: Exception) {
            println("DEBUG: 检查数据库账户状态失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 加载账户数据
     */
    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                println("DEBUG: 开始加载账户数据 - 使用一次性加载方式")

                // 一次性加载所有数据，而不是使用collect持续收集
                // 1. 加载账户分类
                println("DEBUG: 开始加载账户分类")
                val categories = accountCategoryRepository.getAllCategories().first()
                println("DEBUG: 加载到 ${categories.size} 个账户分类")
                _accountCategories.value = categories

                // 2. 根据排序方式加载账户
                println("DEBUG: 开始加载账户，排序方式: ${_sortType.value}")
                try {
                    val accountList = accountRepository.getSortedAccounts(_sortType.value).first()
                    println("DEBUG: 加载到 ${accountList.size} 个账户")
                    if (accountList.isEmpty()) {
                        println("DEBUG: 警告 - 账户列表为空!")
                        // 尝试直接加载所有账户，不使用排序
                        println("DEBUG: 尝试直接加载所有账户，不使用排序")
                        val allAccounts = accountRepository.getAllAccounts().first()
                        println("DEBUG: 直接加载到 ${allAccounts.size} 个账户")
                        if (allAccounts.isNotEmpty()) {
                            println("DEBUG: 使用直接加载的账户列表")
                            _accounts.value = allAccounts
                            val calculatedBalance = calculateTotalBalanceInBaseCurrency(allAccounts)
                            println("DEBUG: 计算的总余额: $calculatedBalance")
                            _totalBalance.value = calculatedBalance
                        }
                    } else {
                        accountList.forEachIndexed { index, account ->
                            println("DEBUG: 账户[$index]: id=${account.id}, name=${account.name}, balance=${account.balance}, includeInTotal=${account.includeInTotal}")
                        }

                        _accounts.value = accountList
                        val calculatedBalance = calculateTotalBalanceInBaseCurrency(accountList)
                        println("DEBUG: 计算的总余额: $calculatedBalance")
                        _totalBalance.value = calculatedBalance
                    }
                } catch (e: Exception) {
                    println("DEBUG: 加载排序账户失败: ${e.message}")
                    // 尝试直接加载所有账户
                    val allAccounts = accountRepository.getAllAccountsSync()
                    println("DEBUG: 同步加载到 ${allAccounts.size} 个账户")
                    _accounts.value = allAccounts
                    val calculatedBalance = calculateTotalBalanceInBaseCurrency(allAccounts)
                    _totalBalance.value = calculatedBalance
                }

                // 3. 加载按分类分组的账户
                println("DEBUG: 开始加载按分类分组的账户")
                try {
                    val groupedAccounts = accountRepository.getAccountsGroupedByCategory().first()
                    println("DEBUG: 加载到 ${groupedAccounts.size} 个分组")
                    _accountsGroupedByCategory.value = groupedAccounts
                } catch (e: Exception) {
                    println("DEBUG: 加载分组账户失败: ${e.message}")
                    // 使用已加载的账户创建分组
                    val accounts = _accounts.value
                    val groupedAccounts = accounts.groupBy { it.categoryId }
                    _accountsGroupedByCategory.value = groupedAccounts
                }

                println("DEBUG: 账户数据加载完成")
            } catch (e: Exception) {
                println("DEBUG: 加载账户失败: ${e.message}")
                e.printStackTrace()
                _operationResult.value = OperationResult.Error("加载账户失败: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 计算总余额（转换为基准币种）
     * @param accounts 账户列表
     * @param respectIncludeInTotal 是否尊重includeInTotal属性，默认为true
     * @return 总余额
     */
    private fun calculateTotalBalanceInBaseCurrency(
        accounts: List<Account>,
        respectIncludeInTotal: Boolean = true
    ): Double {
        val currentBaseCurrency = baseCurrency.value
        println("DEBUG: 计算总余额，基准币种: $currentBaseCurrency, 尊重includeInTotal: $respectIncludeInTotal")

        // 根据参数决定是否过滤账户
        val accountsToCalculate = if (respectIncludeInTotal) {
            accounts.filter { it.includeInTotal }
        } else {
            accounts
        }

        println("DEBUG: ${if (respectIncludeInTotal) "过滤后" else "所有"}的账户数量: ${accountsToCalculate.size}/${accounts.size}")

        if (accountsToCalculate.isEmpty() && accounts.isNotEmpty()) {
            println("DEBUG: 警告 - ${if (respectIncludeInTotal) "所有账户的includeInTotal都为false!" else "没有账户可计算!"}")

            // 如果过滤后没有账户，但原始列表有账户，尝试不过滤重新计算
            if (respectIncludeInTotal) {
                println("DEBUG: 尝试忽略includeInTotal重新计算")
                return calculateTotalBalanceInBaseCurrency(accounts, false)
            }
        }

        // 如果没有账户可计算，直接返回0
        if (accountsToCalculate.isEmpty()) {
            println("DEBUG: 没有账户可计算，返回0")
            return 0.0
        }

        // 计算总余额
        var totalBalance = 0.0
        for ((index, account) in accountsToCalculate.withIndex()) {
            val convertedBalance = currencyService.convert(
                amount = account.balance,
                fromCurrency = account.currency,
                toCurrency = currentBaseCurrency
            )
            println("DEBUG: 账户[$index] ${account.name} 余额转换: ${account.balance} ${account.currency} -> $convertedBalance $currentBaseCurrency")
            totalBalance += convertedBalance
        }

        println("DEBUG: 最终计算的总余额: $totalBalance")
        return totalBalance
    }

    /**
     * 计算总余额（不考虑includeInTotal属性）
     * 这个方法与HomeViewModel中的计算方式一致
     */
    private fun calculateTotalBalanceWithoutFilter(accounts: List<Account>): Double {
        println("DEBUG: 计算总余额（不过滤），账户数量: ${accounts.size}")
        val totalBalance = accounts.sumOf { it.balance }
        println("DEBUG: 不过滤的总余额: $totalBalance")
        return totalBalance
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