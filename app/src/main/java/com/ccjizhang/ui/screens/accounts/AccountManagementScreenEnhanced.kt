package com.ccjizhang.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.data.model.AccountSortType
import com.ccjizhang.data.model.displayName
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import androidx.compose.material.icons.filled.Check
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.AccountViewModel

/**
 * 增强版账户管理界面
 * 支持账户分类和排序功能
 */
@Composable
fun AccountManagementScreenEnhanced(
    navController: NavHostController,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val accountCategories by viewModel.accountCategories.collectAsState()
    val accountsGroupedByCategory by viewModel.accountsGroupedByCategory.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val baseCurrency by viewModel.baseCurrency.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    // 排序菜单状态
    var showSortMenu by remember { mutableStateOf(false) }

    // 分类展开状态
    val expandedCategories = remember { mutableStateOf(mutableSetOf<Long?>()) }

    // 使用rememberCoroutineScope而不是LaunchedEffect，以避免生命周期问题
    val coroutineScope = rememberCoroutineScope()

    // 在组件首次组合时加载数据
    LaunchedEffect(Unit) {
        println("DEBUG: AccountManagementScreen - LaunchedEffect 触发")
        coroutineScope.launch {
            try {
                println("DEBUG: AccountManagementScreen - 开始加载账户数据")
                viewModel.loadAccounts()
                println("DEBUG: AccountManagementScreen - 账户数据加载完成")
            } catch (e: Exception) {
                println("DEBUG: AccountManagementScreen - 加载账户数据失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    UnifiedScaffold(
        title = "账户管理",
        navController = navController,
        showFloatingActionButton = false,
        actions = {
            // 排序按钮
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "排序"
                )
            }

            // 分类管理按钮
            IconButton(onClick = { navController.navigate(NavRoutes.AccountCategoryManagement) }) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = "分类管理"
                )
            }

            // 添加账户按钮
            IconButton(onClick = { navController.navigate(NavRoutes.AccountAdd) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加账户"
                )
            }

            // 排序下拉菜单
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortMenuItem("自定义顺序", AccountSortType.CUSTOM, sortType) {
                    viewModel.setSortType(AccountSortType.CUSTOM)
                    showSortMenu = false
                }
                SortMenuItem("按名称", AccountSortType.NAME, sortType) {
                    viewModel.setSortType(AccountSortType.NAME)
                    showSortMenu = false
                }
                SortMenuItem("按余额", AccountSortType.BALANCE, sortType) {
                    viewModel.setSortType(AccountSortType.BALANCE)
                    showSortMenu = false
                }
                SortMenuItem("按类型", AccountSortType.TYPE, sortType) {
                    viewModel.setSortType(AccountSortType.TYPE)
                    showSortMenu = false
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 总资产卡片
            item {
                println("DEBUG: AccountManagementScreen - 渲染总资产卡片，totalBalance=$totalBalance")
                PrimaryCard(
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

                        // 添加调试信息
                        Text(
                            text = "账户数量: ${accounts.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 账户管理操作卡片
            item {
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
                }
            }

            // 分隔线
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 按分类显示账户（当排序方式为自定义时）
            println("DEBUG: AccountManagementScreen - 开始渲染账户列表，排序方式: $sortType, 账户总数: ${accounts.size}")

            if (accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "没有找到账户数据",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "请检查数据库连接或添加新账户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // 添加手动刷新按钮
                        Button(
                            onClick = {
                                println("DEBUG: AccountManagementScreen - 手动刷新按钮点击")
                                coroutineScope.launch {
                                    try {
                                        println("DEBUG: AccountManagementScreen - 开始手动刷新数据")
                                        viewModel.loadAccounts()
                                        println("DEBUG: AccountManagementScreen - 手动刷新数据完成")
                                    } catch (e: Exception) {
                                        println("DEBUG: AccountManagementScreen - 手动刷新数据失败: ${e.message}")
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("刷新数据")
                        }
                    }
                }
            } else if (sortType == AccountSortType.CUSTOM) {
                println("DEBUG: AccountManagementScreen - 使用分类视图渲染账户")
                // 未分类账户
                val uncategorizedAccounts = accountsGroupedByCategory[null] ?: emptyList()
                println("DEBUG: AccountManagementScreen - 未分类账户数量: ${uncategorizedAccounts.size}")

                if (uncategorizedAccounts.isNotEmpty()) {
                    item {
                        CategoryHeader(
                            category = null,
                            accountCount = uncategorizedAccounts.size,
                            isExpanded = expandedCategories.value.contains(null),
                            onToggleExpand = {
                                if (expandedCategories.value.contains(null)) {
                                    expandedCategories.value.remove(null)
                                } else {
                                    expandedCategories.value.add(null)
                                }
                            }
                        )
                    }

                    if (expandedCategories.value.contains(null)) {
                        items(uncategorizedAccounts) { account ->
                            AccountItem(
                                account = account,
                                viewModel = viewModel,
                                onClick = { navController.navigate(NavRoutes.accountDetail(account.id)) }
                            )
                        }
                    }
                }

                // 分类账户
                println("DEBUG: AccountManagementScreen - 分类数量: ${accountCategories.size}")
                accountCategories.forEach { category ->
                    val categoryAccounts = accountsGroupedByCategory[category.id] ?: emptyList()
                    println("DEBUG: AccountManagementScreen - 分类[${category.name}]账户数量: ${categoryAccounts.size}")

                    if (categoryAccounts.isNotEmpty()) {
                        item {
                            CategoryHeader(
                                category = category,
                                accountCount = categoryAccounts.size,
                                isExpanded = expandedCategories.value.contains(category.id),
                                onToggleExpand = {
                                    if (expandedCategories.value.contains(category.id)) {
                                        expandedCategories.value.remove(category.id)
                                    } else {
                                        expandedCategories.value.add(category.id)
                                    }
                                }
                            )
                        }

                        if (expandedCategories.value.contains(category.id)) {
                            items(categoryAccounts) { account ->
                                AccountItem(
                                    account = account,
                                    viewModel = viewModel,
                                    onClick = { navController.navigate(NavRoutes.accountDetail(account.id)) }
                                )
                            }
                        }
                    }
                }
            } else {
                // 按其他方式排序显示所有账户
                println("DEBUG: AccountManagementScreen - 使用列表视图渲染账户，数量: ${accounts.size}")
                items(accounts) { account ->
                    println("DEBUG: AccountManagementScreen - 渲染账户: ${account.name}, 余额: ${account.balance}")
                    AccountItem(
                        account = account,
                        viewModel = viewModel,
                        onClick = { navController.navigate(NavRoutes.accountDetail(account.id)) }
                    )
                }
            }

            // 底部空间
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 分类标题
 */
@Composable
private fun CategoryHeader(
    category: AccountCategory?,
    accountCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        if (category != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(category.color)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 分类名称
        Text(
            text = category?.name ?: "未分类",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // 账户数量
        Text(
            text = "$accountCount 个账户",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 展开/折叠图标
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = if (isExpanded) "折叠" else "展开",
            modifier = Modifier.rotate(rotationState)
        )
    }
}

/**
 * 账户项
 */
@Composable
private fun AccountItem(
    account: Account,
    viewModel: AccountViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
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
                    imageVector = getIconByName(account.icon),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 账户信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = account.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 账户余额
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = viewModel.formatCurrency(account.balance, account.currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (account.balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                if (account.currency != viewModel.baseCurrency.value) {
                    Text(
                        text = "≈ " + viewModel.formatCurrency(
                            viewModel.convertCurrency(
                                account.balance,
                                account.currency,
                                viewModel.baseCurrency.value
                            ),
                            viewModel.baseCurrency.value
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 排序菜单项
 */
@Composable
private fun SortMenuItem(
    title: String,
    sortType: AccountSortType,
    currentSortType: AccountSortType,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                Spacer(modifier = Modifier.weight(1f))
                if (sortType == currentSortType) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        onClick = onClick
    )
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

/**
 * 根据图标名称获取图标
 */
@Composable
private fun getIconByName(iconName: String): ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "payment" -> Icons.Default.CompareArrows
        else -> Icons.Default.AccountBalance
    }
}
