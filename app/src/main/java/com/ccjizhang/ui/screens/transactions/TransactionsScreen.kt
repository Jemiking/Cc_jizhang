package com.ccjizhang.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.ui.components.CategoryIcon
import com.ccjizhang.ui.components.MainTopAppBar
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.TransactionItem
import com.ccjizhang.ui.screens.transactions.EmptySearchState
import com.ccjizhang.ui.screens.transactions.EmptyTransactionState
import com.ccjizhang.ui.screens.transactions.TransactionSkeletonLoader
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.theme.CategoryBlue
import com.ccjizhang.ui.theme.CategoryOrange
import com.ccjizhang.ui.theme.ExpenseRed
import com.ccjizhang.ui.theme.IncomeGreen
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.viewmodels.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.theme.CCJiZhangTheme

/**
 * 交易列表分组方式
 */
enum class TransactionGroupBy {
    DAY, WEEK, MONTH, YEAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onNavigateToAddTransaction: () -> Unit = { navController.navigate(NavRoutes.TransactionAdd) },
    onNavigateToTransactionDetail: (Long) -> Unit = { id -> navController.navigate(NavRoutes.transactionDetail(id)) },
    viewModel: TransactionViewModel = hiltViewModel()
) {
    // 收集交易列表数据流
    val transactions = viewModel.transactions.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val filterType = viewModel.filterType.collectAsState().value

    // 分组方式状态
    var groupBy by remember { mutableStateOf(TransactionGroupBy.DAY) }

    // 批量操作状态
    var isBatchModeActive by remember { mutableStateOf(false) }
    var selectedTransactions by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // 批量删除确认对话框状态
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // 批量操作菜单状态
    var showBatchMenu by remember { mutableStateOf(false) }

    RoundedTopBarScaffold(
        title = if (isBatchModeActive) "已选择 ${selectedTransactions.size} 项" else "交易历史",
        navController = navController,
        showBackButton = isBatchModeActive,
        actions = {
            if (isBatchModeActive) {
                // 批量操作模式下的操作按钮
                // 全选按钮
                IconButton(
                    onClick = {
                        if (selectedTransactions.size == transactions.size) {
                            // 如果已经全选，则取消全选
                            selectedTransactions = emptySet()
                        } else {
                            // 否则全选
                            selectedTransactions = transactions.map { it.id }.toSet()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTransactions.size == transactions.size)
                                        Icons.Default.DoneAll else Icons.Default.SelectAll,
                        contentDescription = if (selectedTransactions.size == transactions.size)
                                              "取消全选" else "全选",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // 复制按钮
                IconButton(
                    onClick = {
                        // 实现复制交易
                        viewModel.batchCopyTransactions(selectedTransactions.toList())
                        isBatchModeActive = false
                        selectedTransactions = emptySet()
                    },
                    enabled = selectedTransactions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = "复制交易",
                        tint = if (selectedTransactions.isNotEmpty())
                               MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                }

                // 删除按钮
                IconButton(
                    onClick = { if (selectedTransactions.isNotEmpty()) showBatchDeleteDialog = true },
                    enabled = selectedTransactions.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "批量删除",
                        tint = if (selectedTransactions.isNotEmpty())
                               MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                    )
                }

                // 取消按钮
                IconButton(onClick = {
                    isBatchModeActive = false
                    selectedTransactions = emptySet()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消选择",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                // 正常模式下的筛选按钮
                IconButton(onClick = { /* 高级筛选 */ }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "筛选",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // 进入批量选择模式
                IconButton(onClick = { isBatchModeActive = true }) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "批量选择",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        // 主要内容
        TransactionsContent(
            paddingValues = paddingValues,
            onTransactionClick = { transactionId ->
                if (isBatchModeActive) {
                    // 批量选择模式下，点击项目切换选择状态
                    selectedTransactions = if (selectedTransactions.contains(transactionId)) {
                        selectedTransactions - transactionId
                    } else {
                        selectedTransactions + transactionId
                    }
                } else {
                    // 正常模式下，点击项目进入详情页
                    onNavigateToTransactionDetail(transactionId)
                }
            },
            transactions = transactions,
            isLoading = isLoading,
            filterType = filterType,
            onFilterTypeChanged = viewModel::setFilterType,
            groupBy = groupBy,
            onGroupByChanged = { groupBy = it },
            isBatchModeActive = isBatchModeActive,
            selectedTransactions = selectedTransactions,
            navController = navController
        )

        // 批量删除确认对话框
        if (showBatchDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("确认批量删除") },
                text = {
                    Column {
                        Text(
                            "确定要删除选中的 ${selectedTransactions.size} 条交易记录吗？",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "此操作无法撤销，删除后数据将无法恢复。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 执行批量删除
                            viewModel.batchDeleteTransactions(selectedTransactions.toList())
                            showBatchDeleteDialog = false
                            isBatchModeActive = false
                            selectedTransactions = emptySet()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            "删除",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsContent(
    paddingValues: PaddingValues,
    onTransactionClick: (Long) -> Unit,
    transactions: List<Transaction>,
    isLoading: Boolean,
    filterType: com.ccjizhang.data.paging.TransactionFilterType,
    onFilterTypeChanged: (com.ccjizhang.data.paging.TransactionFilterType) -> Unit,
    groupBy: TransactionGroupBy,
    onGroupByChanged: (TransactionGroupBy) -> Unit,
    isBatchModeActive: Boolean = false,
    selectedTransactions: Set<Long> = emptySet(),
    navController: NavHostController
) {
    val viewModel: TransactionViewModel = hiltViewModel()

    // 收集搜索相关的状态
    val searchQuery = viewModel.searchQuery.collectAsState().value
    val isSearchMode = viewModel.isSearchMode.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // 搜索框
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 1.dp
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        "搜索交易记录",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // 显示语音搜索图标
                        IconButton(onClick = { /* 语音搜索功能 */ }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "语音搜索",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    containerColor = Color.Transparent
                )
            )
        }

        // 只有在非搜索模式下才显示过滤选项
        if (!isSearchMode) {
            // 先显示筛选选项
            FilterOptions(
                selectedFilter = when(filterType) {
                    com.ccjizhang.data.paging.TransactionFilterType.ALL -> "全部"
                    com.ccjizhang.data.paging.TransactionFilterType.EXPENSE -> "支出"
                    com.ccjizhang.data.paging.TransactionFilterType.INCOME -> "收入"
                },
                onFilterSelected = { filterString ->
                    val newFilter = when(filterString) {
                        "全部" -> com.ccjizhang.data.paging.TransactionFilterType.ALL
                        "支出" -> com.ccjizhang.data.paging.TransactionFilterType.EXPENSE
                        "收入" -> com.ccjizhang.data.paging.TransactionFilterType.INCOME
                        else -> com.ccjizhang.data.paging.TransactionFilterType.ALL
                    }
                    onFilterTypeChanged(newFilter)
                }
            )

            // 再显示分组选择器
            GroupByOptions(
                selectedGroupBy = groupBy,
                onGroupBySelected = onGroupByChanged
            )
        }

        // 搜索模式下显示搜索结果提示
        if (isSearchMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "搜索结果",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text(
                                text = "找到 ${transactions.size} 条相关交易",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.clearSearch() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "清除",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (isLoading) {
            // 显示骨架屏加载效果
            TransactionSkeletonLoader()
        } else {
            // 如果没有交易记录且在搜索模式下，显示搜索空状态
            if (transactions.isEmpty() && isSearchMode) {
                EmptySearchState(searchQuery = searchQuery, onClearSearch = { viewModel.clearSearch() })
            }
            // 如果没有交易记录且不在搜索模式下，显示空交易状态
            else if (transactions.isEmpty()) {
                EmptyTransactionState(onAddTransaction = { navController.navigate(NavRoutes.TransactionAdd) })
            }
            // 否则显示交易列表
            else {
                RealTransactionsList(
                    transactions = transactions,
                    onTransactionClick = onTransactionClick,
                    groupBy = groupBy,
                    isBatchModeActive = isBatchModeActive,
                    selectedTransactions = selectedTransactions
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterOptions(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filterOptions = listOf(
        Triple("全部", Icons.Default.FilterList, MaterialTheme.colorScheme.primary),
        Triple("支出", Icons.Default.ArrowDownward, ExpenseRed),
        Triple("收入", Icons.Default.ArrowUpward, IncomeGreen)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterOptions) { (option, icon, color) ->
            val isSelected = option == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(option) },
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else color,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

// 添加分组选项组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupByOptions(
    selectedGroupBy: TransactionGroupBy,
    onGroupBySelected: (TransactionGroupBy) -> Unit
) {
    val groupOptions = remember {
        listOf(
            Triple("按天", Icons.Default.CalendarViewDay, TransactionGroupBy.DAY),
            Triple("按周", Icons.Default.ViewWeek, TransactionGroupBy.WEEK),
            Triple("按月", Icons.Default.CalendarMonth, TransactionGroupBy.MONTH),
            Triple("按年", Icons.Default.CalendarToday, TransactionGroupBy.YEAR)
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groupOptions) { (label, icon, option) ->
            val isSelected = option == selectedGroupBy
            FilterChip(
                selected = isSelected,
                onClick = { onGroupBySelected(option) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

// 保留原来的函数以避免破坏其他依赖它的地方
@Composable
fun TransactionsList(onTransactionClick: (Long) -> Unit) {
    val todayTransactions = remember {
        listOf(
            TransactionData(1L, "午餐", "餐饮", 55.0, true, "12:30", Icons.Default.Restaurant, PrimaryDark),
            TransactionData(2L, "咖啡", "餐饮", 26.0, true, "10:15", Icons.Default.Coffee, PrimaryDark)
        )
    }

    val yesterdayTransactions = remember {
        listOf(
            TransactionData(3L, "超市购物", "食品杂货", 326.5, true, "18:15", Icons.Default.ShoppingBag, CategoryBlue),
            TransactionData(4L, "打车", "交通", 45.0, true, "17:30", Icons.Default.DirectionsBus, CategoryBlue),
            TransactionData(5L, "网上购物", "购物", 281.0, true, "14:20", Icons.Default.ShoppingBag, CategoryBlue)
        )
    }

    val lastMonthTransactions = remember {
        listOf(
            TransactionData(6L, "工资", "收入", 12000.0, false, "09:00", Icons.Default.MoneyOff, IncomeGreen),
            TransactionData(7L, "游戏订阅", "娱乐", 98.0, true, "08:30", Icons.Default.Restaurant, CategoryOrange),
            TransactionData(8L, "视频会员", "娱乐", 52.0, true, "08:30", Icons.Default.Restaurant, CategoryOrange)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            TransactionDateGroup(
                date = "今天",
                income = 0.0,
                expense = 81.0
            )
        }

        items(todayTransactions) { transaction ->
            TransactionItem(
                icon = transaction.icon,
                iconBackground = transaction.iconColor,
                title = transaction.title,
                category = transaction.category,
                amount = transaction.amount,
                isExpense = transaction.isExpense,
                time = transaction.time,
                onClick = { onTransactionClick(transaction.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            TransactionDateGroup(
                date = "昨天",
                income = 0.0,
                expense = 652.5
            )
        }

        items(yesterdayTransactions) { transaction ->
            TransactionItem(
                icon = transaction.icon,
                iconBackground = transaction.iconColor,
                title = transaction.title,
                category = transaction.category,
                amount = transaction.amount,
                isExpense = transaction.isExpense,
                time = transaction.time,
                onClick = { onTransactionClick(transaction.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            TransactionDateGroup(
                date = "4月1日",
                income = 12000.0,
                expense = 150.0
            )
        }

        items(lastMonthTransactions) { transaction ->
            TransactionItem(
                icon = transaction.icon,
                iconBackground = transaction.iconColor,
                title = transaction.title,
                category = transaction.category,
                amount = transaction.amount,
                isExpense = transaction.isExpense,
                time = transaction.time,
                onClick = { onTransactionClick(transaction.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// 更新RealTransactionsList以支持不同分组方式
@Composable
fun RealTransactionsList(
    transactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit,
    groupBy: TransactionGroupBy,
    isBatchModeActive: Boolean = false,
    selectedTransactions: Set<Long> = emptySet()
) {
    if (transactions.isEmpty()) {
        // 空列表提示
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MoneyOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "没有交易记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "点击底部按钮添加新交易",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // 根据不同的分组方式对交易进行分组
    val groupedTransactions = when (groupBy) {
        TransactionGroupBy.DAY -> groupTransactionsByDay(transactions)
        TransactionGroupBy.WEEK -> groupTransactionsByWeek(transactions)
        TransactionGroupBy.MONTH -> groupTransactionsByMonth(transactions)
        TransactionGroupBy.YEAR -> groupTransactionsByYear(transactions)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp) // 为底部导航或FAB留出空间
    ) {
        groupedTransactions.forEach { (headerText, transactionsInGroup) ->
            // 分组头部
            item {
                TransactionGroupHeader(
                    title = headerText,
                    income = transactionsInGroup.filter { it.isIncome }.sumOf { it.amount },
                    expense = transactionsInGroup.filter { !it.isIncome }.sumOf { it.amount }
                )
            }

            // 组内交易记录
            items(transactionsInGroup) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) },
                    isBatchModeActive = isBatchModeActive,
                    isSelected = selectedTransactions.contains(transaction.id)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    isBatchModeActive: Boolean = false,
    isSelected: Boolean = false
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    // 格式化日期时间
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(transaction.date)

    // 格式化金额
    val amountText = String.format("%.2f", transaction.amount)
    val formattedAmount = if (transaction.isIncome) "+¥$amountText" else "-¥$amountText"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择指示器
            if (isBatchModeActive) {
                Box(
                    modifier = Modifier
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选择",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // 分类图标 - 使用更鲜明的颜色
            val iconBackgroundColor = if (transaction.isIncome) {
                IncomeGreen.copy(alpha = 0.15f)
            } else {
                ExpenseRed.copy(alpha = 0.15f)
            }

            val iconTint = if (transaction.isIncome) {
                IncomeGreen
            } else {
                ExpenseRed
            }

            if (transaction.categoryId != null) {
                CategoryIcon(
                    categoryId = transaction.categoryId,
                    backgroundColor = iconBackgroundColor,
                    tint = iconTint,
                    size = 40.dp
                )
            } else {
                // 显示默认图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBackgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MiscellaneousServices,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 交易信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 交易备注/标题
                    Text(
                        text = transaction.note.ifEmpty { "无备注" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // 时间
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 账户名称
                    Text(
                        text = "账户名称", // 从账户仓库获取名称
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 金额
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (transaction.isIncome) IncomeGreen else ExpenseRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionGroupHeader(
    title: String,
    income: Double,
    expense: Double
) {
    // 格式化金额
    val formattedIncome = String.format("%.2f", income)
    val formattedExpense = String.format("%.2f", expense)
    val netAmount = income - expense
    val formattedNetAmount = String.format("%.2f", netAmount)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 净收支
                val netColor = when {
                    netAmount > 0 -> IncomeGreen
                    netAmount < 0 -> ExpenseRed
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val netPrefix = when {
                    netAmount > 0 -> "+"
                    netAmount < 0 -> "-"
                    else -> ""
                }

                val displayNetAmount = if (netAmount < 0) -netAmount else netAmount

                Text(
                    text = "净收支: ${netPrefix}¥$formattedNetAmount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = netColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 收支详情行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 收入
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(IncomeGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "收入: +¥$formattedIncome",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen
                    )
                }

                // 支出
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ExpenseRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "支出: -¥$formattedExpense",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

/**
 * 按天分组交易
 */
private fun groupTransactionsByDay(transactions: List<Transaction>): Map<String, List<Transaction>> {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    // 使用LinkedHashMap保持顺序
    val result = mutableMapOf<String, MutableList<Transaction>>()

    // 按日期分组
    transactions.forEach { transaction ->
        calendar.time = transaction.date

        val headerText = when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"

            calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "昨天"

            else -> dateFormatter.format(transaction.date)
        }

        if (!result.containsKey(headerText)) {
            result[headerText] = mutableListOf()
        }

        result[headerText]?.add(transaction)
    }

    return result
}

/**
 * 按周分组交易
 */
private fun groupTransactionsByWeek(transactions: List<Transaction>): Map<String, List<Transaction>> {
    // 实现逻辑略
    return mapOf("本周" to transactions)
}

/**
 * 按月分组交易
 */
private fun groupTransactionsByMonth(transactions: List<Transaction>): Map<String, List<Transaction>> {
    // 实现逻辑略
    return mapOf("本月" to transactions)
}

/**
 * 按年分组交易
 */
private fun groupTransactionsByYear(transactions: List<Transaction>): Map<String, List<Transaction>> {
    // 实现逻辑略
    return mapOf("今年" to transactions)
}

@Composable
fun TransactionDateGroup(
    date: String,
    income: Double,
    expense: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "支出: ¥$expense | 收入: ¥$income",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

data class TransactionData(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val time: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Preview(
    name = "交易记录页面预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewTransactionsScreen() {
    CCJiZhangTheme {
        // 创建一个模拟的NavController用于预览
        val navController = rememberNavController()
        // 显示TransactionsScreen
        TransactionsScreen(navController = navController)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewFilterOptions() {
    CCJiZhangTheme {
        FilterOptions(selectedFilter = "全部", onFilterSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTransactionDateGroup() {
    CCJiZhangTheme {
        TransactionDateGroup(
            date = "今天",
            income = 0.0,
            expense = 81.0
        )
    }
}

