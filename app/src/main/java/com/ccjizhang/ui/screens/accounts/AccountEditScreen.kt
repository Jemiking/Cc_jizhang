package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.Currency
import com.ccjizhang.ui.components.CurrencyDropdown
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.viewmodels.AccountViewModel
import com.ccjizhang.ui.common.OperationResult
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * 账户编辑页面
 * 用于添加或编辑账户信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    navController: NavHostController,
    accountId: Long? = null,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val isEditing = accountId != null && accountId > 0

    // 账户信息状态
    var accountName by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.CASH) }
    var initialBalance by remember { mutableStateOf("0.0") }
    var currency by remember { mutableStateOf(Currency.CNY) }
    var isDefault by remember { mutableStateOf(false) }
    var includeInTotal by remember { mutableStateOf(true) }

    // 操作状态
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 如果是编辑模式，加载现有账户数据
    LaunchedEffect(accountId) {
        if (isEditing) {
            viewModel.getAccountById(accountId!!)
        }
    }

    // 监听选中的账户
    val selectedAccount by viewModel.selectedAccount.collectAsState()

    // 当选中账户发生变化时更新界面
    LaunchedEffect(selectedAccount) {
        selectedAccount?.let { account ->
            accountName = account.name
            accountType = account.type
            initialBalance = account.balance.toString()
            currency = account.currency
            isDefault = account.isDefault
            includeInTotal = account.includeInTotal
        }
    }

    // 操作结果处理
    val operationResult by viewModel.operationResult.collectAsState()

    // 使用LaunchedEffect监听operationResult的变化
    LaunchedEffect(operationResult) {
        println("DEBUG: AccountEditScreen - operationResult变化: $operationResult")
        val result = operationResult
        if (result != null) {
            when (result) {
                is OperationResult.Loading -> {
                    // Optionally show loading indicator
                    println("DEBUG: AccountEditScreen - 操作加载中")
                }
                is OperationResult.Success -> {
                    println("DEBUG: AccountEditScreen - 操作成功: ${result.message}")
                    scope.launch {
                        snackbarHostState.showSnackbar(message = result.message ?: "操作成功")
                    }

                    // 操作成功后导航回账户管理页面
                    if (result.message?.contains("添加账户成功") == true ||
                        result.message?.contains("更新账户成功") == true) {
                        println("DEBUG: AccountEditScreen - 保存成功，返回账户管理页面")
                        try {
                            navController.popBackStack()
                            println("DEBUG: AccountEditScreen - popBackStack 成功")
                        } catch (e: Exception) {
                            println("DEBUG: AccountEditScreen - popBackStack 失败: ${e.message}")
                            e.printStackTrace()
                            // 尝试直接导航到账户管理页面
                            try {
                                navController.navigate(NavRoutes.Accounts) {
                                    popUpTo(NavRoutes.Accounts) { inclusive = true }
                                }
                                println("DEBUG: AccountEditScreen - 直接导航到账户管理页面成功")
                            } catch (e2: Exception) {
                                println("DEBUG: AccountEditScreen - 直接导航到账户管理页面失败: ${e2.message}")
                                e2.printStackTrace()
                            }
                        }
                    } else {
                        println("DEBUG: AccountEditScreen - 不满足跳转条件，消息内容: ${result.message}")
                    }

                    // 清除操作结果，避免重复处理
                    viewModel.clearOperationResult()
                }
                is OperationResult.Error -> {
                    println("DEBUG: AccountEditScreen - 操作失败: ${result.message}")
                    scope.launch {
                        snackbarHostState.showSnackbar(message = "操作失败: ${result.message}", duration = SnackbarDuration.Long)
                    }
                    // 清除操作结果，避免重复处理
                    viewModel.clearOperationResult()
                }
                else -> {
                    println("DEBUG: AccountEditScreen - 未知操作结果类型")
                }
            }
        }
    }

    // 页面标题
    val title = if (isEditing) "编辑账户" else "添加账户"

    UnifiedScaffold(
        title = title,
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = {
                println("DEBUG: AccountEditScreen - 保存按钮点击")
                // 保存账户信息
                val balance = initialBalance.toDoubleOrNull() ?: 0.0
                val account = if (isEditing) {
                    println("DEBUG: AccountEditScreen - 编辑现有账户")
                    selectedAccount?.copy(
                        name = accountName,
                        type = accountType,
                        balance = balance,
                        currency = currency,
                        isDefault = isDefault,
                        includeInTotal = includeInTotal
                    )
                } else {
                    println("DEBUG: AccountEditScreen - 创建新账户")
                    Account(
                        name = accountName,
                        type = accountType,
                        balance = balance,
                        currency = currency,
                        isDefault = isDefault,
                        includeInTotal = includeInTotal,
                        color = 0xFF2196F3.toInt() // 默认蓝色
                    )
                }

                account?.let {
                    println("DEBUG: AccountEditScreen - 账户信息准备完成: ${it.name}, 余额: ${it.balance}")
                    if (isEditing) {
                        println("DEBUG: AccountEditScreen - 调用 updateAccount")
                        viewModel.updateAccount(it)
                    } else {
                        println("DEBUG: AccountEditScreen - 调用 addAccount")
                        viewModel.addAccount(it)

                        // 添加延迟导航逻辑，确保数据保存完成后再导航
                        scope.launch {
                            // 延迟1秒，确保数据保存完成
                            kotlinx.coroutines.delay(1000)
                            println("DEBUG: AccountEditScreen - 延迟导航执行")
                            try {
                                navController.popBackStack()
                                println("DEBUG: AccountEditScreen - 延迟导航成功")
                            } catch (e: Exception) {
                                println("DEBUG: AccountEditScreen - 延迟导航失败: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                    println("DEBUG: AccountEditScreen - 保存操作完成")
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "保存"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 账户名称
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("账户名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 账户类型
            AccountTypeDropdown(
                selectedType = accountType,
                onTypeSelected = { accountType = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 初始余额
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { initialBalance = it },
                label = { Text("初始余额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 币种选择
            CurrencyDropdown(
                selectedCurrency = currency,
                onCurrencySelected = { currency = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // 账户设置选项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "默认账户",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "计入总资产",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                Button(
                    onClick = {
                        selectedAccount?.let {
                            viewModel.deleteAccount(it.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除账户")
                }
            }
        }
    }

    // 显示操作结果
    operationResult?.let {
        if (it is OperationResult.Error) {
            val errorMessage = it.message
            LaunchedEffect(errorMessage) {
                // 清除错误状态
                viewModel.clearOperationResult()
            }

            AlertDialog(
                onDismissRequest = { viewModel.clearOperationResult() },
                title = { Text("错误") },
                text = { Text(text = errorMessage) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearOperationResult() }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 账户类型下拉选择组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val accountTypes = AccountType.values()

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedType.name,
            onValueChange = { },
            readOnly = true,
            label = { Text("账户类型") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "展开")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp)
        ) {
            accountTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}