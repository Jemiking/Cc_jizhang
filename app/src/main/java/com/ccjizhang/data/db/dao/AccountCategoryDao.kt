package com.ccjizhang.data.db.dao

import androidx.room.*
import com.ccjizhang.data.model.AccountCategory
import kotlinx.coroutines.flow.Flow

/**
 * 账户分类DAO
 */
@Dao
interface AccountCategoryDao {
    /**
     * 获取所有账户分类
     */
    @Query("SELECT * FROM account_categories ORDER BY sortOrder")
    fun getAllCategories(): Flow<List<AccountCategory>>
    
    /**
     * 获取指定ID的账户分类
     */
    @Query("SELECT * FROM account_categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<AccountCategory?>
    
    /**
     * 获取指定ID的账户分类（同步方法）
     */
    @Query("SELECT * FROM account_categories WHERE id = :id")
    suspend fun getCategoryByIdSync(id: Long): AccountCategory?
    
    /**
     * 获取默认账户分类
     */
    @Query("SELECT * FROM account_categories WHERE isDefault = 1 LIMIT 1")
    fun getDefaultCategory(): Flow<AccountCategory?>
    
    /**
     * 获取默认账户分类（同步方法）
     */
    @Query("SELECT * FROM account_categories WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultCategorySync(): AccountCategory?
    
    /**
     * 插入账户分类
     */
    @Insert
    suspend fun insert(category: AccountCategory): Long
    
    /**
     * 更新账户分类
     */
    @Update
    suspend fun update(category: AccountCategory)
    
    /**
     * 删除账户分类
     */
    @Delete
    suspend fun delete(category: AccountCategory)
    
    /**
     * 按ID删除账户分类
     */
    @Query("DELETE FROM account_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 获取所有账户分类（同步方法）
     */
    @Query("SELECT * FROM account_categories ORDER BY sortOrder")
    suspend fun getAllCategoriesSync(): List<AccountCategory>
    
    /**
     * 获取账户分类数量
     */
    @Query("SELECT COUNT(*) FROM account_categories")
    suspend fun getCategoryCount(): Int
}
