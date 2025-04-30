package com.ccjizhang.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.theme.SurfaceLight
import com.ccjizhang.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主页屏幕 - MD3风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    onNavigateToTransactions: () -> Unit = { navController.navigate(NavRoutes.Transactions) },
    onNavigateToAccounts: () -> Unit = { navController.navigate(NavRoutes.Accounts) },
    onNavigateToBudgets: () -> Unit = { navController.navigate(NavRoutes.AllBudgets) },
    onNavigateToAnalysis: () -> Unit = { navController.navigate(NavRoutes.Analysis) },
    onNavigateToSettings: () -> Unit = { navController.navigate(NavRoutes.Settings) },
    onNavigateToAddTransaction: () -> Unit = { navController.navigate(NavRoutes.TransactionAdd) },
    onNavigateToSavingGoals: () -> Unit = { navController.navigate(NavRoutes.SavingGoals) },
    onNavigateToRecurringTransactions: () -> Unit = { navController.navigate(NavRoutes.RecurringTransactions) },
    onNavigateToInvestments: () -> Unit = { navController.navigate(NavRoutes.Investments) },
    onNavigateToReports: () -> Unit = { navController.navigate(NavRoutes.FinancialReports) },
    onNavigateToFamilySharing: () -> Unit = { navController.navigate(NavRoutes.FamilyMembers) },
    viewModel: HomeViewModel = hiltViewModel()
) {
    // 收集UI状态
    val uiState by viewModel.uiState.collectAsState()

    // 添加生命周期观察，在页面恢复焦点时刷新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 页面恢复时重新加载所有数据
                viewModel.loadHomeData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 侧边栏状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 侧边栏项目
    val drawerItems = remember {
        listOf(
            DrawerItem(
                title = "首页",
                icon = Icons.Default.Home,
                route = NavRoutes.Home,
                onClick = {}
            ),
            DrawerItem(
                title = "交易记录",
                icon = Icons.Default.Receipt,
                route = NavRoutes.Transactions,
                onClick = onNavigateToTransactions
            ),
            DrawerItem(
                title = "账户管理",
                icon = Icons.Default.AccountBalance,
                route = NavRoutes.AccountManagement,
                onClick = { navController.navigate(NavRoutes.AccountManagement) }
            ),
            DrawerItem(
                title = "预算管理",
                icon = Icons.Default.CreditCard,
                route = NavRoutes.AllBudgets,
                onClick = onNavigateToBudgets
            ),
            DrawerItem(
                title = "统计分析",
                icon = Icons.Default.PieChart,
                route = NavRoutes.Statistics,
                onClick = { navController.navigate(NavRoutes.Statistics) }
            ),
            DrawerItem(
                title = "储蓄目标",
                icon = Icons.Default.Savings,
                route = NavRoutes.SavingGoals,
                onClick = onNavigateToSavingGoals
            ),
            DrawerItem(
                title = "定期交易",
                icon = Icons.Default.Repeat,
                route = NavRoutes.RecurringTransactions,
                onClick = onNavigateToRecurringTransactions
            ),
            DrawerItem(
                title = "投资管理",
                icon = Icons.Default.TrendingUp,
                route = NavRoutes.Investments,
                onClick = onNavigateToInvestments
            ),
            DrawerItem(
                title = "财务报告",
                icon = Icons.Default.Assessment,
                route = NavRoutes.FinancialReports,
                onClick = onNavigateToReports
            ),
            DrawerItem(
                title = "家庭共享",
                icon = Icons.Default.People,
                route = NavRoutes.FamilyMembers,
                onClick = onNavigateToFamilySharing
            ),
            DrawerItem(
                title = "设置",
                icon = Icons.Default.Settings,
                route = NavRoutes.Settings,
                onClick = onNavigateToSettings
            ),
            DrawerItem(
                title = "帮助与反馈",
                icon = Icons.AutoMirrored.Filled.Help,
                route = "help",
                onClick = { /* 打开帮助页面 */ }
            )
        )
    }

    // 使用MD3风格的ModalNavigationDrawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                HomeDrawerContent(
                    items = drawerItems,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    totalBalance = uiState.totalBalance
                )
            }
        },
        gesturesEnabled = true
    ) {
        // 使用RoundedTopBarScaffold，但优化其视觉效果
        RoundedTopBarScaffold(
            title = "CC记账",
            navController = navController,
            navigationIcon = {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.padding(top = 4.dp) // 微调垂直位置
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = SurfaceLight,
                        modifier = Modifier.size(26.dp) // 增大图标尺寸
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { /* 打开通知 */ },
                    modifier = Modifier.padding(top = 4.dp) // 微调垂直位置
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "通知",
                        tint = SurfaceLight,
                        modifier = Modifier.size(26.dp) // 增大图标尺寸
                    )
                }
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                // 加载状态 - 使用更现代的加载指示器
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                // 主内容区域 - 优化间距和视觉层次
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(bottom = 88.dp), // 为底部导航栏留出空间
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 财务概览卡片
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FinancialOverviewCard(
                                totalBalance = uiState.totalBalance,
                                monthlyIncome = uiState.monthlyIncome,
                                monthlyExpense = uiState.monthlyExpense,
                                incomeChangePercent = uiState.incomeChangePercent,
                                expenseChangePercent = uiState.expenseChangePercent
                            )
                        }
                    }

                    // 3. 重要提醒（如果有）
                    if (uiState.upcomingBills.isNotEmpty() || uiState.budgetAlerts.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ImportantAlertsSection(
                                    upcomingBills = uiState.upcomingBills,
                                    budgetAlerts = uiState.budgetAlerts,
                                    onBillClick = { /* 处理账单点击 */ },
                                    onBudgetClick = { /* 处理预算点击 */ }
                                )
                            }
                        }
                    }

                    // 4. 近期交易标题
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            RecentTransactionsHeader(onViewAllClick = onNavigateToTransactions)
                        }
                    }

                    // 近期交易列表
                    if (uiState.recentTransactions.isEmpty()) {
                        item {
                            EmptyTransactionsMessage(onAddTransaction = onNavigateToAddTransaction)
                        }
                    } else {
                        items(uiState.recentTransactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { navigateToTransactionDetail(navController, transaction.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 侧边栏项目数据类
 */
data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val onClick: () -> Unit,
    val badge: Int? = null
)

/**
 * 侧边栏内容 - MD3风格
 */
@Composable
fun HomeDrawerContent(
    items: List<DrawerItem>,
    onCloseDrawer: () -> Unit,
    totalBalance: Double
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // 用户信息区域 - 使用更现代的布局
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                // 用户头像和名称
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    // 头像 - 使用Surface提供更好的视觉效果
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "用户头像",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 用户名 - 优化排版
                    Column {
                        Text(
                            text = "用户",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击登录/注册",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 总资产信息 - 使用ElevatedCard提供更好的视觉层次
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "总资产",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "¥ ${String.format("%,.2f", totalBalance)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 导航项目 - 使用更现代的分组和视觉效果
        // 主要功能标题
        Text(
            text = "主要功能",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Medium
        )

        // 主要功能项 - 优化视觉效果
        items.take(5).forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                selected = item.route == NavRoutes.Home,
                onClick = {
                    item.onClick()
                    onCloseDrawer()
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                badge = item.badge?.let { {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedContainerColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // 高级功能标题
        Text(
            text = "高级功能",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Medium
        )

        // 高级功能项 - 优化视觉效果
        items.subList(5, 10).forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                selected = false,
                onClick = {
                    item.onClick()
                    onCloseDrawer()
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                badge = item.badge?.let { {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // 设置与帮助标题
        Text(
            text = "设置与帮助",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            fontWeight = FontWeight.Medium
        )

        // 设置与帮助项 - 优化视觉效果
        items.subList(10, items.size).forEach { item ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                selected = false,
                onClick = {
                    item.onClick()
                    onCloseDrawer()
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                badge = item.badge?.let { {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// 处理导航函数
private fun navigateToTransactionDetail(navController: NavHostController, transactionId: Long) {
    navController.navigate(NavRoutes.transactionDetail(transactionId))
}

/**
 * 财务概览卡片 - MD3风格
 */
@Composable
fun FinancialOverviewCard(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double,
    incomeChangePercent: Double,
    expenseChangePercent: Double
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 总资产区域 - 更加突出
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "总资产",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "¥ ${String.format("%,.2f", totalBalance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 本月收支标题
            Text(
                text = "本月收支",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 收入和支出区域 - 使用更现代的布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 收入卡片
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "收入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "¥ ${String.format("%,.2f", monthlyIncome)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 环比变化 - 更现代的视觉效果
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            val icon = if (incomeChangePercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                            val color = if (incomeChangePercent >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = color.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (incomeChangePercent >= 0) "收入增加" else "收入减少",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(2.dp),
                                    tint = color
                                )
                            }

                            Text(
                                text = "${String.format("%.1f", kotlin.math.abs(incomeChangePercent))}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 支出卡片
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "支出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "¥ ${String.format("%,.2f", monthlyExpense)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 环比变化 - 更现代的视觉效果
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            // 支出增加是负面的，所以图标和颜色逻辑与收入相反
                            val icon = if (expenseChangePercent <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp
                            val color = if (expenseChangePercent <= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = color.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = if (expenseChangePercent <= 0) "支出减少" else "支出增加",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(2.dp),
                                    tint = color
                                )
                            }

                            Text(
                                text = "${String.format("%.1f", kotlin.math.abs(expenseChangePercent))}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}



/**
 * 重要提醒区域 - MD3风格
 */
@Composable
fun ImportantAlertsSection(
    upcomingBills: List<com.ccjizhang.ui.viewmodels.UpcomingBill>,
    budgetAlerts: List<com.ccjizhang.ui.viewmodels.BudgetAlert>,
    onBillClick: (Long) -> Unit,
    onBudgetClick: (Long) -> Unit
) {
    // 只有当有提醒时才显示此区域
    if (upcomingBills.isEmpty() && budgetAlerts.isEmpty()) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏带有"查看全部"按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "重要提醒",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = { /* 查看全部提醒 */ },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 账单提醒
            upcomingBills.forEach { bill ->
                AlertCard(
                    icon = Icons.Default.DateRange,
                    title = bill.title,
                    subtitle = "${bill.accountName} · 还款日: ${formatDate(bill.dueDate)}",
                    amount = bill.amount,
                    isExpense = true,
                    alertText = "还有${bill.daysLeft}天到期",
                    alertLevel = when {
                        bill.daysLeft <= 3 -> AlertLevel.HIGH
                        bill.daysLeft <= 7 -> AlertLevel.MEDIUM
                        else -> AlertLevel.LOW
                    },
                    onClick = { onBillClick(bill.id) }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 预算提醒
            budgetAlerts.forEach { alert ->
                AlertCard(
                    icon = Icons.Default.Warning,
                    title = alert.name,
                    subtitle = "已使用: ${String.format("%.1f", alert.percentage)}%",
                    amount = alert.spent,
                    isExpense = true,
                    alertText = "预算: ¥${String.format("%,.2f", alert.amount)}",
                    alertLevel = when {
                        alert.percentage >= 90 -> AlertLevel.HIGH
                        alert.percentage >= 80 -> AlertLevel.MEDIUM
                        else -> AlertLevel.LOW
                    },
                    onClick = { onBudgetClick(alert.id) }
                )

                if (alert != budgetAlerts.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 提醒级别枚举
 */
enum class AlertLevel {
    LOW, MEDIUM, HIGH
}

/**
 * 提醒卡片 - MD3风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amount: Double,
    isExpense: Boolean,
    alertText: String,
    alertLevel: AlertLevel,
    onClick: () -> Unit
) {
    val alertColor = when (alertLevel) {
        AlertLevel.HIGH -> MaterialTheme.colorScheme.error
        AlertLevel.MEDIUM -> MaterialTheme.colorScheme.errorContainer
        AlertLevel.LOW -> MaterialTheme.colorScheme.tertiary
    }

    val amountColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标 - 使用Surface提供更好的视觉效果
                Surface(
                    shape = CircleShape,
                    color = alertColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = alertColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 内容 - 优化布局和间距
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 警告文本使用Chip样式
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = alertColor.copy(alpha = 0.15f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = alertText,
                            style = MaterialTheme.typography.labelSmall,
                            color = alertColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // 金额 - 使用更现代的样式
                Text(
                    text = "${if (isExpense) "-" else "+"}¥${String.format("%,.2f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 近期交易区域 - MD3风格
 */
@Composable
fun RecentTransactionsHeader(onViewAllClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // 标题栏带有"查看全部"按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "近期交易",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 交易项 - MD3风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isIncome = transaction.isIncome
    val amountColor = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标 - 使用Surface提供更好的视觉效果
                Surface(
                    shape = CircleShape,
                    color = amountColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = if (isIncome) "收入" else "支出",
                            tint = amountColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 内容 - 优化布局和间距
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = transaction.note.ifEmpty { if (isIncome) "收入" else "支出" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 金额 - 使用更现代的样式
                Text(
                    text = "${if (isIncome) "+" else "-"}¥${String.format("%,.2f", kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 空交易提示 - MD3风格
 */
@Composable
fun EmptyTransactionsMessage(onAddTransaction: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 使用图标提供更好的视觉反馈
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "暂无交易记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onAddTransaction,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加交易")
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(date)
}