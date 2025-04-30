package com.ccjizhang.data.repository

import android.graphics.Color
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountSortType
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账户数据仓库
 * 负责提供账户数据和处理业务逻辑
 */
@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao
) {
    // 获取所有账户
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()

    /**
     * 按指定排序方式获取账户
     */
    fun getSortedAccounts(sortType: AccountSortType = AccountSortType.CUSTOM): Flow<List<Account>> {
        return when (sortType) {
            AccountSortType.NAME -> accountDao.getAccountsSortedByName()
            AccountSortType.BALANCE -> accountDao.getAccountsSortedByBalance()
            AccountSortType.TYPE -> accountDao.getAccountsSortedByType()
            AccountSortType.CUSTOM -> accountDao.getAccountsSortedByDisplayOrder()
        }
    }

    // 获取所有账户（同步方法）
    suspend fun getAllAccountsSync(): List<Account> = accountDao.getAllAccountsSync()

    // 按ID获取账户
    fun getAccountById(id: Long): Flow<Account?> = accountDao.getAccountById(id)

    // 按ID获取账户（同步方法）
    suspend fun getAccountByIdSync(id: Long): Account? = accountDao.getAccountByIdSync(id)

    // 按类型获取账户
    fun getAccountsByType(type: AccountType): Flow<List<Account>> =
        accountDao.getAccountsByType(type)

    // 获取默认账户
    fun getDefaultAccount(): Flow<Account?> = accountDao.getDefaultAccount()

    // 获取默认账户（同步方法）
    suspend fun getDefaultAccountSync(): Account? = accountDao.getDefaultAccountSync()

    // 获取包含在总资产中的账户
    fun getAccountsIncludedInTotal(): Flow<List<Account>> =
        accountDao.getAccountsIncludedInTotal()

    // 获取总余额
    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()

    // 添加账户
    suspend fun addAccount(account: Account): Long {
        // 如果是默认账户，确保其他账户不是默认的
        if (account.isDefault) {
            clearDefaultAccount()
        }
        return accountDao.insert(account)
    }

    // 更新账户
    suspend fun updateAccount(account: Account) {
        // 如果是默认账户，确保其他账户不是默认的
        if (account.isDefault) {
            clearDefaultAccount()
        }
        accountDao.update(account)
    }

    // 删除账户
    suspend fun deleteAccount(account: Account) {
        accountDao.delete(account)
    }

    // 按ID删除账户
    suspend fun deleteAccountById(id: Long) {
        accountDao.deleteById(id)
    }

    /**
     * 更新账户余额（增加或减少）
     * @param id 账户ID
     * @param amount 金额变化值（正数增加，负数减少）
     */
    suspend fun updateAccountBalance(id: Long, amount: Double) {
        val account = accountDao.getAccountByIdSync(id) ?: return
        val newBalance = account.balance + amount
        accountDao.update(account.copy(balance = newBalance))
    }

    /**
     * 直接设置账户余额
     * @param id 账户ID
     * @param balance 新的余额值
     */
    suspend fun updateAccountTo(id: Long, balance: Double) {
        val account = accountDao.getAccountByIdSync(id) ?: return
        accountDao.update(account.copy(balance = balance))
    }

    /**
     * 获取某个币种的所有账户
     * @param currency 币种
     * @return 账户列表流
     */
    fun getAccountsByCurrency(currency: Currency): Flow<List<Account>> =
        accountDao.getAccountsByCurrency(currency)

    /**
     * 按分类获取账户
     * @param categoryId 分类ID，为null时获取未分类账户
     * @return 账户列表流
     */
    fun getAccountsByCategory(categoryId: Long?): Flow<List<Account>> =
        accountDao.getAccountsByCategory(categoryId)

    /**
     * 获取按分类分组的账户
     * @return 分类与账户的映射
     */
    fun getAccountsGroupedByCategory(): Flow<Map<Long?, List<Account>>> =
        accountDao.getAllAccounts().map { accounts ->
            accounts.groupBy { it.categoryId }
        }

    /**
     * 更新账户币种
     * @param id 账户ID
     * @param currency 新币种
     * @param exchangeRate 新汇率
     */
    suspend fun updateAccountCurrency(id: Long, currency: Currency, exchangeRate: Double) {
        val account = accountDao.getAccountByIdSync(id) ?: return
        accountDao.update(account.copy(currency = currency, exchangeRate = exchangeRate))
    }

    /**
     * 更新账户分类
     * @param accountId 账户ID
     * @param categoryId 分类ID
     */
    suspend fun updateAccountCategory(accountId: Long, categoryId: Long?) {
        val account = accountDao.getAccountByIdSync(accountId) ?: return
        accountDao.update(account.copy(categoryId = categoryId))
    }

    /**
     * 更新账户显示顺序
     * @param accountId 账户ID
     * @param displayOrder 显示顺序
     */
    suspend fun updateAccountOrder(accountId: Long, displayOrder: Int) {
        val account = accountDao.getAccountByIdSync(accountId) ?: return
        accountDao.update(account.copy(displayOrder = displayOrder))
    }

    /**
     * 批量更新账户显示顺序
     * @param accountOrders 账户ID与显示顺序的映射
     */
    suspend fun updateAccountOrders(accountOrders: Map<Long, Int>) {
        for ((accountId, order) in accountOrders) {
            updateAccountOrder(accountId, order)
        }
    }

    // 清除所有默认账户标记
    private suspend fun clearDefaultAccount() {
        val accounts = accountDao.getAllAccountsSync()
        for (account in accounts) {
            if (account.isDefault) {
                accountDao.update(account.copy(isDefault = false))
            }
        }
    }

    // 初始化默认账户
    suspend fun initDefaultAccounts() {
        val accounts = accountDao.getAllAccountsSync()
        if (accounts.isEmpty()) {
            val defaultAccounts = listOf(
                Account(
                    name = "现金",
                    type = AccountType.CASH,
                    balance = 0.0,
                    currency = Currency.CNY,
                    icon = "account_balance_wallet",
                    color = 0xFF43A047.toInt(),
                    isDefault = true
                ),
                Account(
                    name = "银行卡",
                    type = AccountType.DEBIT_CARD,
                    balance = 0.0,
                    currency = Currency.CNY,
                    icon = "credit_card",
                    color = 0xFF1976D2.toInt(),
                    isDefault = false
                ),
                Account(
                    name = "支付宝",
                    type = AccountType.ALIPAY,
                    balance = 0.0,
                    currency = Currency.CNY,
                    icon = "payment",
                    color = 0xFF039BE5.toInt(),
                    isDefault = false
                ),
                Account(
                    name = "微信",
                    type = AccountType.WECHAT,
                    balance = 0.0,
                    currency = Currency.CNY,
                    icon = "chat",
                    color = 0xFF4CAF50.toInt(),
                    isDefault = false
                )
            )
            accountDao.insertAll(defaultAccounts)
        }
    }
}