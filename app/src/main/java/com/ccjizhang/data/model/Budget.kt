package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import java.util.Date

/**
 * 预算周期枚举
 */
enum class BudgetPeriod {
    DAILY,   // 每日
    WEEKLY,  // 每周
    MONTHLY, // 每月
    YEARLY   // 每年
}

/**
 * 预算数据实体
 *
 * @param id 预算ID
 * @param name 预算名称
 * @param amount 预算金额
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param period 预算周期，如"日"、"周"、"月"、"季"、"年"
 * @param categories 预算关联的分类ID列表
 * @param isActive 是否激活
 * @param notifyEnabled 是否启用通知
 * @param notifyThreshold 通知阈值百分比（例如80表示使用80%时通知）
 *
 * 注意：分类关联已移至BudgetCategoryRelation表
 */
@Entity(tableName = "budgets")
@TypeConverters(Converters::class)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val amount: Double,
    val startDate: Date,
    val endDate: Date,
    val period: String = "月",
    
    // 关联的分类ID列表
    val categories: List<Long> = emptyList(),
    
    // 预算状态
    val isActive: Boolean = true,
    
    // 通知相关
    val notifyEnabled: Boolean = false,
    val notifyThreshold: Int = 80 // 默认在达到80%时通知
) 