package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import java.util.Date

/**
 * 交易类型枚举
 */
enum class TransactionType {
    EXPENSE, // 支出
    INCOME,  // 收入
    TRANSFER // 转账
}

/**
 * 交易数据实体
 *
 * @param id 交易ID
 * @param amount 交易金额，正数表示收入，负数表示支出
 * @param categoryId 分类ID
 * @param accountId 账户ID
 * @param date 交易日期
 * @param note 备注
 * @param isIncome 是否为收入
 * @param location 位置信息
 * @param imageUri 图片URI
 *
 * 注意：标签已移至TransactionTag表
 */
@Entity(
    tableName = "transactions",
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
            childColumns = ["accountId"],
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
        Index("accountId"),
        Index("date"),
        Index("toAccountId"),
        // 复合索引，优化常用查询
        Index(value = ["isIncome", "date"]),
        Index(value = ["accountId", "date"]),
        Index(value = ["categoryId", "date"]),
        Index(value = ["accountId", "isIncome", "date"])
    ]
)
@TypeConverters(Converters::class)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val amount: Double,            // 金额
    val categoryId: Long?,         // 分类ID
    val accountId: Long,           // 账户ID
    val date: Date,                // 日期
    val note: String = "",         // 备注信息
    val isIncome: Boolean = false, // 是否为收入

    // 可选字段
    val location: String = "",     // 位置信息
    val imageUri: String = "",     // 图片URI
    val toAccountId: Long? = null, // 转账目标账户ID

    // 安全相关字段
    val createdBy: Long = 0L,      // 创建者ID
    val createdAt: Date = Date(),  // 创建时间
    val updatedAt: Date = Date(),  // 更新时间
    val isPrivate: Boolean = false // 是否私密
)