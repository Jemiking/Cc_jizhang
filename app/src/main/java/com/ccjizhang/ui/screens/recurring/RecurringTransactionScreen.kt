package com.ccjizhang.ui.screens.recurring

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.RecurringTransactionViewModel
import com.ccjizhang.ui.viewmodels.RecurringTransactionTab
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionScreen(
    navController: NavHostController,
    onNavigateToAddRecurringTransaction: () -> Unit = { navController.navigate(NavRoutes.RecurringTransactionAdd) },
    onNavigateToEditRecurringTransaction: (Long) -> Unit = { id -> navController.navigate(NavRoutes.recurringTransactionEdit(id)) },
    onNavigateToRecurringTransactionDetail: (Long) -> Unit = { id -> navController.navigate(NavRoutes.recurringTransactionDetail(id)) },
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val transactions by viewModel.currentTabTransactions.collectAsState()
    val isLoading = false
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<RecurringTransaction?>(null) }
    
    Scaffold(
        topBar = {
            CCJiZhangTopAppBar(
                title = stringResource(R.string.recurring_transactions),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddRecurringTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_recurring_transaction)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                RecurringTransactionTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(RecurringTransactionTab.values()[index]) },
                        text = { Text(stringResource(tab.titleResId)) }
                    )
                }
            }

            when {
                isLoading -> {
                    LoadingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                transactions.isEmpty() -> {
                    EmptyContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        title = stringResource(R.string.no_recurring_transactions),
                        message = stringResource(R.string.no_recurring_transactions_message),
                        onActionClick = onNavigateToAddRecurringTransaction,
                        actionText = stringResource(R.string.add_recurring_transaction)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = transactions, key = { it.id }) { transaction: RecurringTransaction ->
                            RecurringTransactionItem(
                                transaction = transaction,
                                onItemClick = { onNavigateToRecurringTransactionDetail(transaction.id) },
                                onEditClick = { /* TODO: Navigate to edit? */ },
                                onDeleteClick = {
                                    selectedTransaction = transaction
                                    showDeleteDialog = true
                                },
                                onToggleStatus = { /* TODO: Add pause/resume? */ }
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog && selectedTransaction != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_recurring_transaction),
            message = stringResource(
                R.string.delete_recurring_transaction_confirmation,
                selectedTransaction?.description ?: ""
            ),
            onConfirm = {
                viewModel.deleteRecurringTransaction(selectedTransaction!!)
                showDeleteDialog = false
                selectedTransaction = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedTransaction = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionItem(
    transaction: RecurringTransaction,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance() }
    val viewModel: RecurringTransactionViewModel = hiltViewModel()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = currencyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.type == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "下次: ${viewModel.getNextExecutionDateDescription(transaction)}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "频率: ${viewModel.getRecurrenceTypeDescription(transaction)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

val RecurringTransactionTab.titleResId: Int
    @StringRes
    get() = when (this) {
        RecurringTransactionTab.ACTIVE -> R.string.app_name
        RecurringTransactionTab.PAUSED -> R.string.app_name
        RecurringTransactionTab.COMPLETED -> R.string.app_name
        RecurringTransactionTab.UPCOMING -> R.string.app_name
        RecurringTransactionTab.ALL -> R.string.app_name
    } 