package com.ccjizhang.ui.screens.investment

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
import com.ccjizhang.R
import com.ccjizhang.data.model.Investment
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.viewmodels.InvestmentViewModel
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    navController: NavHostController,
    onNavigateToAddInvestment: () -> Unit = { navController.navigate(NavRoutes.InvestmentAdd) },
    onNavigateToEditInvestment: (Long) -> Unit = { id -> navController.navigate(NavRoutes.investmentEdit(id)) },
    onNavigateToInvestmentDetail: (Long) -> Unit = { id -> navController.navigate(NavRoutes.investmentDetail(id)) },
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    viewModel: InvestmentViewModel = hiltViewModel()
) {
    // Collect individual state flows from the ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val investments by viewModel.filteredInvestments.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedInvestment by remember { mutableStateOf<Investment?>(null) }
    
    Scaffold(
        topBar = {
            CCJiZhangTopAppBar(
                title = stringResource(R.string.investments),
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddInvestment,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_investment)
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
            investments.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.no_investments),
                    message = stringResource(R.string.no_investments_message),
                    onActionClick = onNavigateToAddInvestment,
                    actionText = stringResource(R.string.add_investment)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = investments, key = { it.id }) { investment: Investment ->
                        InvestmentItem(
                            investment = investment,
                            onItemClick = { onNavigateToInvestmentDetail(investment.id) },
                            onEditClick = { onNavigateToEditInvestment(investment.id) },
                            onDeleteClick = {
                                selectedInvestment = investment
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog && selectedInvestment != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_investment),
            message = stringResource(
                R.string.delete_investment_confirmation,
                selectedInvestment?.name ?: ""
            ),
            onConfirm = {
                viewModel.deleteInvestment(selectedInvestment!!)
                showDeleteDialog = false
                selectedInvestment = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedInvestment = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentItem(
    investment: Investment,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance() }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    
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
                    text = investment.name,
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
            
            // 当前价值
            Text(
                text = currencyFormatter.format(investment.currentValue),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 投资类型和收益率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        when (investment.type) {
                            Investment.Type.STOCK -> R.string.stock
                            Investment.Type.FUND -> R.string.fund
                            Investment.Type.BOND -> R.string.bond
                            Investment.Type.DEPOSIT -> R.string.deposit
                            Investment.Type.OTHER -> R.string.other_investment
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "${stringResource(R.string.return_rate)}: ${
                        if (investment.expectedAnnualReturn != null) {
                            "${investment.expectedAnnualReturn}%"
                        } else {
                            "未设置"
                        }
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 购买日期和到期日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.purchase_date)}: ${dateFormatter.format(investment.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (investment.endDate != null) {
                    Text(
                        text = "${stringResource(R.string.maturity_date)}: ${dateFormatter.format(investment.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
} 