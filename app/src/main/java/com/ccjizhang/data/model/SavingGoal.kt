package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 储蓄目标实体类
 * 用于记录用户的储蓄计划和进度跟踪
 */
@Entity(
    tableName = "saving_goals",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId")
    ]
)
data class SavingGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 目标名称
    val name: String,
    
    // 目标金额
    val targetAmount: Double,
    
    // 当前已存金额
    val currentAmount: Double = 0.0,
    
    // 关联账户ID（可选）
    val accountId: Long? = null,
    
    // 目标开始日期
    val startDate: Date = Date(),
    
    // 目标结束日期
    val targetDate: Date,
    
    // 优先级（1-5，5为最高）
    val priority: Int = 3,
    
    // 图标（资源ID或URI）
    val iconUri: String? = null,
    
    // 目标颜色
    val color: Int = 0xFF2196F3.toInt(), // 默认蓝色
    
    // 备注
    val note: String? = null,
    
    // 自动存款金额（若设置了定期存款）
    val autoSaveAmount: Double? = null,
    
    // 自动存款频率（天）
    val autoSaveFrequencyDays: Int? = null,
    
    // 最后一次自动存款日期
    val lastAutoSaveDate: Date? = null,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 最后更新时间
    val updatedAt: Date = Date()
) 