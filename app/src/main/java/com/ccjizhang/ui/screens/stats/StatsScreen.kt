package com.ccjizhang.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown

import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.charts.CompletePieChart
import com.ccjizhang.ui.components.dialogs.DateRangePickerDialog
import com.ccjizhang.ui.theme.CCJiZhangTheme
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.viewmodels.StatsTab
import com.ccjizhang.ui.viewmodels.StatsViewModel
import com.ccjizhang.ui.viewmodels.TimeFilter
import java.text.DecimalFormat
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun StatsScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onNavigateToStatisticsDetail: (String) -> Unit = { type -> navController.navigate(NavRoutes.statisticsDetail(type)) },
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()



    RoundedTopBarScaffold(
        title = "统计分析",
        navController = navController,
        onBackClick = onNavigateBack,
        showBackButton = true,
        actions = {}
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // 选项卡
                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    StatsTab.values().forEachIndexed { index, tab ->
                        val title = when (tab) {
                            StatsTab.EXPENSE -> "支出"
                            StatsTab.INCOME -> "收入"
                            StatsTab.NET -> "净收支"
                        }
                        Tab(
                            selected = uiState.selectedTab.ordinal == index,
                            onClick = { viewModel.setTab(tab) },
                            text = { Text(title) }
                        )
                    }
                }

                // 日期选择器
                DateSelector(
                    currentPeriod = uiState.currentPeriod.displayName,
                    onPrevious = { viewModel.navigateToPreviousPeriod() },
                    onNext = { viewModel.navigateToNextPeriod() }
                )

                // 时间筛选器
                TimeFilterRow(
                    selectedFilter = uiState.selectedTimeFilter,
                    onFilterSelected = { viewModel.setTimeFilter(it) }
                )

                // 总额卡片，根据选中的标签显示不同内容
                when (uiState.selectedTab) {
                    StatsTab.EXPENSE -> {
                        TotalAmountCard(
                            title = "总支出",
                            amount = uiState.totalExpense,
                            percentChange = uiState.expenseVsLastPeriod,
                            textColor = Color(0xFFE53935)
                        )
                    }
                    StatsTab.INCOME -> {
                        TotalAmountCard(
                            title = "总收入",
                            amount = uiState.totalIncome,
                            percentChange = uiState.incomeVsLastPeriod,
                            textColor = Color(0xFF43A047)
                        )
                    }
                    StatsTab.NET -> {
                        TotalAmountCard(
                            title = "净收支",
                            amount = uiState.netAmount,
                            percentChange = uiState.netVsLastPeriod,
                            textColor = if (uiState.netAmount >= 0) Color(0xFF43A047) else Color(0xFFE53935)
                        )

                        // 储蓄率卡片（仅在净收支标签下显示）
                        SavingsRateCard(savingsRate = uiState.savingsRate)
                    }
                }


                // 分类统计（仅在支出和收入标签下显示）
                if (uiState.selectedTab != StatsTab.NET) {
                    // 使用新的饼图组件
                    CompletePieChart(
                        title = if (uiState.selectedTab == StatsTab.EXPENSE) "支出分类" else "收入分类",
                        totalAmount = if (uiState.selectedTab == StatsTab.EXPENSE) uiState.totalExpense else uiState.totalIncome,
                        categories = uiState.categoryStats,
                        onCategoryClick = { category ->
                            // 点击分类时导航到详情页面
                            onNavigateToStatisticsDetail("category_${category.categoryId}")
                        }
                    )

                    // 排行榜
                    CategoryRankingList(
                        title = if (uiState.selectedTab == StatsTab.EXPENSE) "支出排行" else "收入排行",
                        categories = uiState.categoryStats.take(5)
                    )
                }

                // 底部间距
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DateSelector(
    currentPeriod: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentPeriod,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "选择日期",
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Row {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "上期",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "下期",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterRow(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    var showDateRangePicker by remember { mutableStateOf(false) }

    val timeOptions = listOf(
        TimeFilter.DAY to "日",
        TimeFilter.WEEK to "周",
        TimeFilter.MONTH to "月",
        TimeFilter.QUARTER to "季度",
        TimeFilter.YEAR to "年",
        TimeFilter.CUSTOM to "自定义"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(timeOptions) { (filter, label) ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = {
                    if (filter == TimeFilter.CUSTOM) {
                        showDateRangePicker = true
                    } else {
                        onFilterSelected(filter)
                    }
                },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryDark,
                    selectedLabelColor = Color.White
                )
            )
        }
    }

    // 显示日期范围选择器
    if (showDateRangePicker) {
        val viewModel = (LocalViewModelStoreOwner.current as? ViewModelStoreOwner)?.let {
            hiltViewModel<StatsViewModel>(it)
        }

        DateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel?.setCustomDateRange(startDate, endDate)
                onFilterSelected(TimeFilter.CUSTOM)
                showDateRangePicker = false
            }
        )
    }
}

@Composable
fun TotalAmountCard(
    title: String,
    amount: Double,
    percentChange: Double,
    textColor: Color
) {
    val formatter = DecimalFormat("#,##0.00")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "¥ ${formatter.format(amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val indicatorText = if (percentChange > 0) "↑" else if (percentChange < 0) "↓" else "—"
                Text(
                    text = "$indicatorText ${percentChange.absoluteValue.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        percentChange > 0 -> if (title == "总支出") Color(0xFFE53935) else Color(0xFF43A047)
                        percentChange < 0 -> if (title == "总支出") Color(0xFF43A047) else Color(0xFFE53935)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = "较上期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SavingsRateCard(savingsRate: Double) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "储蓄率",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${savingsRate.toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (savingsRate >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "收入中未消费的比例",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@Composable
fun CategoryPieChart(
    title: String,
    totalAmount: Double,
    categories: List<com.ccjizhang.ui.viewmodels.CategoryStatistics>
) {
    val formatter = DecimalFormat("#,##0")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 饼图（简化实现，实际应用中应使用Canvas绘制真实饼图）
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // 外圈饼图（简化表示）
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(if (categories.isNotEmpty()) categories.first().color else PrimaryDark)
                )

                // 内圈白色遮罩
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "¥${formatter.format(totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 图例
            Column {
                val topCategories = categories.take(6) // 只显示前6个分类

                // 显示前3个分类
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    topCategories.take(3).forEach { category ->
                        LegendItem(
                            name = category.categoryName,
                            amount = category.amount,
                            color = category.color
                        )
                    }
                }

                // 显示后3个分类（如果有的话）
                if (topCategories.size > 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        topCategories.drop(3).forEach { category ->
                            LegendItem(
                                name = category.categoryName,
                                amount = category.amount,
                                color = category.color
                            )
                        }
                        // 如果不足3个，添加空的Spacer保持布局
                        repeat(3 - topCategories.drop(3).size) {
                            Spacer(modifier = Modifier.width(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    name: String,
    amount: Double,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "¥${amount.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryRankingList(
    title: String,
    categories: List<com.ccjizhang.ui.viewmodels.CategoryStatistics>
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 遍历分类数据并显示排行
            categories.forEachIndexed { index, category ->
                RankingItem(
                    rank = index + 1,
                    name = category.categoryName,
                    amount = category.amount,
                    percentage = category.percentage.toInt(),
                    transactions = category.transactionCount,
                    color = category.color
                )
            }
        }
    }
}

@Composable
fun RankingItem(
    rank: Int,
    name: String,
    amount: Double,
    percentage: Int,
    transactions: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 排名数字
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 分类图标
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color,
            modifier = Modifier.size(36.dp),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = getCategoryIcon(name),
                contentDescription = name,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 分类信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$percentage% · ${transactions}笔交易",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 金额
        Text(
            text = "¥${amount.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE53935)
        )
    }
}

// 根据分类名称返回合适的图标
@Composable
fun getCategoryIcon(categoryName: String): ImageVector {
    return when {
        categoryName.contains("食品") || categoryName.contains("餐饮") -> Icons.Default.ShoppingBasket
        categoryName.contains("交通") -> Icons.Default.DirectionsBus
        categoryName.contains("娱乐") -> Icons.Default.SportsEsports
        categoryName.contains("住房") || categoryName.contains("居家") -> Icons.Default.Home
        categoryName.contains("服装") || categoryName.contains("服饰") -> Icons.Default.Checkroom
        else -> Icons.Default.Category
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StatsScreenPreview() {
    CCJiZhangTheme {
        StatsScreen(rememberNavController())
    }
}