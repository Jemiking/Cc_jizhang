package com.ccjizhang.ui.screens.investment

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
import com.ccjizhang.R
import com.ccjizhang.data.model.Investment
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.components.InfoRow
import com.ccjizhang.ui.viewmodels.InvestmentViewModel
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(
    navController: NavHostController,
    investmentId: Long,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onNavigateToEditInvestment: (Long) -> Unit = { id -> navController.navigate(NavRoutes.investmentEdit(id)) },
    onDeleteSuccess: () -> Unit = { navController.navigateUp() },
    viewModel: InvestmentViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val investment by viewModel.selectedInvestmentDetails.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Trigger data loading when investmentId changes
    LaunchedEffect(investmentId) {
        viewModel.selectInvestmentDetails(investmentId)
    }

    // Handle case where investment is not found after loading
    LaunchedEffect(key1 = investment, key2 = isLoading) { // Observe both
        // Only navigate back if not loading AND investment is null after trying to load
        if (investment == null && !isLoading) {
            onNavigateBack()
        }
    }

    UnifiedScaffold(
        title = stringResource(R.string.investment_details),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = { onNavigateToEditInvestment(investmentId) }) {
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
            investment == null -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.investment_not_found),
                    message = stringResource(R.string.investment_not_found_message)
                )
            }
            else -> {
                val currencyFormatter = remember { NumberFormat.getCurrencyInstance() }
                val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                val percentFormatter = remember { NumberFormat.getPercentInstance() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 头部显示名称和当前价值
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
                                text = investment!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = currencyFormatter.format(investment!!.currentValue),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val gainLoss = investment!!.currentValue - investment!!.initialAmount
                            val gainLossPercent = if (investment!!.initialAmount > 0) {
                                gainLoss / investment!!.initialAmount
                            } else 0.0

                            val gainLossText = if (gainLoss >= 0) {
                                "${stringResource(R.string.gain)}: ${currencyFormatter.format(gainLoss)} (${String.format("%.2f%%", gainLossPercent * 100)})"
                            } else {
                                "${stringResource(R.string.loss)}: ${currencyFormatter.format(-gainLoss)} (${String.format("%.2f%%", -gainLossPercent * 100)})"
                            }

                            Text(
                                text = gainLossText,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (gainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // 基本信息
                    SecondaryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.basic_info),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoRow(
                                label = stringResource(R.string.investment_type),
                                value = stringResource(
                                    when (investment!!.type) {
                                        Investment.Type.STOCK -> R.string.stock
                                        Investment.Type.FUND -> R.string.fund
                                        Investment.Type.BOND -> R.string.bond
                                        Investment.Type.DEPOSIT -> R.string.deposit
                                        Investment.Type.OTHER -> R.string.other_investment
                                    }
                                )
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.initial_amount),
                                value = currencyFormatter.format(investment!!.initialAmount)
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.purchase_date),
                                value = dateFormatter.format(investment!!.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                            )

                            if (investment!!.endDate != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                InfoRow(
                                    label = stringResource(R.string.maturity_date),
                                    value = dateFormatter.format(investment!!.endDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.return_rate),
                                value = if (investment!!.expectedAnnualReturn != null) {
                                    String.format("%.2f%%", investment!!.expectedAnnualReturn)
                                } else {
                                    "未设置"
                                }
                            )

                            if (investment!!.riskLevel != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                InfoRow(
                                    label = stringResource(R.string.risk_level),
                                    value = stringResource(
                                        when (investment!!.riskLevel) {
                                            0 -> R.string.risk_low
                                            1 -> R.string.risk_medium
                                            2 -> R.string.risk_high
                                            else -> R.string.risk_unknown
                                        }
                                    )
                                )
                            }

                            if (investment!!.accountId != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                InfoRow(
                                    label = stringResource(R.string.account),
                                    value = investment!!.accountId?.let { viewModel.getAccountName(it) } ?: "未知账户"
                                )
                            }
                        }
                    }

                    // 额外信息和备注
                    if (investment!!.note != null && investment!!.note!!.isNotEmpty()) {
                        SecondaryCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.notes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = investment!!.note!!,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // 预计收益信息
                    SecondaryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.projected_returns),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val projectedReturns = viewModel.calculateProjectedReturns(investment!!)

                            InfoRow(
                                label = stringResource(R.string.one_year_projection),
                                value = currencyFormatter.format(projectedReturns.oneYear)
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.three_year_projection),
                                value = currencyFormatter.format(projectedReturns.threeYears)
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.five_year_projection),
                                value = currencyFormatter.format(projectedReturns.fiveYears)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_investment),
            message = stringResource(
                R.string.delete_investment_confirmation,
                investment?.name ?: ""
            ),
            onConfirm = {
                investment?.let { viewModel.deleteInvestment(it) }
                showDeleteDialog = false
                onDeleteSuccess()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}