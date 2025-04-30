package com.ccjizhang.util

import java.text.NumberFormat
import java.util.*

/**
 * 将Double格式化为货币字符串
 */
fun Double.formatCurrency(locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    return format.format(this)
}

/**
 * 将数字格式化为百分比
 */
fun Double.formatPercent(fractionDigits: Int = 1, locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getPercentInstance(locale)
    format.maximumFractionDigits = fractionDigits
    return format.format(this)
}

/**
 * 将数字格式化为指定小数位的字符串
 */
fun Double.formatNumber(fractionDigits: Int = 2, locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getNumberInstance(locale)
    format.maximumFractionDigits = fractionDigits
    return format.format(this)
}

/**
 * 将金额转换为带单位的简短形式
 * 例如: 1234567.89 -> 123.5万
 */
fun Double.formatCompactCurrency(locale: Locale = Locale.getDefault()): String {
    return when {
        this >= 100_000_000 -> "${(this / 100_000_000).formatNumber(1)}亿"
        this >= 10_000 -> "${(this / 10_000).formatNumber(1)}万"
        else -> this.formatCurrency(locale)
    }
} 