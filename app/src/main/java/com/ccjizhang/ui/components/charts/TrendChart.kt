package com.ccjizhang.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccjizhang.ui.viewmodels.StatsTab
import java.text.DecimalFormat

/**
 * 交互式趋势图组件
 */
@Composable
fun InteractiveTrendChart(
    trends: List<com.ccjizhang.ui.viewmodels.TrendItem>,
    tabType: StatsTab,
    modifier: Modifier = Modifier,
    onPointClick: (com.ccjizhang.ui.viewmodels.TrendItem) -> Unit = {}
) {
    if (trends.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val formatter = DecimalFormat("#,##0.00")
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // 确定图表颜色
    val chartColor = when (tabType) {
        StatsTab.EXPENSE -> Color(0xFFE53935) // 红色
        StatsTab.INCOME -> Color(0xFF43A047) // 绿色
        StatsTab.NET -> Color(0xFF1976D2) // 蓝色
    }

    // 对于净收支，我们可能需要不同的颜色来表示正负值
    val positiveColor = Color(0xFF43A047) // 绿色表示正值
    val negativeColor = Color(0xFFE53935) // 红色表示负值

    // 找出最大值和最小值，用于缩放
    val maxValue = trends.maxByOrNull { it.value }?.value ?: 0.0
    val minValue = if (tabType == StatsTab.NET) {
        // 对于净收支，我们需要考虑负值
        trends.minByOrNull { it.value }?.value ?: 0.0
    } else {
        0.0 // 对于收入和支出，最小值始终为0
    }

    // 确保有足够的范围来显示
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

    // 计算零线位置（仅对净收支有意义）
    val hasNegativeValues = minValue < 0
    val zeroLinePosition = if (hasNegativeValues) {
        (maxValue / valueRange).toFloat()
    } else {
        1.0f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Y轴刻度
        Column(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val gridCount = 5
            val valueStep = valueRange / gridCount

            for (i in 0..gridCount) {
                val value = maxValue - (i * valueStep)
                val formattedValue = if (Math.abs(value) >= 1000) {
                    "${(value / 1000).toInt()}k"
                } else {
                    "${value.toInt()}"
                }

                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 图表主体
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 绘制趋势图
            Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(trends) {
                    detectTapGestures { offset ->
                        // 计算点击的是哪个数据点
                        val width = size.width
                        val height = size.height
                        val barWidth = width / trends.size

                        val clickedIndex = (offset.x / barWidth).toInt().coerceIn(0, trends.size - 1)
                        selectedIndex = if (selectedIndex == clickedIndex) null else clickedIndex

                        if (selectedIndex != null) {
                            onPointClick(trends[selectedIndex!!])
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / trends.size

            // 绘制水平网格线
            val gridCount = 5
            for (i in 0..gridCount) {
                val y = height - (height * i / gridCount)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // 如果有负值，绘制零线
            if (hasNegativeValues && tabType == StatsTab.NET) {
                val zeroY = height * zeroLinePosition
                drawLine(
                    color = Color.Gray,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 2f
                )
            }

            // 我们不再绘制柱状图，只使用折线图显示所有趋势数据

            // 绘制折线图
            if (trends.size > 1) {
                val path = Path()
                var firstPoint = true

                trends.forEachIndexed { index, trend ->
                    val normalizedValue = if (valueRange > 0) {
                        ((trend.value - minValue) / valueRange).toFloat()
                    } else {
                        0f
                    }

                    val x = index * barWidth + barWidth / 2
                    val y = if (tabType == StatsTab.NET && hasNegativeValues) {
                        // 对于净收支，我们需要考虑零线位置
                        val zeroY = height * zeroLinePosition
                        if (trend.value >= 0) {
                            // 正值，在零线上方
                            zeroY - (normalizedValue * height * zeroLinePosition)
                        } else {
                            // 负值，在零线下方
                            zeroY + (normalizedValue * height * (1 - zeroLinePosition))
                        }
                    } else {
                        // 对于收入和支出，正常计算
                        height - normalizedValue * height
                    }

                    if (firstPoint) {
                        path.moveTo(x, y)
                        firstPoint = false
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // 对于净收支，我们可能需要绘制多条线来表示正负值
                if (tabType == StatsTab.NET && hasNegativeValues) {
                    // 分别绘制正值和负值的线
                    var positivePath: Path? = null
                    var negativePath: Path? = null
                    var lastPoint: Pair<Float, Float>? = null

                    trends.forEachIndexed { index, trend ->
                        val normalizedValue = if (valueRange > 0) {
                            ((trend.value - minValue) / valueRange).toFloat()
                        } else {
                            0f
                        }

                        val x = index * barWidth + barWidth / 2
                        val zeroY = height * zeroLinePosition
                        val y = if (trend.value >= 0) {
                            zeroY - (normalizedValue * height * zeroLinePosition)
                        } else {
                            zeroY + (normalizedValue * height * (1 - zeroLinePosition))
                        }

                        // 如果当前点是正值
                        if (trend.value >= 0) {
                            if (positivePath == null) {
                                positivePath = Path()
                                positivePath!!.moveTo(x, y)
                            } else {
                                // 如果上一个点是负值，需要先连接到零线
                                if (lastPoint != null && lastPoint!!.second > zeroY) {
                                    // 计算交叉点
                                    val crossX = lastPoint!!.first + (x - lastPoint!!.first) *
                                        ((zeroY - lastPoint!!.second) / (y - lastPoint!!.second))
                                    positivePath!!.moveTo(crossX, zeroY)
                                    positivePath!!.lineTo(x, y)
                                } else {
                                    positivePath!!.lineTo(x, y)
                                }
                            }
                        } else { // 如果当前点是负值
                            if (negativePath == null) {
                                negativePath = Path()
                                negativePath!!.moveTo(x, y)
                            } else {
                                // 如果上一个点是正值，需要先连接到零线
                                if (lastPoint != null && lastPoint!!.second < zeroY) {
                                    // 计算交叉点
                                    val crossX = lastPoint!!.first + (x - lastPoint!!.first) *
                                        ((zeroY - lastPoint!!.second) / (y - lastPoint!!.second))
                                    negativePath!!.moveTo(crossX, zeroY)
                                    negativePath!!.lineTo(x, y)
                                } else {
                                    negativePath!!.lineTo(x, y)
                                }
                            }
                        }

                        lastPoint = Pair(x, y)
                    }

                    // 绘制正值线
                    if (positivePath != null) {
                        drawPath(
                            path = positivePath!!,
                            color = positiveColor.copy(alpha = 0.8f),
                            style = Stroke(width = 2f)
                        )
                    }

                    // 绘制负值线
                    if (negativePath != null) {
                        drawPath(
                            path = negativePath!!,
                            color = negativeColor.copy(alpha = 0.8f),
                            style = Stroke(width = 2f)
                        )
                    }
                } else {
                    // 对于收入和支出，正常绘制单条线
                    drawPath(
                        path = path,
                        color = chartColor.copy(alpha = 0.8f),
                        style = Stroke(width = 2f)
                    )
                }

                // 绘制数据点
                trends.forEachIndexed { index, trend ->
                    val normalizedValue = if (valueRange > 0) {
                        ((trend.value - minValue) / valueRange).toFloat()
                    } else {
                        0f
                    }

                    val x = index * barWidth + barWidth / 2
                    val y = if (tabType == StatsTab.NET && hasNegativeValues) {
                        // 对于净收支，我们需要考虑零线位置
                        val zeroY = height * zeroLinePosition
                        if (trend.value >= 0) {
                            // 正值，在零线上方
                            zeroY - (normalizedValue * height * zeroLinePosition)
                        } else {
                            // 负值，在零线下方
                            zeroY + (normalizedValue * height * (1 - zeroLinePosition))
                        }
                    } else {
                        // 对于收入和支出，正常计算
                        height - normalizedValue * height
                    }
                    val isSelected = index == selectedIndex

                    // 对于净收支，根据正负值使用不同的颜色
                    val pointColor = if (tabType == StatsTab.NET) {
                        if (trend.value >= 0) positiveColor else negativeColor
                    } else {
                        chartColor
                    }

                    // 绘制数据点
                    drawCircle(
                        color = if (isSelected) Color.White else pointColor,
                        radius = if (isSelected) 6f else 4f,
                        center = Offset(x, y)
                    )

                    if (isSelected) {
                        drawCircle(
                            color = pointColor,
                            radius = 8f,
                            center = Offset(x, y),
                            style = Stroke(width = 2f)
                        )

                        // 对于选中的点，显示数值标签
                        // 注意：Canvas不支持直接绘制文本，这里只绘制背景
                        // 实际文本将在选中点信息卡片中显示
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = Offset(x - 20f, y - 30f),
                            size = Size(40f, 20f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                    }

                    // 对于最大值和最小值，始终显示数值标签
                    if (trend.value == maxValue || trend.value == minValue) {
                        val isMax = trend.value == maxValue
                        val labelText = if (isMax) "最大值" else "最小值"

                        // 绘制标签背景
                        drawRoundRect(
                            color = (if (isMax) positiveColor else negativeColor).copy(alpha = 0.8f),
                            topLeft = Offset(x - 25f, if (isMax) y - 30f else y + 10f),
                            size = Size(50f, 20f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
                            style = androidx.compose.ui.graphics.drawscope.Fill
                        )
                    }
                }

                // 添加平均线
                if (trends.size > 1) {
                    val avgValue = trends.map { it.value }.average()
                    val normalizedAvgValue = if (valueRange > 0) {
                        ((avgValue - minValue) / valueRange).toFloat()
                    } else {
                        0f
                    }

                    val avgY = if (tabType == StatsTab.NET && hasNegativeValues) {
                        val zeroY = height * zeroLinePosition
                        if (avgValue >= 0) {
                            zeroY - (normalizedAvgValue * height * zeroLinePosition)
                        } else {
                            zeroY + (normalizedAvgValue * height * (1 - zeroLinePosition))
                        }
                    } else {
                        height - normalizedAvgValue * height
                    }

                    // 绘制平均线
                    drawLine(
                        color = Color.Gray,
                        start = Offset(0f, avgY),
                        end = Offset(width, avgY),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // 绘制平均线标签背景
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.8f),
                        topLeft = Offset(width - 60f, avgY - 10f),
                        size = Size(60f, 20f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                }
            }
        }

        // 显示平均值标签
        if (trends.size > 1) {
            val avgValue = trends.map { it.value }.average()
            val formattedAvgValue = formatter.format(avgValue)

            Text(
                text = "平均: ${if (avgValue >= 0) "¥$formattedAvgValue" else "-¥${formatter.format(Math.abs(avgValue))}"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 8.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 显示选中的数据点信息
        selectedIndex?.let { index ->
            val trend = trends[index]
            val value = trend.value
            val formattedValue = formatter.format(value)

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = trend.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (value >= 0) "¥$formattedValue" else "-¥${formatter.format(Math.abs(value))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            tabType == StatsTab.EXPENSE -> Color(0xFFE53935)
                            tabType == StatsTab.INCOME -> Color(0xFF43A047)
                            value >= 0 -> Color(0xFF43A047)
                            else -> Color(0xFFE53935)
                        }
                    )
                }
            }
        }
    }

    // 显示图例（仅对净收支显示）
    if (tabType == StatsTab.NET && hasNegativeValues) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 正值图例
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(positiveColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "正值",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 负值图例
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(negativeColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "负值",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 显示X轴标签
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 只显示部分标签，避免拥挤
        val labelCount = 5
        val step = (trends.size / labelCount).coerceAtLeast(1)

        for (i in 0 until trends.size step step) {
            if (i < trends.size) {
                Text(
                    text = trends[i].label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}

/**
 * 完整的趋势图组件，包含标题、图例和数据摘要
 */
@Composable
fun CompleteTrendChart(
    title: String,
    trends: List<com.ccjizhang.ui.viewmodels.TrendItem>,
    tabType: StatsTab,
    modifier: Modifier = Modifier,
    onPointClick: (com.ccjizhang.ui.viewmodels.TrendItem) -> Unit = {}
) {
    val formatter = DecimalFormat("#,##0.00")

    // 计算数据摘要
    val maxValue = trends.maxByOrNull { it.value }?.value ?: 0.0
    val minValue = trends.minByOrNull { it.value }?.value ?: 0.0
    val avgValue = if (trends.isNotEmpty()) trends.map { it.value }.average() else 0.0
    val totalValue = trends.sumOf { it.value }

    // 计算增长率（如果有足够的数据点）
    val growthRate = if (trends.size >= 2) {
        val firstValue = trends.first().value
        val lastValue = trends.last().value
        if (firstValue != 0.0) {
            ((lastValue - firstValue) / Math.abs(firstValue)) * 100
        } else {
            0.0
        }
    } else {
        0.0
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题和数据摘要
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 增长率
                if (trends.size >= 2) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = if (growthRate >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (growthRate >= 0) "+" else "-"}${formatter.format(Math.abs(growthRate))}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (growthRate >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                        )
                    }
                }
            }

            // 数据摘要
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 最大值
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "最大值",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (maxValue >= 0) "¥${formatter.format(maxValue)}" else "-¥${formatter.format(Math.abs(maxValue))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (maxValue >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }

                // 平均值
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "平均值",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (avgValue >= 0) "¥${formatter.format(avgValue)}" else "-¥${formatter.format(Math.abs(avgValue))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (avgValue >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }

                // 最小值
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "最小值",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (minValue >= 0) "¥${formatter.format(minValue)}" else "-¥${formatter.format(Math.abs(minValue))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (minValue >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }
            }

            // 趋势图
            InteractiveTrendChart(
                trends = trends,
                tabType = tabType,
                onPointClick = onPointClick
            )
        }
    }
}
}