package com.ccjizhang.ui.navigation

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导航分析报告生成器
 */
object NavAnalyticsReporter {
    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState

    /**
     * 生成导航分析报告
     *
     * @param context 上下文
     * @param fileName 文件名，默认为navigation_report_yyyy-MM-dd_HH-mm-ss.txt
     * @return 报告文件路径
     */
    suspend fun generateReport(context: Context, fileName: String? = null): String {
        _reportState.value = ReportState.Generating

        try {
            // 获取导航分析数据
            val screenTimeReport = NavAnalytics.getScreenTimeReport()
            val navigationPerformance = NavPerformance.navigationPerformance.value

            // 创建报告内容
            val reportContent = buildString {
                appendLine("# 导航分析报告")
                appendLine("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine()

                appendLine("## 屏幕停留时间")
                if (screenTimeReport.isEmpty()) {
                    appendLine("暂无数据")
                } else {
                    // 按停留时间降序排序
                    val sortedScreenTime = screenTimeReport.entries.sortedByDescending { it.value }
                    sortedScreenTime.forEach { (screen, time) ->
                        val seconds = time / 1000.0
                        appendLine("- $screen: ${String.format("%.2f", seconds)}秒")
                    }
                }
                appendLine()

                appendLine("## 导航性能")
                if (navigationPerformance.isEmpty()) {
                    appendLine("暂无数据")
                } else {
                    // 按导航时间降序排序
                    val sortedPerformance = navigationPerformance.entries.sortedByDescending { it.value }
                    sortedPerformance.forEach { (route, time) ->
                        appendLine("- $route: ${time}毫秒")
                    }
                }
                appendLine()

                appendLine("## 导航路径分析")
                // TODO: 添加导航路径分析
                appendLine("暂无数据")
            }

            // 生成文件名
            val reportFileName = fileName ?: "navigation_report_${
                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            }.txt"

            // 保存报告
            val file = saveReportToFile(context, reportFileName, reportContent)

            _reportState.value = ReportState.Success(file.absolutePath)
            return file.absolutePath
        } catch (e: Exception) {
            _reportState.value = ReportState.Error(e.message ?: "未知错误")
            throw e
        }
    }

    /**
     * 保存报告到文件
     *
     * @param context 上下文
     * @param fileName 文件名
     * @param content 报告内容
     * @return 报告文件
     */
    private fun saveReportToFile(context: Context, fileName: String, content: String): File {
        val reportsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val file = File(reportsDir, fileName)
        file.writeText(content)
        return file
    }

    /**
     * 重置报告状态
     */
    fun resetReportState() {
        _reportState.value = ReportState.Idle
    }

    /**
     * 报告状态
     */
    sealed class ReportState {
        object Idle : ReportState()
        object Generating : ReportState()
        data class Success(val filePath: String) : ReportState()
        data class Error(val message: String) : ReportState()
    }
}

/**
 * 导航分析报告生成器Composable
 */
@Composable
fun NavAnalyticsReporterScreen(
    context: Context,
    onReportGenerated: (String) -> Unit,
    onError: (String) -> Unit
) {
    val reportState by NavAnalyticsReporter.reportState.collectAsState()

    LaunchedEffect(reportState) {
        when (val state = reportState) {
            is NavAnalyticsReporter.ReportState.Success -> {
                onReportGenerated(state.filePath)
            }
            is NavAnalyticsReporter.ReportState.Error -> {
                onError(state.message)
            }
            else -> {
                // 不处理其他状态
            }
        }
    }
}
