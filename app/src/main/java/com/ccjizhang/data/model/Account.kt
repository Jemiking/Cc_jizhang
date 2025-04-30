package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 账户类型枚举
 */
enum class AccountType {
    CASH,        // 现金
    CREDIT_CARD, // 信用卡
    DEBIT_CARD,  // 借记卡
    ALIPAY,      // 支付宝
    WECHAT,      // 微信
    OTHER        // 其他
}

/**
 * 信用卡账单日期类型
 */
enum class BillingCycleType {
    FIXED_DAY,   // 固定日期（每月X日）
    CUSTOM       // 自定义周期
}

/**
 * 币种枚举
 */
enum class Currency(val code: String, val symbol: String) {
    CNY("CNY", "¥"),    // 人民币
    USD("USD", "$"),    // 美元
    EUR("EUR", "€"),    // 欧元
    GBP("GBP", "£"),    // 英镑
    JPY("JPY", "¥"),    // 日元
    HKD("HKD", "HK$"),  // 港币
    KRW("KRW", "₩"),    // 韩元
    CAD("CAD", "C$"),   // 加元
    AUD("AUD", "A$"),   // 澳元
    SGD("SGD", "S$")    // 新加坡元
}

/**
 * 账户实体类
 *
 * @param id 账户ID
 * @param name 账户名称
 * @param type 账户类型
 * @param balance 账户余额
 * @param currency 账户币种
 * @param exchangeRate 与基准币种的汇率
 * @param color 账户颜色（用于UI显示）
 * @param icon 账户图标名称（用于UI显示）
 * @param isDefault 是否为默认账户
 * @param includeInTotal 是否包含在总资产计算中
 * @param creditLimit 信用额度（仅信用卡类型适用）
 * @param billingDay 账单日（仅信用卡类型适用）
 * @param dueDay 还款日（仅信用卡类型适用）
 * @param nextBillingDate 下一个账单日期（仅信用卡类型适用）
 * @param nextDueDate 下一个还款日期（仅信用卡类型适用）
 */
@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("type"),
        Index("isDefault"),
        Index("includeInTotal"),
        Index("categoryId"),
        Index("displayOrder"),
        // 复合索引
        Index(value = ["type", "isDefault"]),
        Index(value = ["currency", "includeInTotal"]),
        Index(value = ["categoryId", "displayOrder"])
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val type: AccountType = AccountType.OTHER,
    val balance: Double = 0.0,

    // 币种相关
    val currency: Currency = Currency.CNY,
    val exchangeRate: Double = 1.0, // 相对于基准币种的汇率

    // UI显示信息
    val color: Int = 0,
    val icon: String = "",

    // 设置
    val isDefault: Boolean = false,
    val includeInTotal: Boolean = true,

    // 信用卡特有属性
    val creditLimit: Double = 0.0,         // 信用额度
    val billingDay: Int = 1,               // 账单日（1-31）
    val dueDay: Int = 15,                  // 还款日（1-31）
    val billingCycleType: BillingCycleType = BillingCycleType.FIXED_DAY, // 账单周期类型
    val nextBillingDate: Date? = null,    // 下一个账单日期
    val nextDueDate: Date? = null,        // 下一个还款日期

    // 分类和排序
    val categoryId: Long? = null,         // 账户分类ID
    val displayOrder: Int = 0             // 显示顺序
)