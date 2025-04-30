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
import com.ccjizhang.data.model.Currency
import com.ccjizhang.data.service.CurrencyService
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.MultiCurrencyReportViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 多币种统计报表界面
 * 显示不同币种账户的资产统计和汇率信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiCurrencyReportScreen(
    navController: NavHostController,
    viewModel: MultiCurrencyReportViewModel = hiltViewModel()
) {
    // 获取数据
    val accounts by viewModel.accounts.collectAsState()
    val currencyGroups by viewModel.currencyGroups.collectAsState()
    val currencyTotals by viewModel.currencyTotals.collectAsState()
    val baseCurrencyTotal by viewModel.baseCurrencyTotal.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUpdatingRates by viewModel.isUpdatingRates.collectAsState()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    
    // 显示更新结果的Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    
    // 处理汇率更新结果
    LaunchedEffect(updateResult) {
        updateResult?.let {
            when (it) {
                is CurrencyService.UpdateResult.Success -> {
                    snackbarHostState.showSnackbar(it.message)
                }
                is CurrencyService.UpdateResult.Error -> {
                    snackbarHostState.showSnackbar(it.message)
                }
            }
            viewModel.clearUpdateResult()
        }
    }
    
    // 选择基准币种的对话框状态
    var showCurrencyDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("多币种资产报表") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.updateExchangeRates() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "更新汇率"
                        )
                    }
                    IconButton(onClick = { showCurrencyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "设置基准币种"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading || isUpdatingRates) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 总资产卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "总资产 (${baseCurrency.code})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.formatCurrency(baseCurrencyTotal, baseCurrency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            // 显示最近更新时间
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "最后更新: ${lastUpdateTime?.let { dateFormat.format(it) } ?: "从未更新"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    // 按币种分组显示
                    Text(
                        text = "按币种统计",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 每种币种的卡片
                    currencyGroups.forEach { (currency, accountsInCurrency) ->
                        val totalInThisCurrency = currencyTotals[currency] ?: 0.0
                        val totalInBaseCurrency = viewModel.convertCurrency(
                            totalInThisCurrency, 
                            currency, 
                            baseCurrency
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // 币种标题和总额
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 币种图标
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currency.symbol,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(
                                                text = currency.code,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "汇率: 1 ${baseCurrency.code} = ${String.format("%.4f", 1.0 / viewModel.getExchangeRate(currency))} ${currency.code}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = viewModel.formatCurrency(totalInThisCurrency, currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "≈ ${viewModel.formatCurrency(totalInBaseCurrency, baseCurrency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                // 该币种下的账户列表
                                accountsInCurrency.forEach { account ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = account.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = viewModel.formatCurrency(account.balance, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (account.balance >= 0) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // 基准币种选择对话框
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("选择基准币种") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    viewModel.getAllCurrencies().forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBaseCurrency(currency)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = currency.code,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            if (currency == baseCurrency) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
} 