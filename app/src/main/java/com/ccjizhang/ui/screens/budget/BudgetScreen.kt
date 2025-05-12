package com.ccjizhang.ui.screens.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Budget
import com.ccjizhang.ui.components.BudgetProgressItem
import com.ccjizhang.ui.components.CardContainer
import com.ccjizhang.ui.components.MainTopAppBar
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.theme.CategoryBlue
import com.ccjizhang.ui.theme.CategoryIndigo
import com.ccjizhang.ui.theme.CategoryOrange
import com.ccjizhang.ui.theme.CategoryPink
import com.ccjizhang.ui.theme.CategoryViolet
import com.ccjizhang.ui.theme.ExpenseRed
import com.ccjizhang.ui.theme.IncomeGreen
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.viewmodels.BudgetViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.theme.CCJiZhangTheme
import kotlin.math.min
import androidx.compose.runtime.collectAsState
import com.ccjizhang.ui.screens.budget.components.TotalBudgetCard
import com.ccjizhang.ui.screens.budget.components.MonthSelector
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onNavigateToAddBudget: () -> Unit = { navController.navigate(NavRoutes.AddEditBudget) },
    onNavigateToBudgetDetail: (Long) -> Unit = { id -> navController.navigate(NavRoutes.budgetDetail(id)) },
    viewModel: BudgetViewModel = hiltViewModel()
) {
    // 加载数据
    val budgets by viewModel.budgets.collectAsState()
    val currentMonthBudgets by viewModel.currentMonthBudgets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 加载预算数据
    LaunchedEffect(key1 = true) {
        viewModel.loadBudgets()
    }

    UnifiedScaffold(
        title = "预算管理",
        navController = navController,
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = false
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (budgets.isEmpty()) {
            // 显示空状态
            EmptyBudgetState(
                onAddBudget = onNavigateToAddBudget,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // 显示预算列表
            BudgetList(
                budgets = currentMonthBudgets,
                viewModel = viewModel,
                onBudgetClick = onNavigateToBudgetDetail,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun EmptyBudgetState(
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 空状态文字
        Text(
            text = "没有预算",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击下方按钮创建您的第一个预算",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 添加预算按钮
        ExtendedFloatingActionButton(
            onClick = onAddBudget,
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            text = { Text("创建预算") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun BudgetList(
    budgets: List<Budget>,
    viewModel: BudgetViewModel,
    onBudgetClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取预算使用情况缓存
    val budgetUsageCache by viewModel.budgetUsageCache.collectAsState()

    // 确保加载所有预算的使用情况
    LaunchedEffect(budgets) {
        viewModel.loadAllBudgetsUsage()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 当月预算标题
        item {
            Text(
                text = "当月预算",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 预算列表
        items(budgets) { budget ->
            BudgetCard(
                budget = budget,
                usedAmount = viewModel.getUsedAmount(budget.id),
                onClick = { onBudgetClick(budget.id) }
            )
        }

        // 底部空间，避免FAB遮挡
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun BudgetCard(
    budget: Budget,
    usedAmount: Double,
    onClick: () -> Unit
) {
    val totalAmount = budget.amount
    val usagePercentage = if (totalAmount > 0) (usedAmount / totalAmount) * 100 else 0.0
    val isOverBudget = usedAmount > totalAmount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 预算名称和期间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = budget.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "周期: ${budget.period}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 预算进度条
            LinearProgressIndicator(
                progress = min(1f, (usedAmount / totalAmount).toFloat()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 使用情况
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 已使用/总预算
                Text(
                    text = "¥${String.format("%.2f", usedAmount)} / ¥${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 使用百分比
                Text(
                    text = "${String.format("%.1f", usagePercentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            // 如果超出预算，显示警告信息
            if (isOverBudget) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "超出预算 ¥${String.format("%.2f", usedAmount - totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BudgetAnalysisCard() {
    CardContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "预算分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 分析信息
            Text(
                text = "您的预算使用情况良好，继续保持！",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 图表占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "预算使用趋势图",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 节约提示
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = IncomeGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "本月已节约 ¥1,745.68，比上月增长15%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: Long,
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "预算详情",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "预算详情: ID = $budgetId",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

data class BudgetItem(
    val name: String,
    val current: Double,
    val max: Double,
    val progress: Float,
    val color: Color,
    val icon: ImageVector
)

// 添加预览
@Preview(
    name = "预算页面预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewBudgetScreen() {
    CCJiZhangTheme {
        BudgetScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTotalBudgetCard() {
    CCJiZhangTheme {
        TotalBudgetCard(
            totalBudget = 5000.0,
            usedAmount = 3254.32
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBudgetAnalysisCard() {
    CCJiZhangTheme {
        BudgetAnalysisCard()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMonthSelector() {
    CCJiZhangTheme {
        MonthSelector(
            currentMonth = YearMonth.now(),
            onPreviousMonth = {},
            onNextMonth = {}
        )
    }
}