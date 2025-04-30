package com.ccjizhang.data.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ccjizhang.data.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * 归档账户数据访问对象
 * 用于访问归档数据库中的账户数据
 */
@Dao
interface ArchiveAccountDao {
    
    /**
     * 插入账户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long
    
    /**
     * 批量插入账户
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<Account>)
    
    /**
     * 获取所有账户
     */
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<Account>>
    
    /**
     * 获取指定ID的账户
     */
    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<Account?>
}
