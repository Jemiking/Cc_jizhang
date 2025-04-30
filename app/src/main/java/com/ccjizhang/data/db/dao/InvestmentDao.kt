package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ccjizhang.data.model.Investment
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 投资与理财产品数据访问对象
 */
@Dao
interface InvestmentDao {
    
    /**
     * 插入新的投资记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investment: Investment): Long
    
    /**
     * 批量插入投资记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(investments: List<Investment>): List<Long>
    
    /**
     * 更新投资记录
     */
    @Update
    suspend fun update(investment: Investment)
    
    /**
     * 删除投资记录
     */
    @Delete
    suspend fun delete(investment: Investment)
    
    /**
     * 根据ID获取投资记录
     */
    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getById(id: Long): Investment?
    
    /**
     * 获取所有投资记录
     */
    @Query("SELECT * FROM investments ORDER BY currentValue DESC")
    fun getAllInvestments(): Flow<List<Investment>>
    
    /**
     * 获取活跃的投资记录
     */
    @Query("SELECT * FROM investments WHERE status = 0 ORDER BY currentValue DESC")
    fun getActiveInvestments(): Flow<List<Investment>>
    
    /**
     * 获取已赎回或已到期的投资记录
     */
    @Query("SELECT * FROM investments WHERE status IN (1, 2) ORDER BY redemptionDate DESC, endDate DESC")
    fun getCompletedInvestments(): Flow<List<Investment>>
    
    /**
     * 按投资类型获取投资记录
     */
    @Query("SELECT * FROM investments WHERE type = :type ORDER BY currentValue DESC")
    fun getInvestmentsByType(type: Int): Flow<List<Investment>>
    
    /**
     * 按风险等级获取投资记录
     */
    @Query("SELECT * FROM investments WHERE riskLevel = :riskLevel ORDER BY currentValue DESC")
    fun getInvestmentsByRiskLevel(riskLevel: Int): Flow<List<Investment>>
    
    /**
     * 获取指定账户的投资记录
     */
    @Query("SELECT * FROM investments WHERE accountId = :accountId ORDER BY currentValue DESC")
    fun getInvestmentsByAccountId(accountId: Long): Flow<List<Investment>>
    
    /**
     * 获取即将到期的投资记录（30天内）
     */
    @Query("""
        SELECT * FROM investments 
        WHERE status = 0 
        AND endDate IS NOT NULL 
        AND date(endDate) <= date(:currentDate, '+30 days') 
        AND date(endDate) >= date(:currentDate)
        ORDER BY endDate ASC
    """)
    fun getUpcomingMaturityInvestments(currentDate: Date = Date()): Flow<List<Investment>>
    
    /**
     * 获取需要更新价值的投资记录
     */
    @Query("""
        SELECT * FROM investments 
        WHERE status = 0 
        AND autoUpdateFrequencyDays IS NOT NULL 
        AND date(lastValueUpdateDate, '+' || autoUpdateFrequencyDays || ' days') <= date(:currentDate)
    """)
    suspend fun getInvestmentsNeedingValueUpdate(currentDate: Date = Date()): List<Investment>
    
    /**
     * 更新投资价值
     */
    @Transaction
    suspend fun updateValue(id: Long, newValue: Double, updateTime: Date = Date()) {
        val investment = getById(id) ?: return
        
        // 计算收益变化
        val gainChange = newValue - investment.currentValue
        val newTotalReturn = investment.totalReturn + gainChange
        
        // 更新投资记录
        updateValueAndReturn(
            id = id,
            currentValue = newValue,
            totalReturn = newTotalReturn,
            lastValueUpdateDate = updateTime,
            updatedAt = updateTime
        )
    }
    
    /**
     * 更新投资价值和收益
     */
    @Query("""
        UPDATE investments 
        SET currentValue = :currentValue, 
            totalReturn = :totalReturn, 
            lastValueUpdateDate = :lastValueUpdateDate,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateValueAndReturn(
        id: Long,
        currentValue: Double,
        totalReturn: Double,
        lastValueUpdateDate: Date,
        updatedAt: Date
    )
    
    /**
     * 更新投资状态
     */
    @Query("""
        UPDATE investments 
        SET status = :status, 
            redemptionDate = CASE WHEN :status IN (1, 2) THEN :updateDate ELSE redemptionDate END,
            updatedAt = :updateDate 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: Int, updateDate: Date = Date())
    
    /**
     * 获取所有投资的总价值
     */
    @Query("SELECT SUM(currentValue) FROM investments WHERE status = 0")
    fun getTotalInvestmentValue(): Flow<Double?>
    
    /**
     * 获取所有投资的总收益
     */
    @Query("SELECT SUM(totalReturn) FROM investments")
    fun getTotalInvestmentReturn(): Flow<Double?>
    
    /**
     * 按投资类型获取总价值
     */
    @Query("SELECT SUM(currentValue) FROM investments WHERE type = :type AND status = 0")
    fun getTotalValueByType(type: Int): Flow<Double?>
} 