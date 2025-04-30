package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 删除所有自定义分类
     * @return 删除的记录数量
     */
    @Query("DELETE FROM categories WHERE isCustom = 1")
    suspend fun deleteCustomCategories(): Int
    
    /**
     * 同步获取所有分类
     */
    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun getAllCategoriesSync(): List<Category>
    
    @Query("SELECT * FROM categories ORDER BY name")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<Category?>
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY level, name")
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE type = :type AND isCustom = :isCustom ORDER BY level, name")
    fun getCategoriesByTypeAndCustom(type: CategoryType, isCustom: Boolean): Flow<List<Category>>
    
    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    fun getCategoryCountByType(type: CategoryType): Flow<Int>
    
    @Query("SELECT * FROM categories WHERE id IN (:ids)")
    suspend fun getCategoriesByIds(ids: List<Long>): List<Category>
    
    // 新增方法：获取顶级分类（没有父分类）
    @Query("SELECT * FROM categories WHERE parentId IS NULL AND type = :type ORDER BY sortOrder, name")
    fun getTopLevelCategories(type: CategoryType): Flow<List<Category>>
    
    // 新增方法：获取指定父分类的子分类
    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY sortOrder, name")
    fun getChildCategories(parentId: Long): Flow<List<Category>>
    
    // 新增方法：获取所有子分类（包括子分类的子分类）
    @Query("SELECT * FROM categories WHERE parentId = :parentId OR id IN (SELECT id FROM categories WHERE parentId IN (SELECT id FROM categories WHERE parentId = :parentId)) ORDER BY level, sortOrder, name")
    fun getAllChildCategories(parentId: Long): Flow<List<Category>>
    
    // 新增方法：获取指定层级的分类
    @Query("SELECT * FROM categories WHERE level = :level AND type = :type ORDER BY sortOrder, name")
    fun getCategoriesByLevel(level: Int, type: CategoryType): Flow<List<Category>>
    
    // 新增方法：检查是否有子分类
    @Query("SELECT COUNT(*) FROM categories WHERE parentId = :categoryId")
    suspend fun hasChildCategories(categoryId: Long): Int
} 