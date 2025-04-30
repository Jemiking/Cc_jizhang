package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.Account
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.CreditCardViewModel
import com.ccjizhang.ui.common.OperationResult
import java.text.SimpleDateFormat
import java.util.*

/**
 * 信用卡列表界面
 * 显示所有信用卡及其相关信息，提供添加、查看详情等功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardListScreen(
    navController: NavHostController,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    // 获取信用卡列表
    val creditCards by viewModel.creditCards.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    // 操作结果提示
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    // 处理操作结果
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is OperationResult.Success -> {
                    snackbarMessage = it.message ?: "操作成功"
                    isError = false
                    showSnackbar = true
                }
                is OperationResult.Error -> {
                    snackbarMessage = it.message
                    isError = true
                    showSnackbar = true
                }
                is OperationResult.Loading -> {
                    snackbarMessage = "正在处理..."
                    isError = false
                    showSnackbar = true
                }
            }
            viewModel.clearOperationResult()
        }
    }
    
    // 加载信用卡数据
    LaunchedEffect(Unit) {
        viewModel.loadCreditCards()
    }
    
    RoundedTopBarScaffold(
        title = "信用卡管理",
        navController = navController,
        showBackButton = true,
        actions = {
            IconButton(onClick = { navController.navigate(NavRoutes.CreditCardAdd) }) {
                Icon(Icons.Default.Add, contentDescription = "添加信用卡")
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // 信用卡总览
                    val totalBalance = creditCards.sumOf { it.balance }
                    val totalLimit = creditCards.sumOf { it.creditLimit }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "信用卡总负债",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "¥${String.format("%.2f", -totalBalance)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "总额度: ¥${String.format("%.2f", totalLimit)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "可用额度: ¥${String.format("%.2f", totalLimit + totalBalance)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateAllCreditCardDates()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "更新账单周期",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("更新账单周期")
                                }
                                
                                TextButton(
                                    onClick = { navController.navigate(NavRoutes.AccountTransfer) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = "还款",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("还款")
                                }
                            }
                        }
                    }
                    
                    // 信用卡列表
                    Text(
                        text = "我的信用卡",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    if (creditCards.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无信用卡，点击右上角添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(creditCards) { creditCard ->
                                CreditCardItem(
                                    creditCard = creditCard,
                                    daysUntilBilling = viewModel.getDaysUntilNextBillingDate(creditCard),
                                    daysUntilDue = viewModel.getDaysUntilNextDueDate(creditCard),
                                    onClick = {
                                        navController.navigate(NavRoutes.creditCardDetail(creditCard.id))
                                    }
                                )
                            }
                        }
                    }
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
    }
}

/**
 * 信用卡项UI组件
 */
@Composable
fun CreditCardItem(
    creditCard: Account,
    daysUntilBilling: Int,
    daysUntilDue: Int,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 信用卡头部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 信用卡图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(creditCard.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                
                // 信用卡信息
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = creditCard.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "额度: ¥${String.format("%.2f", creditCard.creditLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                // 账户余额
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "¥${String.format("%.2f", creditCard.balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (creditCard.balance < 0) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "可用额度: ¥${String.format("%.2f", creditCard.creditLimit + creditCard.balance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 账单与还款信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "账单日: ${creditCard.billingDay}日",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (creditCard.nextBillingDate != null) {
                        Text(
                            text = "下个账单日: ${dateFormat.format(creditCard.nextBillingDate!!)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (daysUntilBilling > 0) {
                        Text(
                            text = "距离账单日还有${daysUntilBilling}天",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "还款日: ${creditCard.dueDay}日",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (creditCard.nextDueDate != null) {
                        Text(
                            text = "下个还款日: ${dateFormat.format(creditCard.nextDueDate!!)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (daysUntilDue > 0) {
                        Text(
                            text = "距离还款日还有${daysUntilDue}天",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (daysUntilDue <= 7) 
                                MaterialTheme.colorScheme.error
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
} 