package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.model.BudgetCategoryRelation
import com.ccjizhang.data.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 预算分类关系数据访问对象
 */
@Dao
interface BudgetCategoryRelationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relation: BudgetCategoryRelation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relations: List<BudgetCategoryRelation>)
    
    @Delete
    suspend fun delete(relation: BudgetCategoryRelation)
    
    @Query("DELETE FROM budget_category_relations WHERE budgetId = :budgetId")
    suspend fun deleteByBudgetId(budgetId: Long)
    
    @Query("DELETE FROM budget_category_relations WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
    
    @Query("SELECT * FROM budget_category_relations WHERE budgetId = :budgetId")
    fun getRelationsByBudgetId(budgetId: Long): Flow<List<BudgetCategoryRelation>>
    
    @Query("SELECT * FROM budget_category_relations WHERE categoryId = :categoryId")
    fun getRelationsByCategoryId(categoryId: Long): Flow<List<BudgetCategoryRelation>>
    
    /**
     * 获取指定预算的所有分类
     */
    @Transaction
    @Query("SELECT c.* FROM categories c INNER JOIN budget_category_relations bcr ON c.id = bcr.categoryId WHERE bcr.budgetId = :budgetId")
    fun getCategoriesForBudget(budgetId: Long): Flow<List<Category>>
    
    /**
     * 获取包含指定分类的所有预算
     */
    @Transaction
    @Query("SELECT b.* FROM budgets b INNER JOIN budget_category_relations bcr ON b.id = bcr.budgetId WHERE bcr.categoryId = :categoryId")
    fun getBudgetsForCategory(categoryId: Long): Flow<List<Budget>>
} 