package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.Currency
import kotlinx.coroutines.flow.Flow

/**
 * 账户数据访问对象
 */
@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 删除所有账户
     * @return 删除的记录数量
     */
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts(): Int

    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountByIdSync(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE type = :type")
    fun getAccountsByType(type: AccountType): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE currency = :currency")
    fun getAccountsByCurrency(currency: Currency): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    fun getDefaultAccount(): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAccountSync(): Account?

    @Query("SELECT * FROM accounts WHERE includeInTotal = 1")
    fun getAccountsIncludedInTotal(): Flow<List<Account>>

    @Query("SELECT SUM(balance) FROM accounts WHERE includeInTotal = 1")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE includeInTotal = 1")
    suspend fun getTotalBalanceSync(): Double?

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Long, amount: Double)

    /**
     * 根据ID列表获取账户
     */
    @Query("SELECT * FROM accounts WHERE id IN (:ids)")
    fun getAccountsByIds(ids: List<Long>): Flow<List<Account>>

    /**
     * 按分类获取账户
     */
    @Query("SELECT * FROM accounts WHERE categoryId = :categoryId ORDER BY displayOrder")
    fun getAccountsByCategory(categoryId: Long?): Flow<List<Account>>

    /**
     * 按名称排序获取账户
     */
    @Query("SELECT * FROM accounts ORDER BY name")
    fun getAccountsSortedByName(): Flow<List<Account>>

    /**
     * 按余额排序获取账户
     */
    @Query("SELECT * FROM accounts ORDER BY balance DESC")
    fun getAccountsSortedByBalance(): Flow<List<Account>>

    /**
     * 按类型排序获取账户
     */
    @Query("SELECT * FROM accounts ORDER BY type, name")
    fun getAccountsSortedByType(): Flow<List<Account>>

    /**
     * 按自定义顺序排序获取账户
     */
    @Query("SELECT * FROM accounts ORDER BY displayOrder")
    fun getAccountsSortedByDisplayOrder(): Flow<List<Account>>
}