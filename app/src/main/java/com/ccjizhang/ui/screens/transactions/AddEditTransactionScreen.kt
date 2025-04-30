package com.ccjizhang.ui.screens.transactions

import android.location.Location
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.Icon as MaterialIcon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ccjizhang.ui.components.CategoryIcon
import com.ccjizhang.ui.components.ImagePicker
import com.ccjizhang.ui.components.LocationPicker
import com.ccjizhang.ui.theme.ExpenseRed
import com.ccjizhang.ui.theme.IncomeGreen
import com.ccjizhang.ui.viewmodels.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.ui.common.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel(),
    transactionId: Long = 0L
) {
    // 状态
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf(1L) } // 默认账户ID
    var categoryId by remember { mutableStateOf(1L) } // 默认分类ID
    var date by remember { mutableStateOf(Date()) }
    var amountError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 获取已选的图片URI和位置
    val selectedImage = viewModel.selectedImageUri.collectAsState().value
    val locationAddress = viewModel.locationAddress.collectAsState().value

    // 获取错误信息
    val errorMessage = viewModel.errorMessage.collectAsState().value

    // 模拟账户和分类列表，实际应该从ViewModel获取
    val accounts = remember {
        listOf(
            1L to "现金账户",
            2L to "银行卡",
            3L to "支付宝",
            4L to "微信"
        )
    }

    val categories = remember {
        listOf(
            1L to "餐饮",
            2L to "购物",
            3L to "交通",
            4L to "住房",
            5L to "娱乐",
            6L to "医疗",
            7L to "教育",
            8L to "工资",
            9L to "奖金",
            10L to "投资收益"
        )
    }

    // 自动获取焦点
    val amountFocusRequester = remember { FocusRequester() }

    // 如果是编辑模式，加载交易数据
    if (transactionId > 0) {
        LaunchedEffect(key1 = transactionId) {
            viewModel.loadTransactionDetails(transactionId)
        }

        // 使用collectAsState监听交易数据变化
        val transaction = viewModel.selectedTransaction.collectAsState().value
        transaction?.let {
            LaunchedEffect(key1 = transaction) {
                amount = transaction.amount.toString()
                note = transaction.note
                isIncome = transaction.isIncome
                accountId = transaction.accountId
                categoryId = transaction.categoryId ?: 0L
                date = transaction.date
            }
        }
    } else {
        LaunchedEffect(Unit) {
            amountFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId > 0) "编辑交易" else "添加交易") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 验证输入
                    if (amount.isBlank() || amount.toDoubleOrNull() == null) {
                        amountError = true
                        return@FloatingActionButton
                    }

                    // 保存交易记录
                    val amountValue = amount.toDouble()

                    // 使用viewModel的方法保存交易
                    if (transactionId > 0) {
                        viewModel.updateTransaction(
                            Transaction(
                                id = transactionId,
                                amount = amountValue,
                                note = note,
                                isIncome = isIncome,
                                accountId = accountId,
                                categoryId = categoryId,
                                date = date
                            )
                        )
                    } else {
                        viewModel.addTransaction(
                            Transaction(
                                amount = amountValue,
                                note = note,
                                isIncome = isIncome,
                                accountId = accountId,
                                categoryId = categoryId,
                                date = date
                            )
                        )
                    }

                    // 返回上一页
                    navController.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "保存"
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 收入/支出选择器
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "交易类型",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        // 简化版分段按钮，使用Row和Button代替SegmentedButton
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { isIncome = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isIncome) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                            ) {
                                Text("支出")
                            }

                            Button(
                                onClick = { isIncome = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                            ) {
                                Text("收入")
                            }
                        }
                    }
                }

                // 金额输入
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        amountError = false
                    },
                    label = { Text("金额") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = { Text("¥") },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("请输入有效金额") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // 账户选择
                var accountMenuExpanded by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = accounts.find { it.first == accountId }?.second ?: "选择账户",
                    onValueChange = {},
                    label = { Text("账户") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { accountMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, "展开账户选择")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                DropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.second) },
                            onClick = {
                                accountId = account.first
                                accountMenuExpanded = false
                            }
                        )
                    }
                }

                // 分类选择
                var categoryMenuExpanded by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = categories.find { it.first == categoryId }?.second ?: "选择分类",
                    onValueChange = {},
                    label = { Text("分类") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { categoryMenuExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, "展开分类选择")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.second) },
                            onClick = {
                                categoryId = category.first
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }

                // 日期选择
                val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
                val formattedDate = dateFormatter.format(date)

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    label = { Text("日期") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "选择日期")
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                // 备注输入
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                // 图片选择器
                ImagePicker(
                    currentImageUri = selectedImage,
                    onImageSelected = { uri -> viewModel.setSelectedImageUri(uri) }
                )

                // 位置选择器
                LocationPicker(
                    currentLocation = locationAddress,
                    onLocationSelected = { address -> viewModel.setLocationAddress(address) },
                    onGetAddressFromLocation = { latitude, longitude ->
                        viewModel.getAddressFromLatLng(latitude, longitude)
                    }
                )

                // 错误提示
                errorMessage?.let { error ->
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "错误",
                                tint = androidx.compose.ui.graphics.Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = androidx.compose.ui.graphics.Color.Red
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                MaterialIcon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "关闭",
                                    tint = androidx.compose.ui.graphics.Color.Red
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // 为FAB留出空间
            }
        }

        // 日期选择器对话框
        if (showDatePicker) {
            DatePickerDialog(
                initialDate = date,
                onDateSelected = { newDate ->
                    date = newDate
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}