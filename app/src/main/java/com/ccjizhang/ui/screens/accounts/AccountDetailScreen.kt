package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.ccjizhang.data.model.Account
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.AccountViewModel
import com.ccjizhang.ui.viewmodels.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 账户详情界面
 * 显示单个账户的详细信息，提供编辑和删除功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    navController: NavHostController,
    accountId: Long,
    accountViewModel: AccountViewModel = hiltViewModel(),
    transactionViewModel: TransactionViewModel = hiltViewModel()
) {
    // 获取账户详情
    val account by accountViewModel.selectedAccount.collectAsState()
    val isLoading by accountViewModel.isLoading.collectAsState()
    val recentTransactions = listOf<com.ccjizhang.data.model.Transaction>() // 简化实现

    // 操作结果提示
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 加载账户数据
    LaunchedEffect(accountId) {
        try {
            // 直接使用同步方法加载账户数据
            accountViewModel.loadAccountSync(accountId)
            // transactionViewModel.loadRecentTransactionsByAccount(accountId, 5)
        } catch (e: Exception) {
            println("DEBUG: 加载账户数据失败: ${e.message}")
        }
    }

    // 处理删除结果
    LaunchedEffect(accountViewModel.operationResult.collectAsState().value) {
        accountViewModel.operationResult.value?.let { result ->
            when (result) {
                is com.ccjizhang.ui.common.OperationResult.Success -> {
                    if (result.message?.contains("删除") == true) {
                        navController.popBackStack()
                    }
                    accountViewModel.clearOperationResult()
                }
                is com.ccjizhang.ui.common.OperationResult.Error -> {
                    showSnackbar = true
                    snackbarMessage = result.message
                    isError = true
                    accountViewModel.clearOperationResult()
                }
                else -> {}
            }
        }
    }

    UnifiedScaffold(
        title = account?.name ?: "账户详情",
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = {
                accountId.let { id -> navController.navigate(NavRoutes.accountEdit(id)) }
            }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑账户")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除账户")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (account == null) {
                Text(
                    text = "账户不存在或已被删除",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AccountDetailContent(
                    account = account!!,
                    recentTransactions = recentTransactions,
                    onTransactionClick = { transactionId ->
                        navController.navigate(NavRoutes.transactionDetail(transactionId))
                    },
                    onAddTransaction = {
                        navController.navigate(NavRoutes.TransactionAdd)
                    },
                    onViewAllTransactions = {
                        // 导航到按账户筛选的交易列表
                        navController.navigate(NavRoutes.Transactions)
                    }
                )
            }

            // 删除确认对话框
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("确认删除") },
                    text = { Text("确定要删除账户 ${account?.name} 吗？此操作无法撤销，账户相关的所有交易记录也将被删除。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                accountViewModel.deleteAccount(accountId)
                                showDeleteDialog = false
                                // 直接导航到主页，避免账户管理页面的自动导航逻辑
                                navController.navigate(NavRoutes.Home) {
                                    // 清除导航栈，防止返回到已删除的账户详情页
                                    popUpTo(NavRoutes.Home) { inclusive = true }
                                }
                            }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // 操作结果提示
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    }
}

@Composable
fun AccountDetailContent(
    account: Account,
    recentTransactions: List<com.ccjizhang.data.model.Transaction>,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: () -> Unit,
    onViewAllTransactions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 账户余额卡片
        PrimaryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "当前余额",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${account.currency.symbol}${String.format("%.2f", account.balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (account.balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                Text(
                    text = account.type.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 账户操作卡片
        SecondaryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.Add,
                    label = "添加交易",
                    onClick = onAddTransaction
                )

                ActionButton(
                    icon = Icons.Default.CompareArrows,
                    label = "转账",
                    onClick = { /* 导航到转账页面 */ }
                )

                ActionButton(
                    icon = Icons.Default.Edit,
                    label = "调整余额",
                    onClick = { /* 导航到余额调整页面 */ }
                )
            }
        }

        // 账户详情卡片
        SecondaryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "账户信息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DetailRow(label = "账户名称", value = account.name)
                DetailRow(label = "账户类型", value = account.type.name)
                DetailRow(label = "币种", value = "${account.currency.code} (${account.currency.symbol})")
                DetailRow(label = "默认账户", value = if (account.isDefault) "是" else "否")
                DetailRow(label = "包含在总资产中", value = if (account.includeInTotal) "是" else "否")

                // 信用卡特有信息
                if (account.type == com.ccjizhang.data.model.AccountType.CREDIT_CARD) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "信用卡信息",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DetailRow(label = "信用额度", value = "${account.currency.symbol}${String.format("%.2f", account.creditLimit)}")
                    DetailRow(label = "账单日", value = "每月${account.billingDay}日")
                    DetailRow(label = "还款日", value = "每月${account.dueDay}日")

                    account.nextBillingDate?.let {
                        val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                        DetailRow(label = "下次账单日", value = dateFormat.format(it))
                    }

                    account.nextDueDate?.let {
                        val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                        DetailRow(label = "下次还款日", value = dateFormat.format(it))
                    }
                }
            }
        }

        // 最近交易卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近交易",
                        style = MaterialTheme.typography.titleMedium
                    )

                    TextButton(onClick = onViewAllTransactions) {
                        Text("查看全部")
                    }
                }

                if (recentTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无交易记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    recentTransactions.forEach { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun TransactionItem(
    transaction: com.ccjizhang.data.model.Transaction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 交易信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.note.ifEmpty { "无备注" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            Text(
                text = dateFormat.format(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 交易金额
        Text(
            text = if (transaction.isIncome) "+${transaction.amount}" else "-${transaction.amount}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
            fontWeight = FontWeight.Bold
        )
    }

    Divider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
