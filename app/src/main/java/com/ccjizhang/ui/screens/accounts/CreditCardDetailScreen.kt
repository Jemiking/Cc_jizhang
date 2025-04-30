package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.background
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
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.CreditCardViewModel
import com.ccjizhang.ui.common.OperationResult
import java.text.SimpleDateFormat
import java.util.*

/**
 * 信用卡详情界面
 * 显示单个信用卡的详细信息，提供编辑和删除功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDetailScreen(
    navController: NavHostController,
    creditCardId: Long,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    // 获取信用卡详情
    val creditCard by viewModel.selectedCard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    // 操作结果提示
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 处理操作结果
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is OperationResult.Success -> {
                    snackbarMessage = it.message ?: "操作成功"
                    isError = false
                    showSnackbar = true
                    
                    // 如果是删除操作成功，返回上一页
                    if (it.message?.contains("删除") == true) {
                        navController.popBackStack()
                    }
                }
                is OperationResult.Error -> {
                    snackbarMessage = it.message
                    isError = true
                    showSnackbar = true
                }
                is OperationResult.Loading -> {
                    // Optionally show loading indicator or message
                    snackbarMessage = "正在处理..."
                    isError = false
                    showSnackbar = true
                }
            }
            viewModel.clearOperationResult()
        }
    }
    
    // 加载信用卡数据
    LaunchedEffect(key1 = Unit) {
        viewModel.loadCreditCard(creditCardId)
    }
    
    RoundedTopBarScaffold(
        title = creditCard?.name ?: "信用卡详情",
        navController = navController,
        showBackButton = true,
        actions = {
            IconButton(onClick = { navController.navigate(NavRoutes.creditCardEdit(creditCardId)) }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑信用卡")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "删除信用卡")
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
            } else if (creditCard == null) {
                Text(
                    text = "信用卡不存在或已被删除",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                CreditCardDetailContent(
                    creditCard = creditCard!!,
                    daysUntilBilling = viewModel.getDaysUntilNextBillingDate(creditCard!!),
                    daysUntilDue = viewModel.getDaysUntilNextDueDate(creditCard!!),
                    onPayClick = { navController.navigate(NavRoutes.creditCardPayment(creditCardId)) }
                )
            }
            
            // Snackbar显示操作结果
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("关闭")
                        }
                    },
                    containerColor = if (isError) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isError) 
                        MaterialTheme.colorScheme.onErrorContainer 
                    else 
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    dismissAction = { showSnackbar = false }
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除信用卡") },
            text = { 
                Text("确定要删除信用卡 \"${creditCard?.name}\" 吗？与此信用卡相关的所有交易记录也将被删除，此操作不可撤销。") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCreditCard(creditCardId)
                        showDeleteDialog = false
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
}

/**
 * 信用卡详情内容
 */
@Composable
fun CreditCardDetailContent(
    creditCard: Account,
    daysUntilBilling: Int,
    daysUntilDue: Int,
    onPayClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // 信用卡余额与额度卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(creditCard.color).copy(alpha = 0.8f),
                contentColor = Color.White
            )
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
                        text = creditCard.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "当前余额",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "¥${String.format("%.2f", creditCard.balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "信用额度",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "¥${String.format("%.2f", creditCard.creditLimit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "可用额度",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "¥${String.format("%.2f", creditCard.creditLimit + creditCard.balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onPayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(creditCard.color)
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = "还款"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("还款")
                }
            }
        }
        
        // 账单信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "账单周期信息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 账单日信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "账单日",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "每月${creditCard.billingDay}日",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 下一个账单日
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "下一个账单日",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = creditCard.nextBillingDate?.let { dateFormat.format(it) } ?: "未设置",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (daysUntilBilling > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "距离账单日还有",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${daysUntilBilling}天",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 还款日信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "还款日",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "每月${creditCard.dueDay}日",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 下一个还款日
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "下一个还款日",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = creditCard.nextDueDate?.let { dateFormat.format(it) } ?: "未设置",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (daysUntilDue > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "距离还款日还有",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${daysUntilDue}天",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (daysUntilDue <= 7) MaterialTheme.colorScheme.error 
                                  else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // 其他信息与设置卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "其他信息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 包含在总资产计算中
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "包含在总资产计算中",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (creditCard.includeInTotal) "是" else "否",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 账户类型
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "账户类型",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "信用卡",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
} 