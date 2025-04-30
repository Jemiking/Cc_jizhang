package com.ccjizhang.data.archive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ccjizhang.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 归档分类数据访问对象
 * 用于访问归档数据库中的分类数据
 */
@Dao
interface ArchiveCategoryDao {
    
    /**
     * 插入分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    /**
     * 批量插入分类
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
    
    /**
     * 获取所有分类
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    /**
     * 获取指定ID的分类
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<Category?>
    
    /**
     * 获取支出分类
     */
    @Query("SELECT * FROM categories WHERE type = 0 ORDER BY sortOrder ASC")
    fun getExpenseCategories(): Flow<List<Category>>
    
    /**
     * 获取收入分类
     */
    @Query("SELECT * FROM categories WHERE type = 1 ORDER BY sortOrder ASC")
    fun getIncomeCategories(): Flow<List<Category>>
}
