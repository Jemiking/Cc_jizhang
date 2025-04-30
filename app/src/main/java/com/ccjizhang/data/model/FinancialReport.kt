package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import java.util.Date

/**
 * 报告周期枚举
 */
enum class Period {
    MONTHLY,  // 月度报告
    QUARTERLY, // 季度报告
    YEARLY,    // 年度报告
    CUSTOM     // 自定义周期
}

/**
 * 财务报告实体类
 * 用于生成和存储用户的财务状况报告
 */
@Entity(tableName = "financial_reports")
@TypeConverters(Converters::class)
data class FinancialReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 报告标题
    val title: String,
    
    // 报告类型：月度(0)、季度(1)、年度(2)、自定义(3)
    val type: Int,
    
    // 报告开始日期
    val startDate: Date,
    
    // 报告结束日期
    val endDate: Date,
    
    // 报告生成日期
    val generatedDate: Date = Date(),
    
    // 总收入
    val totalIncome: Double = 0.0,
    
    // 总支出
    val totalExpense: Double = 0.0,
    
    // 净收支
    val netCashflow: Double = 0.0,
    
    // 储蓄率
    val savingRate: Double? = null,
    
    // 期初总资产
    val initialTotalAssets: Double? = null,
    
    // 期末总资产
    val finalTotalAssets: Double? = null,
    
    // 资产增长率
    val assetGrowthRate: Double? = null,
    
    // 报告数据（JSON格式，包含图表数据、详细分类统计等）
    val reportDataJson: String,
    
    // 报告配置（JSON格式，包含显示设置、个性化选项等）
    val configJson: String? = null,
    
    // 报告PDF文件URI
    val pdfUri: String? = null,
    
    // 报告分享链接
    val shareUrl: String? = null,
    
    // 报告状态：草稿(0)、已完成(1)、已分享(2)
    val status: Int = 1,
    
    // 备注
    val note: String? = null,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 更新时间
    val updatedAt: Date = Date(),
    
    // 报告内容标记
    val includeIncomeAnalysis: Boolean = true,
    val includeExpenseAnalysis: Boolean = true,
    val includeCategoryBreakdown: Boolean = true,
    val includeAccountBalances: Boolean = true,
    val includeBudgetComparison: Boolean = false,
    val includeFinancialHealth: Boolean = true,
    
    // 报告数据
    val netIncome: Double = 0.0,
    val savingsRate: Double = 0.0,
    
    // 保存的JSON数据
    val incomeAnalysisJson: String = "{}",
    val expenseAnalysisJson: String = "{}",
    val accountBalancesJson: String = "{}",
    val budgetComparisonJson: String = "{}",
    val financialHealthJson: String = "{}"
) 