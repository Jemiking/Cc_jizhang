package com.ccjizhang.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.StatisticsDetailViewModel
import com.ccjizhang.ui.viewmodels.StatsTab
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 统计详情页面
 * 显示特定类型（支出/收入/净收支）或特定分类的详细统计信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDetailScreen(
    navController: NavHostController,
    type: String,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    viewModel: StatisticsDetailViewModel = hiltViewModel()
) {
    // 加载数据
    LaunchedEffect(type) {
        viewModel.loadStatisticsDetail(type)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    UnifiedScaffold(
        title = uiState.title,
        navController = navController,
        onBackClick = onNavigateBack,
        showBackButton = true,
        showFloatingActionButton = false,
        actions = {
            // 筛选按钮
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = Color.White
                )
            }

            // 导出按钮
            IconButton(onClick = { viewModel.exportData() }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "导出数据",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            StatisticsDetailContent(
                uiState = uiState,
                onTransactionClick = { transactionId ->
                    navController.navigate(NavRoutes.transactionDetail(transactionId))
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // 筛选对话框
    if (showFilterDialog) {
        FilterDialog(
            currentTimeRange = uiState.timeRange,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { timeRange ->
                viewModel.updateTimeRange(timeRange)
                showFilterDialog = false
            }
        )
    }

    // 导出成功提示
    if (uiState.showExportSuccess) {
        LaunchedEffect(Unit) {
            viewModel.resetExportState()
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.resetExportState() }) {
                    Text("确定")
                }
            }
        ) {
            Text("数据已成功导出到: ${uiState.exportPath}")
        }
    }
}

@Composable
fun StatisticsDetailContent(
    uiState: StatisticsDetailViewModel.StatisticsDetailUiState,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,##0.00")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 总金额卡片
        item {
            TotalAmountDetailCard(
                title = uiState.title,
                amount = uiState.totalAmount,
                timeRange = uiState.timeRange,
                tabType = uiState.tabType
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 时间趋势图
        item {
            DetailedTrendChart(
                trends = uiState.trends,
                tabType = uiState.tabType
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 分类饼图 (如果是分类详情，则不显示)
        if (!uiState.isCategoryDetail) {
            item {
                DetailedCategoryPieChart(
                    categories = uiState.categoryStats,
                    totalAmount = uiState.totalAmount,
                    tabType = uiState.tabType
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 交易列表标题
        item {
            Text(
                text = "相关交易记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 交易列表
        if (uiState.transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无交易记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.transactions) { transaction ->
                // 使用简化版的TransactionItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTransactionClick(transaction.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 交易图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (transaction.isIncome) Color(0xFF43A047) else Color(0xFFE53935)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 交易信息
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.note.ifEmpty { if (transaction.isIncome) "收入" else "支出" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 金额
                    Text(
                        text = if (transaction.isIncome) "+¥${transaction.amount}" else "-¥${-transaction.amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.isIncome) Color(0xFF43A047) else Color(0xFFE53935)
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }

        // 底部间距
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TotalAmountDetailCard(
    title: String,
    amount: Double,
    timeRange: String,
    tabType: StatsTab
) {
    val formatter = DecimalFormat("#,##0.00")
    val textColor = when (tabType) {
        StatsTab.EXPENSE -> Color(0xFFE53935)
        StatsTab.INCOME -> Color(0xFF43A047)
        StatsTab.NET -> if (amount >= 0) Color(0xFF43A047) else Color(0xFFE53935)
    }

    PrimaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = timeRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¥ ${formatter.format(amount)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun DetailedTrendChart(
    trends: List<com.ccjizhang.ui.viewmodels.StatisticsDetailViewModel.TrendItem>,
    tabType: StatsTab
) {
    // 这里将在后续实现更详细的趋势图
    // 暂时使用与主统计页面相同的图表组件
    SecondaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "时间趋势",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "详细趋势图将在后续实现",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DetailedCategoryPieChart(
    categories: List<com.ccjizhang.ui.viewmodels.CategoryStatistics>,
    totalAmount: Double,
    tabType: StatsTab
) {
    // 这里将在后续实现更详细的饼图
    // 暂时使用与主统计页面相同的图表组件
    SecondaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (tabType == StatsTab.EXPENSE) "支出分类" else "收入分类",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "详细饼图将在后续实现",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun FilterDialog(
    currentTimeRange: String,
    onDismiss: () -> Unit,
    onApplyFilter: (String) -> Unit
) {
    val timeRanges = listOf("本日", "本周", "本月", "本季度", "本年", "自定义")
    var selectedTimeRange by remember { mutableStateOf(currentTimeRange) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间范围") },
        text = {
            Column {
                timeRanges.forEach { timeRange ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = timeRange == selectedTimeRange,
                            onClick = { selectedTimeRange = timeRange }
                        )

                        Text(
                            text = timeRange,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApplyFilter(selectedTimeRange) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
