package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.viewmodels.AccountBalanceAdjustViewModel

/**
 * 账户余额调整界面
 * 允许用户直接设置账户余额或增减余额
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBalanceAdjustScreen(
    navController: NavHostController,
    viewModel: AccountBalanceAdjustViewModel = hiltViewModel()
) {
    // 获取账户列表和状态
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val adjustmentResult by viewModel.adjustmentResult.collectAsState()

    // 界面状态
    var accountExpanded by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var adjustmentMode by remember { mutableStateOf(AdjustmentMode.SET_BALANCE) }
    var amountText by remember { mutableStateOf("") }

    // 操作结果提示
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    // 加载账户数据
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    // 当选择账户ID变化时，加载账户详情
    LaunchedEffect(selectedAccountId) {
        selectedAccountId?.let {
            viewModel.loadAccount(it)
        }
    }

    // 处理调整结果
    LaunchedEffect(adjustmentResult) {
        if (adjustmentResult) {
            showSnackbar = true
            snackbarMessage = "账户余额调整成功"
            // 延迟返回
            kotlinx.coroutines.delay(1000)
            viewModel.resetAdjustmentResult()
            navController.popBackStack()
        }
    }

    RoundedTopBarScaffold(
        title = "账户余额调整",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 账户选择器
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "选择账户",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // 账户下拉选择
                            ExposedDropdownMenuBox(
                                expanded = accountExpanded,
                                onExpandedChange = { accountExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = accounts.find { it.id == selectedAccountId }?.name
                                        ?: "请选择账户",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = accountExpanded
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = accountExpanded,
                                    onDismissRequest = { accountExpanded = false }
                                ) {
                                    accounts.forEach { account ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${account.name} (¥${String.format("%.2f", account.balance)})")
                                            },
                                            onClick = {
                                                selectedAccountId = account.id
                                                accountExpanded = false
                                                // 初始化金额输入框为当前余额
                                                amountText = account.balance.toString()
                                            }
                                        )
                                    }
                                }
                            }

                            // 显示当前账户余额
                            selectedAccount?.let { account ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("当前余额:")
                                    Text(
                                        "¥${String.format("%.2f", account.balance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (account.balance >= 0)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // 调整模式选择
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "调整方式",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // 调整模式切换
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = adjustmentMode == AdjustmentMode.SET_BALANCE,
                                    onClick = { adjustmentMode = AdjustmentMode.SET_BALANCE },
                                    label = { Text("直接设置余额") },
                                    leadingIcon = if (adjustmentMode == AdjustmentMode.SET_BALANCE) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = adjustmentMode == AdjustmentMode.ADJUST_BALANCE,
                                    onClick = { adjustmentMode = AdjustmentMode.ADJUST_BALANCE },
                                    label = { Text("增加/减少") },
                                    leadingIcon = if (adjustmentMode == AdjustmentMode.ADJUST_BALANCE) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Balance,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 金额输入框
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = {
                                    Text(
                                        when (adjustmentMode) {
                                            AdjustmentMode.SET_BALANCE -> "新余额"
                                            AdjustmentMode.ADJUST_BALANCE -> "调整金额（正数增加，负数减少）"
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                prefix = { Text("¥") },
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    if (adjustmentMode == AdjustmentMode.ADJUST_BALANCE && selectedAccount != null) {
                                        val currentBalance = selectedAccount!!.balance
                                        val adjustment = amountText.toDoubleOrNull() ?: 0.0
                                        val newBalance = currentBalance + adjustment
                                        Text(
                                            text = "调整后余额: ¥${String.format("%.2f", newBalance)}",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 确认按钮
                    Button(
                        onClick = {
                            val accountId = selectedAccountId ?: return@Button
                            val amountValue = amountText.toDoubleOrNull() ?: return@Button

                            when (adjustmentMode) {
                                AdjustmentMode.SET_BALANCE -> {
                                    viewModel.setAccountBalance(accountId, amountValue)
                                }
                                AdjustmentMode.ADJUST_BALANCE -> {
                                    viewModel.adjustAccountBalance(accountId, amountValue)
                                }
                            }
                        },
                        enabled = selectedAccountId != null && amountText.isNotBlank() && amountText.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("确认调整")
                    }
                }
            }

            // Snackbar显示操作结果
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    }
}

// 调整模式枚举
enum class AdjustmentMode {
    SET_BALANCE,      // 直接设置余额
    ADJUST_BALANCE    // 增加/减少余额
}