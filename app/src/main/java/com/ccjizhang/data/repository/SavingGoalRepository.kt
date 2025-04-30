package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.SavingGoalDao
import com.ccjizhang.data.model.SavingGoal
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 储蓄目标数据仓库
 * 负责储蓄目标数据的管理和业务逻辑
 */
@Singleton
class SavingGoalRepository @Inject constructor(
    private val savingGoalDao: SavingGoalDao
) {
    
    /**
     * 获取所有储蓄目标
     */
    fun getAllSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getAllSavingGoals()
    }
    
    /**
     * 获取进行中的储蓄目标
     */
    fun getActiveSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getActiveSavingGoals()
    }
    
    /**
     * 获取已完成的储蓄目标
     */
    fun getCompletedSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getCompletedSavingGoals()
    }
    
    /**
     * 获取过期未完成的储蓄目标
     */
    fun getExpiredSavingGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getExpiredSavingGoals()
    }
    
    /**
     * 获取指定账户的储蓄目标
     */
    fun getSavingGoalsByAccountId(accountId: Long): Flow<List<SavingGoal>> {
        return savingGoalDao.getSavingGoalsByAccountId(accountId)
    }
    
    /**
     * 获取即将完成的储蓄目标（达到90%以上）
     */
    fun getNearCompletionGoals(): Flow<List<SavingGoal>> {
        return savingGoalDao.getNearCompletionGoals()
    }
    
    /**
     * 根据ID获取储蓄目标
     */
    suspend fun getSavingGoalById(id: Long): SavingGoal? {
        return savingGoalDao.getById(id)
    }
    
    /**
     * 添加新的储蓄目标
     */
    suspend fun addSavingGoal(savingGoal: SavingGoal): Long {
        return savingGoalDao.insert(savingGoal)
    }
    
    /**
     * 更新储蓄目标
     */
    suspend fun updateSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.update(savingGoal)
    }
    
    /**
     * 删除储蓄目标
     */
    suspend fun deleteSavingGoal(savingGoal: SavingGoal) {
        savingGoalDao.delete(savingGoal)
    }
    
    /**
     * 向储蓄目标存入资金
     */
    suspend fun depositToGoal(goalId: Long, amount: Double) {
        savingGoalDao.deposit(goalId, amount)
    }
    
    /**
     * 从储蓄目标取出资金
     */
    suspend fun withdrawFromGoal(goalId: Long, amount: Double) {
        savingGoalDao.withdraw(goalId, amount)
    }
    
    /**
     * 处理需要自动存款的目标
     */
    suspend fun processAutoSaveGoals() {
        val goalsForAutoSave = savingGoalDao.findGoalsForAutoSave()
        val currentDate = Date()
        
        goalsForAutoSave.forEach { goal ->
            goal.autoSaveAmount?.let { amount ->
                // 执行自动存款
                savingGoalDao.deposit(goal.id, amount, currentDate)
                // 更新最后存款日期
                savingGoalDao.updateLastAutoSaveDate(goal.id, currentDate)
            }
        }
    }
    
    /**
     * 计算储蓄目标的完成进度百分比
     */
    fun calculateGoalProgress(goal: SavingGoal): Float {
        if (goal.targetAmount <= 0) return 0f
        return (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }
    
    /**
     * 计算储蓄目标的时间进度百分比
     */
    fun calculateTimeProgress(goal: SavingGoal): Float {
        val now = Date().time
        val start = goal.startDate.time
        val end = goal.targetDate.time
        
        if (end <= start) return 1f
        
        return ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f)
    }
    
    /**
     * 检查储蓄目标是否已完成
     */
    fun isGoalCompleted(goal: SavingGoal): Boolean {
        return goal.currentAmount >= goal.targetAmount
    }
    
    /**
     * 检查储蓄目标是否已过期
     */
    fun isGoalExpired(goal: SavingGoal): Boolean {
        return !isGoalCompleted(goal) && goal.targetDate.before(Date())
    }
} 