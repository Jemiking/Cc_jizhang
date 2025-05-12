package com.ccjizhang.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import com.ccjizhang.data.model.FinancialReport
import com.ccjizhang.ui.common.EmptyContent
import com.ccjizhang.ui.common.LoadingContent
import com.ccjizhang.ui.components.CCJiZhangTopAppBar
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.components.ConfirmDeleteDialog
import com.ccjizhang.ui.components.InfoRow
import com.ccjizhang.ui.viewmodels.FinancialReportViewModel
import com.ccjizhang.util.CollectionUtils.forEachTyped
import com.ccjizhang.util.DateUtils.toLocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportDetailScreen(
    navController: NavHostController,
    reportId: Long,
    onNavigateBack: () -> Unit,
    onDeleteSuccess: () -> Unit = {},
    viewModel: FinancialReportViewModel = hiltViewModel()
) {
    val report by viewModel.getReportByIdFlow(reportId).collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = report) {
        if (report == null && !uiState.isLoading) {
            // Report not found
            onNavigateBack()
        }
    }

    UnifiedScaffold(
        title = stringResource(R.string.financial_report),
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = false,
        actions = {
            IconButton(onClick = { /* TODO: 实现分享功能 */ }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
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
            uiState.isLoading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            report == null -> {
                EmptyContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    title = stringResource(R.string.report_not_found),
                    message = stringResource(R.string.report_not_found_message)
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
                    // 报告头部
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
                                text = report!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.report_period),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = GetFormattedReportPeriodText(report = report!!),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${stringResource(R.string.generated_on)}: ${dateFormatter.format(report!!.generatedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (!report!!.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = report!!.note!!,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // 财务概览
                    SecondaryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.financial_overview),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            InfoRow(
                                label = stringResource(R.string.total_income),
                                value = currencyFormatter.format(report!!.totalIncome.toDouble()),
                                valueColor = MaterialTheme.colorScheme.primary
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            InfoRow(
                                label = stringResource(R.string.total_expense),
                                value = currencyFormatter.format(report!!.totalExpense.toDouble()),
                                valueColor = MaterialTheme.colorScheme.error
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            val netIncome = report!!.totalIncome - report!!.totalExpense
                            InfoRow(
                                label = stringResource(R.string.net_income),
                                value = currencyFormatter.format(netIncome.toDouble()),
                                valueColor = if (netIncome >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )

                            if (report!!.savingsRate != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                InfoRow(
                                    label = stringResource(R.string.savings_rate),
                                    value = percentFormatter.format(report!!.savingsRate),
                                    valueColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 收入分析 (接收 JSON String)
                    AnalysisSectionJson(
                        title = stringResource(R.string.income_analysis),
                        json = report!!.incomeAnalysisJson
                    )

                    // 支出分析 (接收 JSON String)
                    AnalysisSectionJson(
                        title = stringResource(R.string.expense_analysis),
                        json = report!!.expenseAnalysisJson
                    )

                    // 账户余额 (接收 JSON String)
                    AccountBalancesSectionJson(json = report!!.accountBalancesJson)

                    // 预算对比 (仍然注释掉)
                    /*
                    if (report!!.budgetComparison != null && report!!.budgetComparison!!.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.budget_comparison),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 需要解析 report.budgetComparisonJson 并定义 BudgetComparisonData
                                // report!!.budgetComparison!!.forEachTyped<String, FinancialReport.BudgetComparisonData> { budget, comparison -> ... }
                            }
                        }
                    }
                    */

                    // 财务健康状况 (接收 JSON String)
                    FinancialHealthSectionJson(json = report!!.financialHealthJson)
                }
            }
        }
    }

    if (showDeleteDialog && report != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_report),
            message = stringResource(R.string.delete_report_confirmation, report!!.title),
            onConfirm = {
                viewModel.deleteReport(report!!)
                showDeleteDialog = false
                onDeleteSuccess()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// 修改分析部分 Composable 以接收 JSON (临时方案)
@Composable
private fun AnalysisSectionJson(title: String, json: String?) {
    if (!json.isNullOrBlank() && json != "{}") {
        SecondaryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                // 暂时只显示 JSON 内容或提示
                Text("数据: (JSON 需解析)", style = MaterialTheme.typography.bodySmall)
                // Text(json, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// 修改账户余额 Composable 以接收 JSON
@Composable
private fun AccountBalancesSectionJson(json: String?) {
    if (!json.isNullOrBlank() && json != "{}") {
        Card(
             modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
             Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.account_balances), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("数据: (JSON 需解析)", style = MaterialTheme.typography.bodySmall)
             }
        }
    }
}

// 修改财务健康状况 Composable 以接收 JSON
@Composable
private fun FinancialHealthSectionJson(json: String?) {
     if (!json.isNullOrBlank() && json != "{}") {
        SecondaryCard(
             modifier = Modifier.fillMaxWidth()
        ) {
             Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.financial_health), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("数据: (JSON 需解析)", style = MaterialTheme.typography.bodySmall)
             }
         }
    }
}

// 获取报告期间文本 (移除内部 stringResource 调用)
// 需要在调用处获取字符串资源
private fun getReportPeriodTextInternal(
    report: FinancialReport,
    monthlyText: String,
    quarterlyText: String,
    yearlyText: String,
    customPeriodText: String,
    unknownPeriodText: String
): String {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return when (report.type) {
        0 -> { // 月度
            val date = report.startDate?.toLocalDate()
            date?.format(DateTimeFormatter.ofPattern("yyyy年MM月")) ?: monthlyText
        }
        1 -> { // 季度
            val date = report.startDate?.toLocalDate()
            if (date != null) {
                 "${date.year}年第${(date.monthValue - 1) / 3 + 1}季度"
            } else {
                 quarterlyText
            }
        }
        2 -> { // 年度
             val date = report.startDate?.toLocalDate()
             date?.year?.toString()?.let { "${it}年" } ?: yearlyText
        }
        3 -> { // 自定义
            val start = report.startDate?.toLocalDate()
            val end = report.endDate?.toLocalDate()
            if (start != null && end != null) {
                "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
            } else {
                customPeriodText
            }
        }
        // 使用 R.string.unknown_period 对应的字符串
        else -> unknownPeriodText
    }
}

// 在 Composable 函数内部调用 getReportPeriodTextInternal
@Composable
private fun GetFormattedReportPeriodText(report: FinancialReport): String {
    return getReportPeriodTextInternal(
        report = report,
        monthlyText = stringResource(R.string.monthly_report),
        quarterlyText = stringResource(R.string.quarterly_report),
        yearlyText = stringResource(R.string.yearly_report),
        customPeriodText = stringResource(R.string.custom_period),
        // 确保 R.string.unknown_period 存在，如果不存在，使用 R.string.custom_period 或其他
        unknownPeriodText = stringResource(id = R.string.custom_period) // 修正 unknown_period 引用
    )
}