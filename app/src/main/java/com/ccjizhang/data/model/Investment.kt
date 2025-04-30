package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import java.util.Date

/**
 * 投资与理财产品实体类
 * 用于记录和跟踪各类理财产品
 */
@Entity(
    tableName = "investments",
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
@TypeConverters(Converters::class)
data class Investment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 投资名称
    val name: String,
    
    // 投资类型：存款(0)、股票(1)、基金(2)、债券(3)、P2P(4)、保险(5)、其他(6)
    val type: Type,
    
    // 初始投资金额
    val initialAmount: Double,
    
    // 当前价值
    val currentValue: Double,
    
    // 累计收益
    val totalReturn: Double = 0.0,
    
    // 关联账户ID
    val accountId: Long? = null,
    
    // 投资机构/平台
    val institution: String? = null,
    
    // 产品代码/编号
    val productCode: String? = null,
    
    // 预期年化收益率（百分比）
    val expectedAnnualReturn: Double? = null,
    
    // 实际年化收益率（百分比）
    val actualAnnualReturn: Double? = null,
    
    // 风险等级：低(0)、中低(1)、中(2)、中高(3)、高(4)
    val riskLevel: Int? = null,
    
    // 开始日期
    val startDate: Date,
    
    // 到期日期（可为空表示无固定期限）
    val endDate: Date? = null,
    
    // 投资状态：活跃(0)、已赎回(1)、已到期(2)、已转出(3)
    val status: Int = 0,
    
    // 赎回日期
    val redemptionDate: Date? = null,
    
    // 最后更新价值的日期
    val lastValueUpdateDate: Date = Date(),
    
    // 自动更新价值的频率（天）
    val autoUpdateFrequencyDays: Int? = null,
    
    // 备注
    val note: String? = null,
    
    // 附件URI列表（JSON格式）
    val attachmentsJson: String? = null,
    
    // 交易记录历史（JSON格式）
    val transactionHistoryJson: String? = null,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 更新时间
    val updatedAt: Date = Date()
) {
    /**
     * 投资类型枚举
     */
    enum class Type {
        STOCK,  // 股票
        FUND,   // 基金
        BOND,   // 债券
        DEPOSIT, // 定期存款
        OTHER   // 其他
    }
    
    /**
     * 风险等级枚举
     */
    enum class Risk {
        LOW,     // 低风险
        MEDIUM,  // 中风险
        HIGH,    // 高风险
        UNKNOWN  // 未知风险
    }
} 