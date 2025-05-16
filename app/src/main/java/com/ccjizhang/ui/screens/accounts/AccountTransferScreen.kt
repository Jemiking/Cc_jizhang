package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.viewmodels.AccountTransferViewModel
import java.util.*

/**
 * 账户转账界面
 * 允许用户在两个账户之间转移资金
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferScreen(
    navController: NavHostController,
    viewModel: AccountTransferViewModel = hiltViewModel()
) {
    // 获取账户列表
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val transferResult by viewModel.transferResult.collectAsState()

    // 转账信息状态
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // 下拉菜单状态
    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    // 转账成功后自动返回
    LaunchedEffect(transferResult) {
        if (transferResult) {
            navController.popBackStack()
        }
    }

    // 加载账户数据
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    UnifiedScaffold(
        title = "账户转账",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 转账表单
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecondaryCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "转账信息",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // 来源账户选择
                        ExposedDropdownMenuBox(
                            expanded = fromAccountExpanded,
                            onExpandedChange = { fromAccountExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = accounts.find { it.id == fromAccountId }?.name ?: "选择来源账户",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("从") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromAccountExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = fromAccountExpanded,
                                onDismissRequest = { fromAccountExpanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${account.name} (¥${String.format("%.2f", account.balance)})")
                                        },
                                        onClick = {
                                            fromAccountId = account.id
                                            fromAccountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 转换图标
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    // 交换来源和目标账户
                                    val temp = fromAccountId
                                    fromAccountId = toAccountId
                                    toAccountId = temp
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "交换账户",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 目标账户选择
                        ExposedDropdownMenuBox(
                            expanded = toAccountExpanded,
                            onExpandedChange = { toAccountExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = accounts.find { it.id == toAccountId }?.name ?: "选择目标账户",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("到") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = toAccountExpanded,
                                onDismissRequest = { toAccountExpanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${account.name} (¥${String.format("%.2f", account.balance)})")
                                        },
                                        onClick = {
                                            toAccountId = account.id
                                            toAccountExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 金额输入
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("转账金额") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text("¥") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // 备注输入
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("备注（可选）") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // 转账按钮
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        if (fromAccountId != null && toAccountId != null && amountValue > 0) {
                            viewModel.transferBetweenAccounts(
                                fromAccountId = fromAccountId!!,
                                toAccountId = toAccountId!!,
                                amount = amountValue,
                                note = note,
                                date = Date()
                            )
                        }
                    },
                    enabled = fromAccountId != null && toAccountId != null &&
                             fromAccountId != toAccountId &&
                             amount.toDoubleOrNull() != null &&
                             (amount.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("确认转账")
                }

                // 提示文本
                if (fromAccountId == toAccountId && fromAccountId != null) {
                    Text(
                        "不能向同一账户转账",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 余额不足提示
                fromAccountId?.let { id ->
                    val fromAccount = accounts.find { it.id == id }
                    val transferAmount = amount.toDoubleOrNull() ?: 0.0
                    if (fromAccount != null && transferAmount > fromAccount.balance) {
                        Text(
                            "余额不足，当前账户余额: ¥${String.format("%.2f", fromAccount.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}