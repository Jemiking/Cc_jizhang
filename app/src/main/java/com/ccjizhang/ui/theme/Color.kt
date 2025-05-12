package com.ccjizhang.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

// 主要品牌颜色
val Purple80 = Color(0xFF9575CD)
val PurpleGrey80 = Color(0xFFB39DDB)
val Pink80 = Color(0xFFEF9A9A)

val Purple40 = Color(0xFF4E2A84) // 主品牌颜色，紫色
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 自定义颜色
val Primary = Color(0xFF2196F3) // 蓝色，与截图中的APP状态栏颜色一致
val PrimaryDark = Color(0xFF4E2A84)
val PrimaryLight = Color(0xFFB39DDB)
val Secondary = Color(0xFF43A047) // 绿色

// 状态颜色
val ExpenseRed = Color(0xFFE53935) // 支出红色
val IncomeGreen = Color(0xFF43A047) // 收入绿色
val WarningYellow = Color(0xFFFFC107) // 警告黄色
val NeutralGrey = Color(0xFF757575) // 中性灰色

// 分类颜色
val CategoryBlue = Color(0xFF1976D2) // 蓝色
val CategoryIndigo = Color(0xFF5C6BC0) // 靛蓝色
val CategoryViolet = Color(0xFF8E24AA) // 紫罗兰色
val CategoryOrange = Color(0xFFFB8C00) // 橙色
val CategoryPink = Color(0xFFEC407A) // 粉色

// 背景颜色
val Background = Color(0xFFF8F9FA)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1F1F1F)

// 文本颜色
val TextPrimary = Color(0xFF1F1F1F)
val TextSecondary = Color(0xFF757575)
val TextDisabled = Color(0xFFBDBDBD)

/**
 * 计算颜色的亮度值，亮度范围为 0.0-1.0
 * 值越大表示颜色越亮
 */
fun Color.luminance(): Float {
    val colorInLinearSpace = convert(ColorSpaces.LinearSrgb)
    return (0.2126f * colorInLinearSpace.red +
            0.7152f * colorInLinearSpace.green +
            0.0722f * colorInLinearSpace.blue).coerceIn(0f, 1f)
}