package com.ccjizhang.ui.screens.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ccjizhang.R
import com.ccjizhang.data.model.FinancialReport
import com.ccjizhang.data.model.Period
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.viewmodels.FinancialReportViewModel
import com.ccjizhang.util.DateUtils.toLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.navigation.NavHostController
import com.ccjizhang.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportScreen(
    navController: NavHostController,
    onNavigateToGenerateReport: () -> Unit = { navController.navigate(NavRoutes.GenerateReport) },
    onNavigateToReportDetail: (Long) -> Unit = { id -> navController.navigate(NavRoutes.financialReportDetail(id)) },
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    viewModel: FinancialReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<FinancialReport?>(null) }

    UnifiedScaffold(
        title = stringResource(R.string.financial_reports),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = true,
        floatingActionButtonContent = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.generate_report),
                tint = androidx.compose.ui.graphics.Color.White
            )
        },
        onFloatingActionButtonClick = onNavigateToGenerateReport
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            uiState.reports.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.no_reports),
                    message = stringResource(R.string.no_reports_message),
                    onActionClick = onNavigateToGenerateReport,
                    actionText = stringResource(R.string.generate_report)
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
                    items(uiState.reports) { report ->
                        FinancialReportItem(
                            report = report,
                            onItemClick = { onNavigateToReportDetail(report.id) },
                            onDeleteClick = {
                                selectedReport = report
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedReport != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_report),
            message = stringResource(
                R.string.delete_report_confirmation,
                selectedReport?.title ?: ""
            ),
            onConfirm = {
                viewModel.deleteReport(selectedReport!!)
                showDeleteDialog = false
                selectedReport = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedReport = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportItem(
    report: FinancialReport,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    var showMenu by remember { mutableStateOf(false) }

    SecondaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.title,
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
                            text = { Text(stringResource(R.string.share)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                // TODO: 实现分享功能
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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.generated_on)}: ${dateFormatter.format(report.generatedDate.toLocalDate())}",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = getReportPeriodText(report),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = report.note ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun getReportPeriodText(report: FinancialReport): String {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return when (report.type) {
        0 -> stringResource(R.string.monthly_report)   // Period.MONTHLY
        1 -> stringResource(R.string.quarterly_report) // Period.QUARTERLY
        2 -> stringResource(R.string.yearly_report)    // Period.YEARLY
        3 -> {                                         // Period.CUSTOM
            val start = report.startDate?.toLocalDate()
            val end = report.endDate?.toLocalDate()
            if (start != null && end != null) {
                "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
            } else {
                stringResource(R.string.custom_period)
            }
        }
        else -> stringResource(R.string.custom_period)
    }
}