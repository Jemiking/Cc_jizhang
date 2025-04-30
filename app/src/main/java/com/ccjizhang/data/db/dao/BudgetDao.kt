package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ccjizhang.data.db.relations.BudgetWithCategories
import com.ccjizhang.data.model.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 预算数据访问对象
 */
@Dao
interface BudgetDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<Budget>)
    
    @Update
    suspend fun update(budget: Budget)
    
    @Delete
    suspend fun delete(budget: Budget)
    
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 删除所有预算
     * @return 删除的记录数量
     */
    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets(): Int
    
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    fun getBudgetById(id: Long): Flow<Budget?>
    
    @Query("SELECT * FROM budgets WHERE isActive = 1")
    fun getActiveBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE startDate <= :date AND endDate >= :date AND isActive = 1")
    fun getBudgetsByDate(date: Date): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE notifyEnabled = 1")
    fun getBudgetsWithNotifications(): Flow<List<Budget>>
    
    /**
     * 获取带有分类关联的预算
     */
    @Transaction
    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun getBudgetWithCategories(budgetId: Long): Flow<BudgetWithCategories?>
    
    /**
     * 获取所有带有分类关联的预算
     */
    @Transaction
    @Query("SELECT * FROM budgets")
    fun getAllBudgetsWithCategories(): Flow<List<BudgetWithCategories>>
    
    /**
     * 获取当前活跃的带有分类关联的预算
     */
    @Transaction
    @Query("SELECT * FROM budgets WHERE isActive = 1")
    fun getActiveBudgetsWithCategories(): Flow<List<BudgetWithCategories>>
} 