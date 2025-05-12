package com.ccjizhang.ui.screens.budget

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Budget
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.viewmodels.BudgetViewModel
import com.ccjizhang.ui.viewmodels.TransactionViewModel
import com.ccjizhang.ui.viewmodels.CategoryViewModel
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import kotlin.math.min

@Composable
fun BudgetDetailScreen(
    navController: NavHostController,
    budgetId: Long,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
    // 加载数据
    val budget by budgetViewModel.selectedBudget.collectAsState()
    val isLoading by budgetViewModel.isLoading.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()

    // 状态
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // 加载预算详情
    LaunchedEffect(budgetId) {
        budgetViewModel.loadBudgetDetails(budgetId)
        categoryViewModel.loadCategories()
    }

    UnifiedScaffold(
        title = budget?.name ?: "预算详情",
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = {
                // 导航到编辑预算页面
                budget?.id?.let { id -> navController.navigate(NavRoutes.budgetEdit(id)) }
            }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑预算",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除预算",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    ) { paddingValues ->
        if (isLoading || budget == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            budget?.let { currentBudget ->
                BudgetDetailContent(
                    budget = currentBudget,
                    budgetViewModel = budgetViewModel,
                    categoryViewModel = categoryViewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // 删除确认对话框
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("删除预算") },
                text = { Text("确定要删除此预算吗？此操作无法撤销。") },
                confirmButton = {
                    Button(
                        onClick = {
                            budget?.id?.let { budgetViewModel.deleteBudget(it) }
                            showDeleteConfirmation = false
                            navController.popBackStack()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun BudgetDetailContent(
    budget: Budget,
    budgetViewModel: BudgetViewModel,
    categoryViewModel: CategoryViewModel,
    modifier: Modifier = Modifier
) {
    val categories by categoryViewModel.categories.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 总预算卡片
        item {
            BudgetSummaryCard(budget = budget, budgetViewModel = budgetViewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 分类预算使用情况
        item {
            Text(
                text = "分类预算使用情况",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 这里应该根据预算类型显示不同内容
        if (budget.categories.isEmpty()) {
            // 总体预算
            item {
                CategoryBudgetItem(
                    categoryName = "总体预算",
                    icon = null,
                    iconColor = MaterialTheme.colorScheme.primary,
                    usedAmount = budgetViewModel.getUsedAmount(budget.id),
                    totalAmount = budget.amount
                )
            }
        } else {
            // 分类预算
            items(budget.categories) { categoryId ->
                val category = categories.find { it.category.id == categoryId }
                val (usedAmount, totalAmount) = budgetViewModel.getBudgetUsageForCategory(budget.id, categoryId)

                if (category != null) {
                    CategoryBudgetItem(
                        categoryName = category.category.name,
                        icon = category.icon,
                        iconColor = category.color,
                        usedAmount = usedAmount,
                        totalAmount = totalAmount
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 底部空间
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun BudgetSummaryCard(
    budget: Budget,
    budgetViewModel: BudgetViewModel
) {
    val usedAmount = budgetViewModel.getUsedAmount(budget.id)
    val totalAmount = budget.amount
    val usagePercentage = if (totalAmount > 0) (usedAmount / totalAmount) * 100 else 0.0
    val isOverBudget = usedAmount > totalAmount

    PrimaryCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 预算名称
            Text(
                text = budget.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // 预算期间
            Text(
                text = "周期: ${budget.period}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预算进度
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

            // 预算使用情况
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 已使用金额
                Column {
                    Text(
                        text = "已使用",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "¥${String.format("%.2f", usedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 剩余金额
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "剩余",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "¥${String.format("%.2f", totalAmount - usedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 使用百分比
            Text(
                text = "${String.format("%.1f", usagePercentage)}% ${if (isOverBudget) "超出预算" else "已使用"}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun CategoryBudgetItem(
    categoryName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    iconColor: Color,
    usedAmount: Double,
    totalAmount: Double
) {
    val usagePercentage = if (totalAmount > 0) (usedAmount / totalAmount) * 100 else 0.0
    val isOverBudget = usedAmount > totalAmount

    SecondaryCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 分类信息
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标（如果有）
                if (icon != null) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = categoryName,
                            tint = iconColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    // 如果没有图标，显示一个空的占位符
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                // 分类名称
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 预算进度
            LinearProgressIndicator(
                progress = min(1f, (usedAmount / totalAmount).toFloat()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverBudget) MaterialTheme.colorScheme.error else iconColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 预算使用情况
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 预算金额
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
        }
    }
}