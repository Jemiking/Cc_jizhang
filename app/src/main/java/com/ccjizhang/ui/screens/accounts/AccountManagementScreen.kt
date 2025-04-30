package com.ccjizhang.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Account
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.AccountViewModel

/**
 * 账户管理界面
 * 显示所有账户信息，并提供账户的添加、编辑和删除功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    navController: NavHostController,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }
    
    RoundedTopBarScaffold(
        title = "账户管理",
        navController = navController,
        actions = {
            IconButton(onClick = { navController.navigate(NavRoutes.AccountAdd) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加账户"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 账户总余额
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "总资产",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = viewModel.formatCurrency(totalBalance, baseCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // 显示基准币种信息
                    Text(
                        text = "基准币种: ${baseCurrency.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 账户管理操作卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionCard(
                    title = "转账",
                    icon = Icons.Default.CompareArrows,
                    onClick = { navController.navigate(NavRoutes.AccountTransfer) }
                )
                
                ActionCard(
                    title = "余额调整",
                    icon = Icons.Default.Edit,
                    onClick = { navController.navigate(NavRoutes.AccountBalanceAdjust) }
                )
                
                ActionCard(
                    title = "信用卡",
                    icon = Icons.Default.CreditCard,
                    onClick = { navController.navigate(NavRoutes.CreditCardList) }
                )
                
                ActionCard(
                    title = "币种设置",
                    icon = Icons.Default.AttachMoney,
                    onClick = { navController.navigate(NavRoutes.CurrencySettings) }
                )
                
                ActionCard(
                    title = "多币种报表",
                    icon = Icons.Default.PieChart,
                    onClick = { navController.navigate(NavRoutes.MultiCurrencyReport) }
                )
            }
            
            // 账户列表
            Text(
                text = "账户列表",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            accounts.forEach { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                        .clickable { navController.navigate(NavRoutes.accountDetail(account.id)) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 账户图标
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(account.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        
                        // 账户信息
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = account.type.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                
                                Text(
                                    text = account.currency.code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // 账户余额
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = viewModel.formatCurrency(account.balance, account.currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (account.balance >= 0) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            
                            if (account.isDefault) {
                                Text(
                                    text = "默认账户",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * 操作按钮组件
 */
@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 