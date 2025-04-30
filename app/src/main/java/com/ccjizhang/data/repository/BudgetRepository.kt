package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.BudgetDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * 预算数据仓库
 * 负责提供预算数据和处理业务逻辑
 */
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    // 获取所有预算
    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAllBudgets()
    
    // 按ID获取预算
    fun getBudgetById(id: Long): Flow<Budget?> = budgetDao.getBudgetById(id)
    
    // 获取激活的预算
    fun getActiveBudgets(): Flow<List<Budget>> = budgetDao.getActiveBudgets()
    
    // 获取当前日期的预算
    fun getCurrentBudgets(): Flow<List<Budget>> = 
        budgetDao.getBudgetsByDate(Date())
    
    // 获取启用通知的预算
    fun getBudgetsWithNotifications(): Flow<List<Budget>> = 
        budgetDao.getBudgetsWithNotifications()
    
    // 添加预算
    suspend fun addBudget(budget: Budget): Long {
        return budgetDao.insert(budget)
    }
    
    // 更新预算
    suspend fun updateBudget(budget: Budget) {
        budgetDao.update(budget)
    }
    
    // 删除预算
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget)
    }
    
    // 按ID删除预算
    suspend fun deleteBudgetById(id: Long) {
        budgetDao.deleteById(id)
    }
    
    // 获取预算使用情况
    suspend fun getBudgetUsage(budgetId: Long): Pair<Double, Double> {
        val budget = budgetDao.getBudgetById(budgetId).first() ?: return Pair(0.0, 0.0)
        
        // 预算总额
        val totalAmount = budget.amount
        
        // 已使用金额
        var usedAmount = 0.0
        
        if (budget.categories.isEmpty()) {
            // 总体预算
            val transactions = transactionDao.getTransactionsByDateRange(
                budget.startDate,
                budget.endDate
            ).first()
            
            // 计算支出总额
            usedAmount = transactions
                .filter { !it.isIncome }
                .sumOf { it.amount }
        } else {
            // 分类预算
            for (categoryId in budget.categories) {
                val categoryAmount = transactionDao.getAmountByCategoryAndDateRange(
                    categoryId,
                    budget.startDate,
                    budget.endDate
                ).first() ?: 0.0
                
                usedAmount += categoryAmount.absoluteValue
            }
        }
        
        return Pair(usedAmount, totalAmount)
    }
    
    // 获取预算使用百分比
    suspend fun getBudgetUsagePercentage(budgetId: Long): Float {
        val (used, total) = getBudgetUsage(budgetId)
        return if (total > 0) (used / total).toFloat() else 0f
    }
    
    // 检查预算是否超支
    suspend fun isBudgetOverspent(budgetId: Long): Boolean {
        val (used, total) = getBudgetUsage(budgetId)
        return used > total
    }
    
    // 获取特定分类的预算使用情况
    suspend fun getBudgetUsageForCategory(budgetId: Long, categoryId: Long): Pair<Double, Double> {
        val budget = budgetDao.getBudgetById(budgetId).first() ?: return Pair(0.0, 0.0)
        
        // 如果预算不包含此分类，返回0
        if (!budget.categories.contains(categoryId)) {
            return Pair(0.0, 0.0)
        }
        
        // 计算该分类的使用金额
        val usedAmount = transactionDao.getAmountByCategoryAndDateRange(
            categoryId,
            budget.startDate,
            budget.endDate
        ).first() ?: 0.0
        
        // 计算该分类的预算金额（平均分配给每个分类）
        val categoryBudget = budget.amount / budget.categories.size
        
        return Pair(usedAmount.absoluteValue, categoryBudget)
    }
    
    // 检查是否有需要通知的预算
    suspend fun checkBudgetsForNotification(): List<Budget> {
        val budgetsWithNotification = budgetDao.getBudgetsWithNotifications().first()
        val budgetsToNotify = mutableListOf<Budget>()
        
        for (budget in budgetsWithNotification) {
            val usagePercentage = getBudgetUsagePercentage(budget.id) * 100
            
            // 如果使用百分比超过通知阈值，需要通知
            if (usagePercentage >= budget.notifyThreshold) {
                budgetsToNotify.add(budget)
            }
        }
        
        return budgetsToNotify
    }
} 