package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.ui.components.AccountDropdownMenu
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.viewmodels.AccountViewModel
import com.ccjizhang.ui.viewmodels.CreditCardViewModel
import com.ccjizhang.ui.common.OperationResult
import java.text.NumberFormat
import java.util.*

/**
 * 信用卡还款界面
 * 用于从其他账户向信用卡还款
 * @param navController 导航控制器
 * @param creditCardId 信用卡ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPaymentScreen(
    navController: NavHostController,
    creditCardId: Long,
    viewModel: CreditCardViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    // 加载信用卡信息
    val selectedCard by viewModel.selectedCard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()

    // 加载所有可用账户（排除当前信用卡）
    val accounts by accountViewModel.accounts.collectAsState()
    val availableAccounts = remember(accounts, creditCardId) {
        accounts.filter { it.id != creditCardId && it.type != AccountType.CREDIT_CARD }
    }

    // 表单状态
    var selectedSourceAccount by remember { mutableStateOf<Account?>(null) }
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    // 下拉菜单状态
    var expanded by remember { mutableStateOf(false) }

    // 操作结果提示
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // 处理操作结果 (确保使用 SharedOperationResult)
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

    // 加载信用卡数据
    LaunchedEffect(creditCardId) {
        viewModel.loadCreditCard(creditCardId)
        accountViewModel.loadAccounts()
    }

    // 当可用账户列表更新时，如果尚未选择源账户且有可用账户，则默认选择第一个
    LaunchedEffect(availableAccounts) {
        if (selectedSourceAccount == null && availableAccounts.isNotEmpty()) {
            selectedSourceAccount = availableAccounts.first()
        }
    }

    val scrollState = rememberScrollState()

    UnifiedScaffold(
        title = "信用卡还款",
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
                selectedCard?.let { creditCard ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // 信用卡信息卡片
                        SecondaryCard(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "还款信息",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Divider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("信用卡")
                                    Text(creditCard.name)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("当前余额")
                                    Text(
                                        text = NumberFormat.getCurrencyInstance(Locale.CHINA)
                                            .format(creditCard.balance),
                                        color = if (creditCard.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (creditCard.balance < 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("待还金额")
                                        Text(
                                            text = NumberFormat.getCurrencyInstance(Locale.CHINA)
                                                .format(-creditCard.balance),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                val daysUntilDue = remember(creditCard) {
                                    viewModel.getDaysUntilNextDueDate(creditCard)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("距离还款日")
                                    Text(
                                        text = "$daysUntilDue 天",
                                        color = when {
                                            daysUntilDue <= 3 -> MaterialTheme.colorScheme.error
                                            daysUntilDue <= 7 -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }

                        // 还款表单卡片
                        SecondaryCard(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "还款详情",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                // 源账户选择
                                Text("从哪个账户还款")

                                Box {
                                    OutlinedTextField(
                                        value = selectedSourceAccount?.name ?: "",
                                        onValueChange = { },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "选择账户"
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // 点击区域
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .padding(0.dp)
                                    ) { /* 空盒子，只用于捕获点击 */ }

                                    // 账户下拉菜单
                                    AccountDropdownMenu(
                                        expanded = expanded,
                                        accounts = availableAccounts,
                                        onAccountSelected = { account ->
                                            selectedSourceAccount = account
                                            expanded = false
                                        },
                                        onDismissRequest = { expanded = false }
                                    )
                                }

                                // 还款金额
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = { amount = it },
                                    label = { Text("还款金额") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    prefix = { Text("¥") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        if (creditCard.balance < 0) {
                                            Text("待还金额: ${NumberFormat.getCurrencyInstance(Locale.CHINA).format(-creditCard.balance)}")
                                        }
                                    }
                                )

                                // 快速金额选择按钮
                                if (creditCard.balance < 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { amount = (-creditCard.balance).toString() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("全额还款")
                                        }

                                        OutlinedButton(
                                            onClick = { amount = ((-creditCard.balance) / 2).toString() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("还一半")
                                        }
                                    }
                                }

                                // 备注
                                OutlinedTextField(
                                    value = memo,
                                    onValueChange = { memo = it },
                                    label = { Text("备注") },
                                    placeholder = { Text("例如：9月信用卡还款") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
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
                                    val amountValue = amount.toDoubleOrNull()
                                    if (selectedSourceAccount != null && amountValue != null && amountValue > 0) {
                                        viewModel.payCreditCard(
                                            creditCardId = creditCardId,
                                            sourceAccountId = selectedSourceAccount!!.id,
                                            amount = amountValue,
                                            memo = memo.ifBlank { "${creditCard.name}还款" }
                                        )
                                    } else {
                                        showSnackbar = true
                                        snackbarMessage = "请填写有效的还款金额"
                                        isError = true
                                    }
                                },
                                enabled = selectedSourceAccount != null &&
                                         amount.isNotBlank() &&
                                         amount.toDoubleOrNull() != null &&
                                         amount.toDoubleOrNull()!! > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("确认还款")
                            }
                        }
                    }
                } ?: run {
                    // 信用卡不存在的情况
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "未找到信用卡信息",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("返回")
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