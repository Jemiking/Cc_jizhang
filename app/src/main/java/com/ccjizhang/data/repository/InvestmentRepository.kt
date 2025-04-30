package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.InvestmentDao
import com.ccjizhang.data.model.Investment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 投资与理财产品数据仓库
 * 负责理财产品的管理和收益跟踪
 */
@Singleton
class InvestmentRepository @Inject constructor(
    private val investmentDao: InvestmentDao
) {
    
    /**
     * 获取所有投资记录
     */
    fun getAllInvestments(): Flow<List<Investment>> {
        return investmentDao.getAllInvestments()
    }
    
    /**
     * 获取活跃的投资记录
     */
    fun getActiveInvestments(): Flow<List<Investment>> {
        return investmentDao.getActiveInvestments()
    }
    
    /**
     * 获取已完成的投资记录（已赎回或已到期）
     */
    fun getCompletedInvestments(): Flow<List<Investment>> {
        return investmentDao.getCompletedInvestments()
    }
    
    /**
     * 按投资类型获取投资记录
     */
    fun getInvestmentsByType(type: Int): Flow<List<Investment>> {
        return investmentDao.getInvestmentsByType(type)
    }
    
    /**
     * 按风险等级获取投资记录
     */
    fun getInvestmentsByRiskLevel(riskLevel: Int): Flow<List<Investment>> {
        return investmentDao.getInvestmentsByRiskLevel(riskLevel)
    }
    
    /**
     * 获取指定账户的投资记录
     */
    fun getInvestmentsByAccountId(accountId: Long): Flow<List<Investment>> {
        return investmentDao.getInvestmentsByAccountId(accountId)
    }
    
    /**
     * 获取即将到期的投资记录（30天内）
     */
    fun getUpcomingMaturityInvestments(): Flow<List<Investment>> {
        return investmentDao.getUpcomingMaturityInvestments()
    }
    
    /**
     * 根据ID获取投资记录
     */
    suspend fun getInvestmentById(id: Long): Investment? {
        return investmentDao.getById(id)
    }
    
    /**
     * 添加新的投资记录
     */
    suspend fun addInvestment(investment: Investment): Long {
        return investmentDao.insert(investment)
    }
    
    /**
     * 批量添加投资记录
     */
    suspend fun addInvestments(investments: List<Investment>): List<Long> {
        return investmentDao.insertAll(investments)
    }
    
    /**
     * 更新投资记录
     */
    suspend fun updateInvestment(investment: Investment) {
        investmentDao.update(investment)
    }
    
    /**
     * 删除投资记录
     */
    suspend fun deleteInvestment(investment: Investment) {
        investmentDao.delete(investment)
    }
    
    /**
     * 更新投资价值
     */
    suspend fun updateInvestmentValue(id: Long, newValue: Double) {
        investmentDao.updateValue(id, newValue)
    }
    
    /**
     * 更新投资状态
     * 状态：活跃(0)、已赎回(1)、已到期(2)、已转出(3)
     */
    suspend fun updateInvestmentStatus(id: Long, status: Int) {
        investmentDao.updateStatus(id, status)
    }
    
    /**
     * 获取所有投资的总价值
     */
    fun getTotalInvestmentValue(): Flow<Double?> {
        return investmentDao.getTotalInvestmentValue()
    }
    
    /**
     * 获取所有投资的总收益
     */
    fun getTotalInvestmentReturn(): Flow<Double?> {
        return investmentDao.getTotalInvestmentReturn()
    }
    
    /**
     * 按投资类型获取总价值
     */
    fun getTotalValueByType(type: Int): Flow<Double?> {
        return investmentDao.getTotalValueByType(type)
    }
    
    /**
     * 处理需要自动更新价值的投资
     */
    suspend fun processAutoValueUpdates() {
        val investments = investmentDao.getInvestmentsNeedingValueUpdate()
        
        investments.forEach { investment ->
            // 这里仅是示例，实际应用中，应该基于市场数据或外部API获取更新的价值
            // 这里我们简单地假设一个微小的价值增长
            val randomFactor = 1.0 + (Math.random() * 0.02 - 0.01) // -1% 到 +1% 的随机波动
            val newValue = investment.currentValue * randomFactor
            
            // 更新投资价值
            investmentDao.updateValue(investment.id, newValue)
        }
    }
    
    /**
     * 计算投资的年化收益率
     * (当前价值 - 初始金额) / 初始金额 / 年份
     */
    fun calculateAnnualReturn(investment: Investment): Double {
        val initialAmount = investment.initialAmount
        if (initialAmount <= 0) return 0.0
        
        val currentValue = investment.currentValue
        val startDate = investment.startDate.time
        val currentDate = Date().time
        
        // 计算投资的年份（使用毫秒转换为年）
        val years = (currentDate - startDate) / (1000.0 * 60 * 60 * 24 * 365)
        if (years <= 0) return 0.0
        
        // 计算年化收益率
        return (currentValue - initialAmount) / initialAmount / years
    }
    
    /**
     * 分析投资组合风险分布
     * 返回各风险等级的投资占比 Map<风险等级, 占比百分比>
     */
    suspend fun analyzeRiskDistribution(): Map<Int, Double> {
        val totalValueFlow = investmentDao.getTotalInvestmentValue()
        val totalValue = totalValueFlow.first() ?: 0.0
        
        if (totalValue <= 0) return emptyMap()
        
        val result = mutableMapOf<Int, Double>()
        
        for (riskLevel in 0..4) { // 从低风险(0)到高风险(4)
            val investments = investmentDao.getInvestmentsByRiskLevel(riskLevel).first()
            val sumForRiskLevel = investments.sumOf { it.currentValue }
            result[riskLevel] = (sumForRiskLevel / totalValue) * 100.0
        }
        
        return result
    }
} 