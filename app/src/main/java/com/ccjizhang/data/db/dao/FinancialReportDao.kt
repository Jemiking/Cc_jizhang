package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ccjizhang.data.model.FinancialReport
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 财务报告数据访问对象
 */
@Dao
interface FinancialReportDao {
    
    /**
     * 插入新的财务报告
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(financialReport: FinancialReport): Long
    
    /**
     * 更新财务报告
     */
    @Update
    suspend fun update(financialReport: FinancialReport)
    
    /**
     * 删除财务报告
     */
    @Delete
    suspend fun delete(financialReport: FinancialReport)
    
    /**
     * 根据ID获取财务报告
     */
    @Query("SELECT * FROM financial_reports WHERE id = :id")
    suspend fun getById(id: Long): FinancialReport?
    
    /**
     * 获取所有财务报告
     */
    @Query("SELECT * FROM financial_reports ORDER BY generatedDate DESC")
    fun getAllFinancialReports(): Flow<List<FinancialReport>>
    
    /**
     * 获取指定类型的财务报告
     */
    @Query("SELECT * FROM financial_reports WHERE type = :type ORDER BY generatedDate DESC")
    fun getFinancialReportsByType(type: Int): Flow<List<FinancialReport>>
    
    /**
     * 获取时间范围内的财务报告
     */
    @Query("""
        SELECT * FROM financial_reports 
        WHERE (startDate >= :fromDate OR endDate >= :fromDate) 
        AND (startDate <= :toDate OR endDate <= :toDate) 
        ORDER BY generatedDate DESC
    """)
    fun getFinancialReportsInPeriod(fromDate: Date, toDate: Date): Flow<List<FinancialReport>>
    
    /**
     * 搜索财务报告
     */
    @Query("""
        SELECT * FROM financial_reports 
        WHERE title LIKE '%' || :query || '%' 
        ORDER BY generatedDate DESC
    """)
    fun searchFinancialReports(query: String): Flow<List<FinancialReport>>
    
    /**
     * 获取最近生成的N个财务报告
     */
    @Query("SELECT * FROM financial_reports ORDER BY generatedDate DESC LIMIT :limit")
    fun getRecentFinancialReports(limit: Int): Flow<List<FinancialReport>>
    
    /**
     * 获取指定年份的年度报告
     */
    @Query("""
        SELECT * FROM financial_reports 
        WHERE type = 2 
        AND strftime('%Y', startDate) = :year 
        ORDER BY startDate DESC
    """)
    fun getYearlyReports(year: String): Flow<List<FinancialReport>>
    
    /**
     * 获取指定年份的季度报告
     */
    @Query("""
        SELECT * FROM financial_reports 
        WHERE type = 1 
        AND strftime('%Y', startDate) = :year 
        ORDER BY startDate DESC
    """)
    fun getQuarterlyReports(year: String): Flow<List<FinancialReport>>
    
    /**
     * 获取指定年份和月份的月度报告
     */
    @Query("""
        SELECT * FROM financial_reports 
        WHERE type = 0 
        AND strftime('%Y', startDate) = :year 
        AND strftime('%m', startDate) = :month
        ORDER BY startDate DESC
    """)
    fun getMonthlyReports(year: String, month: String): Flow<List<FinancialReport>>
    
    /**
     * 更新财务报告状态
     */
    @Query("UPDATE financial_reports SET status = :status, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, updateTime: Date = Date())
    
    /**
     * 更新财务报告PDF文件URI
     */
    @Query("UPDATE financial_reports SET pdfUri = :pdfUri, updatedAt = :updateTime WHERE id = :id")
    suspend fun updatePdfUri(id: Long, pdfUri: String?, updateTime: Date = Date())
    
    /**
     * 更新财务报告分享链接
     */
    @Query("UPDATE financial_reports SET shareUrl = :shareUrl, status = 2, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateShareUrl(id: Long, shareUrl: String?, updateTime: Date = Date())
    
    /**
     * 删除特定日期之前的报告
     */
    @Query("DELETE FROM financial_reports WHERE generatedDate < :date")
    suspend fun deleteReportsOlderThan(date: Date)
    
    /**
     * 获取总报告数量
     */
    @Query("SELECT COUNT(*) FROM financial_reports")
    fun getTotalReportCount(): Flow<Int>
} 