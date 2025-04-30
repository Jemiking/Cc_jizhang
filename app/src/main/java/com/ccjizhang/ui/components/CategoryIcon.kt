package com.ccjizhang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 分类图标组件
 * 根据分类ID显示对应的图标
 *
 * @param categoryId 分类ID
 * @param tint 图标颜色
 * @param backgroundColor 背景颜色
 * @param size 图标大小
 */
@Composable
fun CategoryIcon(
    categoryId: Long,
    tint: Color = Color.White,
    backgroundColor: Color = Color.Transparent,
    size: Dp = 40.dp
) {
    val icon = getCategoryIcon(categoryId)
    
    Box(
        modifier = Modifier
            .size(size)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * 根据分类ID获取对应的图标
 */
fun getCategoryIcon(categoryId: Long): ImageVector {
    // 这里可以根据实际分类ID映射不同的图标
    // 在真实应用中，应该从数据库中查询分类的图标信息
    return when (categoryId % 10) {
        0L -> Icons.Default.Restaurant
        1L -> Icons.Default.ShoppingBag
        2L -> Icons.Default.DirectionsBus
        3L -> Icons.Default.Home
        4L -> Icons.Default.Smartphone
        5L -> Icons.Default.School
        6L -> Icons.Default.LocalHospital
        7L -> Icons.Default.AccountBalance
        8L -> Icons.Default.MoneyOff
        else -> Icons.Default.MiscellaneousServices
    }
} 