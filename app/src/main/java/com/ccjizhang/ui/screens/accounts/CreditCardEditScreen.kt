package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Account
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.CreditCardViewModel
import com.ccjizhang.ui.common.OperationResult

/**
 * 信用卡编辑界面
 * 用于添加新信用卡或编辑现有信用卡
 * @param navController 导航控制器
 * @param creditCardId 信用卡ID，为null时表示添加新信用卡
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardEditScreen(
    navController: NavHostController,
    creditCardId: Long? = null,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val isEditMode = creditCardId != null
    val title = if (isEditMode) "编辑信用卡" else "添加信用卡"
    
    // 获取信用卡详情
    val selectedCard by viewModel.selectedCard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    
    // 表单状态
    var cardName by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }
    var billingDay by remember { mutableStateOf("1") }
    var dueDay by remember { mutableStateOf("15") }
    var cardColor by remember { mutableStateOf(Color(0xFF1976D2)) }
    var includeInTotal by remember { mutableStateOf(true) }
    var iconName by remember { mutableStateOf("credit_card") }
    
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
                    
                    // 如果操作成功，返回上一页
                    navController.popBackStack()
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
    
    // 加载现有信用卡数据（编辑模式）
    LaunchedEffect(creditCardId) {
        if (isEditMode) {
            creditCardId?.let { id ->
                viewModel.loadCreditCard(id)
            }
        }
    }
    
    // 更新表单值为当前信用卡信息（编辑模式）
    LaunchedEffect(selectedCard) {
        selectedCard?.let { card ->
            cardName = card.name
            creditLimit = card.creditLimit.toString()
            billingDay = card.billingDay.toString()
            dueDay = card.dueDay.toString()
            cardColor = Color(card.color)
            includeInTotal = card.includeInTotal
            iconName = card.icon
        }
    }
    
    val scrollState = rememberScrollState()
    
    RoundedTopBarScaffold(
        title = title,
        navController = navController,
        showBackButton = true
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
                        .verticalScroll(scrollState)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "基本信息",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            // 信用卡名称
                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text("信用卡名称") },
                                placeholder = { Text("例如：工商银行信用卡") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 信用额度
                            OutlinedTextField(
                                value = creditLimit,
                                onValueChange = { creditLimit = it },
                                label = { Text("信用额度") },
                                placeholder = { Text("信用卡的总额度") },
                                prefix = { Text("¥") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            // 颜色选择器
                            Text(
                                text = "颜色",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 预定义的颜色列表
                                val colors = listOf(
                                    Color(0xFF1976D2), // 蓝色
                                    Color(0xFF388E3C), // 绿色
                                    Color(0xFFD32F2F), // 红色
                                    Color(0xFFF57C00), // 橙色
                                    Color(0xFF7B1FA2), // 紫色
                                    Color(0xFF455A64)  // 蓝灰色
                                )
                                
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { cardColor = color }
                                            .border(
                                                width = 2.dp,
                                                color = if (color == cardColor) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                            
                            // 包含在总资产中的选项
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = includeInTotal,
                                    onCheckedChange = { includeInTotal = it }
                                )
                                Text(
                                    text = "包含在总资产计算中",
                                    modifier = Modifier.clickable { includeInTotal = !includeInTotal }
                                )
                            }
                        }
                    }
                    
                    // 账单周期信息卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "账单周期信息",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            // 账单日
                            OutlinedTextField(
                                value = billingDay,
                                onValueChange = { 
                                    val day = it.toIntOrNull()
                                    if (it.isEmpty() || (day != null && day in 1..31)) {
                                        billingDay = it
                                    }
                                },
                                label = { Text("账单日") },
                                placeholder = { Text("每月的第几天") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = { Text("范围: 1-31") }
                            )
                            
                            // 还款日
                            OutlinedTextField(
                                value = dueDay,
                                onValueChange = { 
                                    val day = it.toIntOrNull()
                                    if (it.isEmpty() || (day != null && day in 1..31)) {
                                        dueDay = it
                                    }
                                },
                                label = { Text("还款日") },
                                placeholder = { Text("每月的第几天") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = { Text("范围: 1-31") }
                            )
                        }
                    }
                    
                    // 按钮区域
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        
                        Button(
                            onClick = {
                                try {
                                    val limitValue = creditLimit.toDoubleOrNull() ?: 0.0
                                    val billingDayValue = billingDay.toIntOrNull() ?: 1
                                    val dueDayValue = dueDay.toIntOrNull() ?: 15
                                    
                                    if (isEditMode && creditCardId != null) {
                                        viewModel.updateCreditCard(
                                            id = creditCardId,
                                            name = cardName,
                                            creditLimit = limitValue,
                                            billingDay = billingDayValue,
                                            dueDay = dueDayValue,
                                            color = cardColor.toArgb(),
                                            iconName = iconName,
                                            includeInTotal = includeInTotal
                                        )
                                    } else {
                                        viewModel.addCreditCard(
                                            name = cardName,
                                            creditLimit = limitValue,
                                            billingDay = billingDayValue,
                                            dueDay = dueDayValue,
                                            color = cardColor.toArgb(),
                                            iconName = iconName,
                                            includeInTotal = includeInTotal
                                        )
                                    }
                                } catch (e: Exception) {
                                    showSnackbar = true
                                    snackbarMessage = "保存失败: ${e.message}"
                                    isError = true
                                }
                            },
                            enabled = cardName.isNotBlank() && 
                                     creditLimit.isNotBlank() &&
                                     billingDay.isNotBlank() &&
                                     dueDay.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存")
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