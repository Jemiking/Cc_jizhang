package com.ccjizhang.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ccjizhang.ui.viewmodels.CategoryStatistics
import java.text.DecimalFormat
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

/**
 * 交互式饼图组件
 * 支持点击查看详情
 */
@Composable
fun InteractivePieChart(
    categories: List<CategoryStatistics>,
    totalAmount: Double,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryStatistics) -> Unit = {}
) {
    val formatter = DecimalFormat("#,##0.00")
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // 绘制饼图
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(categories) {
                    detectTapGestures { offset ->
                        // 计算点击位置相对于圆心的角度
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) / 2f

                        // 检查是否在圆内
                        val distance = kotlin.math.sqrt(
                            (offset.x - center.x) * (offset.x - center.x) +
                            (offset.y - center.y) * (offset.y - center.y)
                        )

                        if (distance > radius || distance < radius * 0.4f) {
                            // 点击在圆外或内圈，取消选择
                            selectedIndex = null
                            return@detectTapGestures
                        }

                        // 计算角度
                        val angle = (atan2(
                            offset.y - center.y,
                            offset.x - center.x
                        ) * 180 / PI).toFloat()

                        // 转换为0-360度
                        val normalizedAngle = if (angle < 0) angle + 360 else angle

                        // 查找对应的扇形
                        var startAngle = 0f
                        categories.forEachIndexed { index, category ->
                            val sweepAngle = (category.percentage * 3.6f).toFloat()
                            if (normalizedAngle >= startAngle && normalizedAngle <= startAngle + sweepAngle) {
                                selectedIndex = index
                                onCategoryClick(category)
                                return@detectTapGestures
                            }
                            startAngle += sweepAngle
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f

            // 如果没有数据，绘制空圆
            if (categories.isEmpty()) {
                drawCircle(
                    color = Color.LightGray,
                    radius = radius,
                    center = center
                )
                return@Canvas
            }

            // 绘制各个扇形
            var startAngle = 0f
            categories.forEachIndexed { index, category ->
                val sweepAngle = (category.percentage * 3.6f).toFloat()
                val isSelected = index == selectedIndex

                // 绘制扇形
                drawArc(
                    color = category.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                // 如果被选中，绘制高亮边框
                if (isSelected) {
                    drawArc(
                        color = Color.White,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 4f)
                    )
                }

                startAngle += sweepAngle
            }

            // 绘制内圈
            drawCircle(
                color = Color.White.copy(alpha = 1f),
                radius = radius * 0.4f,
                center = center
            )
        }

        // 中心显示总金额
        Column(
            modifier = Modifier
                .size(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "总金额",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "¥${formatter.format(totalAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 饼图图例项
 */
@Composable
fun PieChartLegendItem(
    category: CategoryStatistics,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val formatter = DecimalFormat("#,##0.00")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 颜色标记
        Box(
            modifier = Modifier
                .size(16.dp)
                .padding(end = 8.dp)
                .background(
                    color = category.color,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        // 分类名称
        Text(
            text = category.categoryName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        // 金额
        Text(
            text = "¥${formatter.format(category.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // 百分比
        Text(
            text = String.format("%.1f%%", category.percentage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 完整的饼图组件，包含图例
 */
@Composable
fun CompletePieChart(
    categories: List<CategoryStatistics>,
    totalAmount: Double,
    title: String,
    modifier: Modifier = Modifier,
    onCategoryClick: (CategoryStatistics) -> Unit = {}
) {
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 饼图
        InteractivePieChart(
            categories = categories,
            totalAmount = totalAmount,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            onCategoryClick = { category ->
                selectedCategoryIndex = categories.indexOf(category)
                onCategoryClick(category)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 图例
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, category ->
                PieChartLegendItem(
                    category = category,
                    isSelected = index == selectedCategoryIndex,
                    onClick = {
                        selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                        if (selectedCategoryIndex != null) {
                            onCategoryClick(category)
                        }
                    }
                )
            }
        }
    }
}
