package com.ccjizhang.ui.screens.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ccjizhang.R
import com.ccjizhang.data.model.FinancialReport
import com.ccjizhang.data.model.Period
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.common.DatePickerDialog
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.viewmodels.FinancialReportViewModel
import com.ccjizhang.util.DateUtils.toDate
import com.ccjizhang.util.DateUtils.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.navigation.NavHostController
import java.util.*
import com.ccjizhang.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateReportScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit = { navController.navigateUp() },
    onReportGenerated: (Long) -> Unit = { id -> navController.navigate(NavRoutes.financialReportDetail(id)) },
    viewModel: FinancialReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(Period.MONTHLY) }
    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var includeIncomeAnalysis by remember { mutableStateOf(true) }
    var includeExpenseAnalysis by remember { mutableStateOf(true) }
    var includeCategoryBreakdown by remember { mutableStateOf(true) }
    var includeAccountBalances by remember { mutableStateOf(true) }
    var includeBudgetComparison by remember { mutableStateOf(true) }
    var includeFinancialHealth by remember { mutableStateOf(true) }

    // UI state
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    // Error states
    var titleError by remember { mutableStateOf(false) }
    var dateRangeError by remember { mutableStateOf(false) }

    // 开始日期选择器状态
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // 结束日期选择器状态
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // Adjust dates based on period selection
    LaunchedEffect(period) {
        when (period) {
            Period.MONTHLY -> {
                startDate = LocalDate.now().withDayOfMonth(1)
                endDate = LocalDate.now()
            }
            Period.QUARTERLY -> {
                val month = LocalDate.now().monthValue
                val quarter = (month - 1) / 3
                val quarterStartMonth = quarter * 3 + 1
                startDate = LocalDate.now().withMonth(quarterStartMonth).withDayOfMonth(1)
                endDate = LocalDate.now()
            }
            Period.YEARLY -> {
                startDate = LocalDate.now().withDayOfYear(1)
                endDate = LocalDate.now()
            }
            Period.CUSTOM -> {
                // Keep current dates
            }
        }
    }

    UnifiedScaffold(
        title = stringResource(R.string.generate_report),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = true,
        floatingActionButtonContent = {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = stringResource(R.string.generate),
                tint = androidx.compose.ui.graphics.Color.White
            )
        },
        onFloatingActionButtonClick = {
                    // Validate form
                    titleError = title.isBlank()
                    dateRangeError = startDate.isAfter(endDate)

                    if (!titleError && !dateRangeError) {
                        isGenerating = true

                        viewModel.generateReport(
                            title = title,
                            description = description,
                            period = period,
                            startDate = startDate,
                            endDate = endDate,
                            includeIncomeAnalysis = includeIncomeAnalysis,
                            includeExpenseAnalysis = includeExpenseAnalysis,
                            includeCategoryBreakdown = includeCategoryBreakdown,
                            includeAccountBalances = includeAccountBalances,
                            includeBudgetComparison = includeBudgetComparison,
                            includeFinancialHealth = includeFinancialHealth,
                            onSuccess = { reportId ->
                                isGenerating = false
                                onReportGenerated(reportId)
                            },
                            onError = {
                                isGenerating = false
                                // Error handling will be done by the ViewModel
                            }
                        )
                    }
                }
    ) { innerPadding ->
        if (uiState.isLoading || isGenerating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.generating_report))
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 报告标题
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text(stringResource(R.string.report_title)) },
                    isError = titleError,
                    supportingText = {
                        if (titleError) {
                            Text(stringResource(R.string.title_required))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                // 报告描述
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.report_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    minLines = 2
                )

                // 报告周期
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stringResource(
                            when (period) {
                                Period.MONTHLY -> R.string.monthly
                                Period.QUARTERLY -> R.string.quarterly_report
                                Period.YEARLY -> R.string.yearly
                                Period.CUSTOM -> R.string.custom_period
                            }
                        ),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.report_period)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPeriodDropdown = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Period.values().forEach { reportPeriod ->
                            val resourceId = when (reportPeriod) {
                                Period.MONTHLY -> R.string.monthly
                                Period.QUARTERLY -> R.string.quarterly_report
                                Period.YEARLY -> R.string.yearly
                                Period.CUSTOM -> R.string.custom_period
                            }

                            DropdownMenuItem(
                                onClick = {
                                    period = reportPeriod
                                    showPeriodDropdown = false
                                },
                                text = { Text(stringResource(resourceId)) }
                            )
                        }
                    }
                }

                // 日期范围
                SecondaryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.date_range),
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (dateRangeError) {
                            Text(
                                text = stringResource(R.string.date_range_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

                        // 开始日期
                        OutlinedTextField(
                            value = startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.start_date)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStartDatePicker = true },
                            readOnly = true,
                            enabled = period == Period.CUSTOM,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null
                                )
                            }
                        )

                        // 结束日期
                        OutlinedTextField(
                            value = endDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            onValueChange = {},
                            label = { Text(stringResource(R.string.end_date)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = period == Period.CUSTOM) { showEndDatePicker = true },
                            readOnly = true,
                            enabled = period == Period.CUSTOM,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                // 报告内容
                SecondaryCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.report_content),
                            style = MaterialTheme.typography.titleMedium
                        )

                        // 包含收入分析
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_income_analysis))
                            Switch(
                                checked = includeIncomeAnalysis,
                                onCheckedChange = { includeIncomeAnalysis = it }
                            )
                        }

                        // 包含支出分析
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_expense_analysis))
                            Switch(
                                checked = includeExpenseAnalysis,
                                onCheckedChange = { includeExpenseAnalysis = it }
                            )
                        }

                        // 包含分类明细
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_category_breakdown))
                            Switch(
                                checked = includeCategoryBreakdown,
                                onCheckedChange = { includeCategoryBreakdown = it }
                            )
                        }

                        // 包含账户余额
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_account_balances))
                            Switch(
                                checked = includeAccountBalances,
                                onCheckedChange = { includeAccountBalances = it }
                            )
                        }

                        // 包含预算对比
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_budget_comparison))
                            Switch(
                                checked = includeBudgetComparison,
                                onCheckedChange = { includeBudgetComparison = it }
                            )
                        }

                        // 包含财务健康状况
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.include_financial_health))
                            Switch(
                                checked = includeFinancialHealth,
                                onCheckedChange = { includeFinancialHealth = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // 开始日期选择器
    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = Instant.ofEpochMilli(startDatePickerState.selectedDateMillis ?: System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toDate(),
            onDateSelected = { date ->
                startDate = date.toLocalDate()
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // 结束日期选择器
    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = Instant.ofEpochMilli(endDatePickerState.selectedDateMillis ?: System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toDate(),
            onDateSelected = { date ->
                endDate = date.toLocalDate()
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}