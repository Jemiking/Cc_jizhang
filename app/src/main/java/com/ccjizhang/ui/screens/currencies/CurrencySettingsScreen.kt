package com.ccjizhang.ui.screens.currencies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Currency
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.viewmodels.CurrencySettingsViewModel

/**
 * 币种设置界面
 * 用于设置基准币种和调整汇率
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    navController: NavHostController,
    viewModel: CurrencySettingsViewModel = hiltViewModel()
) {
    // 获取ViewModel中的数据
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()

    // 状态
    var showBaseCurrencyDialog by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf<Currency?>(null) }
    var editingRate by remember { mutableStateOf<Double?>(null) }
    var editingRateText by remember { mutableStateOf("") }

    // 保存状态
    var hasChanges by remember { mutableStateOf(false) }

    // 显示成功提示
    var showSnackbar by remember { mutableStateOf(false) }

    // 页面界面
    RoundedTopBarScaffold(
        title = "币种设置",
        navController = navController,
        showBackButton = true,
        actions = {
            if (hasChanges) {
                IconButton(onClick = {
                    viewModel.saveSettings()
                    showSnackbar = true
                    hasChanges = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "保存设置"
                    )
                }
            }
        }
    ) { paddingValues ->
        // 显示保存成功提示
        if (showSnackbar) {
            LaunchedEffect(Unit) {
                // 自动关闭提示
                kotlinx.coroutines.delay(2000)
                showSnackbar = false
            }

            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text("关闭")
                    }
                }
            ) {
                Text("设置已保存")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基准币种设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "基准币种",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "所有金额将基于此币种进行换算和统计",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 显示当前基准币种
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showBaseCurrencyDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${baseCurrency.symbol} ${baseCurrency.code}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "修改基准币种",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 汇率设置列表
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "汇率设置",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "设置各币种相对于基准币种(${baseCurrency.code})的汇率",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 显示所有币种的汇率设置
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (currency in availableCurrencies) {
                            if (currency == baseCurrency) continue

                            val rate = exchangeRates[currency] ?: 1.0
                            val isEditing = selectedCurrency == currency

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 币种信息
                                Text(
                                    text = "${currency.symbol} ${currency.code}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                // 汇率输入框
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editingRateText,
                                        onValueChange = {
                                            editingRateText = it
                                            it.toDoubleOrNull()?.let { value ->
                                                editingRate = value
                                                viewModel.updateExchangeRate(currency, value)
                                                hasChanges = true
                                            }
                                        },
                                        modifier = Modifier.width(120.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                selectedCurrency = null
                                            }) {
                                                Icon(Icons.Default.Check, "完成")
                                            }
                                        }
                                    )
                                } else {
                                    // 汇率显示
                                    Text(
                                        text = String.format("%.4f", rate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 编辑按钮
                                    IconButton(
                                        onClick = {
                                            selectedCurrency = currency
                                            editingRate = rate
                                            editingRateText = String.format("%.4f", rate)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "编辑汇率",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (currency != availableCurrencies.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            // 说明信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "汇率说明",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. 基准币种的汇率固定为1.0\n" +
                              "2. 其他币种的汇率表示：1单位基准币种 = X单位其他币种\n" +
                              "3. 例如：如果基准币种为CNY，USD汇率为0.14，表示1元人民币=0.14美元",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // 基准币种选择对话框
        if (showBaseCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showBaseCurrencyDialog = false },
                title = { Text("选择基准币种") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        availableCurrencies.forEach { currency ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setBaseCurrency(currency)
                                        showBaseCurrencyDialog = false
                                        hasChanges = true
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currency == baseCurrency,
                                    onClick = {
                                        viewModel.setBaseCurrency(currency)
                                        showBaseCurrencyDialog = false
                                        hasChanges = true
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "${currency.symbol} ${currency.code}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBaseCurrencyDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}