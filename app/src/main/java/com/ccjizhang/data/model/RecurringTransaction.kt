package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import java.util.Date

/**
 * 定期交易频率枚举
 */
enum class RecurringTransactionFrequency {
    DAILY,  // 每日
    WEEKLY, // 每周
    MONTHLY, // 每月
    YEARLY  // 每年
}

/**
 * 定期交易实体类
 * 用于设置周期性、自动创建的交易记录
 */
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("fromAccountId"),
        Index("toAccountId")
    ]
)
@TypeConverters(Converters::class)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 交易类型：支出(0)、收入(1)、转账(2)
    val type: Int,
    
    // 金额
    val amount: Double,
    
    // 交易描述
    val description: String,
    
    // 分类ID
    val categoryId: Long? = null,
    
    // 来源账户ID
    val fromAccountId: Long,
    
    // 目标账户ID（仅转账类型需要）
    val toAccountId: Long? = null,
    
    // 首次执行日期
    val firstExecutionDate: Date,
    
    // 结束日期（可为空表示无限期）
    val endDate: Date? = null,
    
    // 重复类型：每天(0)、每周(1)、每两周(2)、每月(3)、每季度(4)、每年(5)、自定义天数(6)
    val recurrenceType: Int,
    
    // 自定义重复天数（当recurrenceType为6时使用）
    val customRecurrenceDays: Int? = null,
    
    // 特定重复日（月度重复时：1-31表示每月几号；年度重复时：格式为"MM-dd"）
    val specificRecurrenceDay: String? = null,
    
    // 每周重复的星期几，使用星期的位掩码表示 (1:周日, 2:周一, 4:周二, ...)
    val weekdayMask: Int? = null,
    
    // 最后执行日期
    val lastExecutionDate: Date? = null,
    
    // 下次执行日期
    val nextExecutionDate: Date,
    
    // 总执行次数
    val totalExecutions: Int = 0,
    
    // 最大执行次数（0表示无限次）
    val maxExecutions: Int = 0,
    
    // 状态：活跃(0)、暂停(1)、已完成(2)
    val status: Int = 0,
    
    // 备注
    val note: String? = null,
    
    // 是否提前通知
    val notifyBeforeExecution: Boolean = false,
    
    // 提前通知天数
    val notifyDaysBefore: Int? = null,
    
    // 模板数据的JSON字符串（存储交易的附加信息）
    val templateDataJson: String? = null,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 更新时间
    val updatedAt: Date = Date(),
    
    // 显示用字段，不存储在数据库中
    val accountName: String = "",
    val categoryName: String = "",
    val formattedRecurrencePattern: String = ""
) 