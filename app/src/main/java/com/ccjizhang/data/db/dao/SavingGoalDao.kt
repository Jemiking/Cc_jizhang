package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ccjizhang.data.model.SavingGoal
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 储蓄目标数据访问对象
 */
@Dao
interface SavingGoalDao {
    
    /**
     * 插入新的储蓄目标
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savingGoal: SavingGoal): Long
    
    /**
     * 更新储蓄目标
     */
    @Update
    suspend fun update(savingGoal: SavingGoal)
    
    /**
     * 删除储蓄目标
     */
    @Delete
    suspend fun delete(savingGoal: SavingGoal)
    
    /**
     * 根据ID获取单个储蓄目标
     */
    @Query("SELECT * FROM saving_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingGoal?
    
    /**
     * 获取所有储蓄目标，按优先级和创建时间排序
     */
    @Query("SELECT * FROM saving_goals ORDER BY priority DESC, createdAt ASC")
    fun getAllSavingGoals(): Flow<List<SavingGoal>>
    
    /**
     * 获取进行中的储蓄目标
     */
    @Query("SELECT * FROM saving_goals WHERE currentAmount < targetAmount AND (targetDate >= :currentDate) ORDER BY priority DESC")
    fun getActiveSavingGoals(currentDate: Date = Date()): Flow<List<SavingGoal>>
    
    /**
     * 获取已完成的储蓄目标
     */
    @Query("SELECT * FROM saving_goals WHERE currentAmount >= targetAmount ORDER BY updatedAt DESC")
    fun getCompletedSavingGoals(): Flow<List<SavingGoal>>
    
    /**
     * 获取过期未完成的储蓄目标
     */
    @Query("SELECT * FROM saving_goals WHERE currentAmount < targetAmount AND targetDate < :currentDate ORDER BY targetDate ASC")
    fun getExpiredSavingGoals(currentDate: Date = Date()): Flow<List<SavingGoal>>
    
    /**
     * 获取指定账户的储蓄目标
     */
    @Query("SELECT * FROM saving_goals WHERE accountId = :accountId ORDER BY priority DESC")
    fun getSavingGoalsByAccountId(accountId: Long): Flow<List<SavingGoal>>
    
    /**
     * 更新储蓄目标的当前金额
     */
    @Query("UPDATE saving_goals SET currentAmount = :newAmount, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateCurrentAmount(id: Long, newAmount: Double, updateTime: Date = Date())
    
    /**
     * 增加储蓄目标的当前金额（存入）
     */
    @Transaction
    suspend fun deposit(id: Long, amount: Double, updateTime: Date = Date()) {
        val goal = getById(id) ?: return
        val newAmount = goal.currentAmount + amount
        updateCurrentAmount(id, newAmount, updateTime)
    }
    
    /**
     * 减少储蓄目标的当前金额（取出）
     */
    @Transaction
    suspend fun withdraw(id: Long, amount: Double, updateTime: Date = Date()) {
        val goal = getById(id) ?: return
        val newAmount = (goal.currentAmount - amount).coerceAtLeast(0.0)
        updateCurrentAmount(id, newAmount, updateTime)
    }
    
    /**
     * 查找需要自动存款的储蓄目标
     */
    @Query("""
        SELECT * FROM saving_goals 
        WHERE autoSaveAmount IS NOT NULL 
        AND autoSaveFrequencyDays IS NOT NULL 
        AND (lastAutoSaveDate IS NULL OR date(lastAutoSaveDate, '+' || autoSaveFrequencyDays || ' days') <= date(:currentDate))
        AND currentAmount < targetAmount
        AND (targetDate >= :currentDate)
    """)
    suspend fun findGoalsForAutoSave(currentDate: Date = Date()): List<SavingGoal>
    
    /**
     * 更新最后自动存款日期
     */
    @Query("UPDATE saving_goals SET lastAutoSaveDate = :date, updatedAt = :date WHERE id = :id")
    suspend fun updateLastAutoSaveDate(id: Long, date: Date = Date())
    
    /**
     * 获取即将完成的储蓄目标（达到90%以上）
     */
    @Query("SELECT * FROM saving_goals WHERE currentAmount >= (targetAmount * 0.9) AND currentAmount < targetAmount ORDER BY (currentAmount / targetAmount) DESC")
    fun getNearCompletionGoals(): Flow<List<SavingGoal>>
} 