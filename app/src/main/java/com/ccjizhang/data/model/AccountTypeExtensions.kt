package com.ccjizhang.data.model

/**
 * AccountType 的扩展属性和方法
 */

/**
 * 获取账户类型的显示名称
 */
val AccountType.displayName: String
    get() = when (this) {
        AccountType.CASH -> "现金"
        AccountType.CREDIT_CARD -> "信用卡"
        AccountType.DEBIT_CARD -> "借记卡"
        AccountType.ALIPAY -> "支付宝"
        AccountType.WECHAT -> "微信"
        AccountType.OTHER -> "其他"
    }
