package com.ccjizhang.ui.screens.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.R
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.components.InfoRow
import com.ccjizhang.ui.viewmodels.RecurringTransactionViewModel
import com.ccjizhang.util.DateUtils.toLocalDate
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionDetailScreen(
    navController: NavHostController,
    transactionId: Long,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onNavigateToEditTransaction: (Long) -> Unit = { id -> navController.navigate("recurring_transaction_edit/$id") },
    onDeleteSuccess: () -> Unit = { navController.navigateUp() },
    viewModel: RecurringTransactionViewModel = hiltViewModel()
) {
    val isLoading = false // Placeholder - Assuming false for now
    val transaction by viewModel.selectedTransactionDetails.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.selectTransactionDetails(transactionId)
    }

    LaunchedEffect(key1 = transaction, key2 = isLoading) {
        if (transaction == null && !isLoading) {
            onNavigateBack()
        }
    }

    UnifiedScaffold(
        title = stringResource(R.string.recurring_transaction_details),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = { onNavigateToEditTransaction(transactionId) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            transaction == null -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.transaction_not_found),
                    message = stringResource(R.string.transaction_not_found_message)
                )
            }
            else -> {
                val currencyFormatter = remember { NumberFormat.getCurrencyInstance() }
                val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with amount
                    PrimaryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = transaction!!.description,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currencyFormatter.format(transaction!!.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (transaction!!.type == 0)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = viewModel.getRecurrenceTypeDescription(transaction!!),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Transaction details
                    SecondaryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            InfoRow(
                                label = stringResource(R.string.account),
                                value = "账户 ID: ${transaction!!.fromAccountId}" +
                                        if (transaction!!.toAccountId != null) " -> ${transaction!!.toAccountId}" else ""
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.category),
                                value = transaction!!.categoryId?.toString() ?: "无"
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.next_occurrence),
                                value = viewModel.getNextExecutionDateDescription(transaction!!)
                            )

                            if (transaction!!.note?.isNotBlank() == true) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text(
                                    text = stringResource(R.string.notes),
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = transaction!!.note ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_recurring_transaction),
            message = stringResource(
                R.string.delete_recurring_transaction_confirmation,
                transaction?.description ?: ""
            ),
            onConfirm = {
                transaction?.let { viewModel.deleteRecurringTransaction(it) }
                showDeleteDialog = false
                onDeleteSuccess()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}