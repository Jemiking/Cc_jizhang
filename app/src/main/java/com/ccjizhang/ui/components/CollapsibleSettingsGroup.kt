package com.ccjizhang.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 可折叠的设置组
 *
 * @param title 组标题
 * @param icon 组图标
 * @param isExpanded 是否展开
 * @param onToggleExpanded 切换展开状态的回调
 * @param content 组内容
 */
@Composable
fun CollapsibleSettingsGroup(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 组标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onToggleExpanded,
                    // 添加语义属性，提高屏幕阅读器可用性
                    onClickLabel = if (isExpanded) "折叠${title}设置组" else "展开${title}设置组"
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = "${title} 图标",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                // 支持动态字体大小
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                // 添加字体粗细以提高可读性
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )

            // 展开/折叠箭头
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "Arrow Rotation"
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "折叠" else "展开",
                modifier = Modifier.rotate(rotation)
            )
        }

        // 可折叠内容
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                content()
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}
