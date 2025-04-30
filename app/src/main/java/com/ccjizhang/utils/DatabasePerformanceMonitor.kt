package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import com.ccjizhang.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库性能监控工具
 * 用于收集和分析数据库性能指标
 */
@Singleton
class DatabasePerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase
) {
    companion object {
        private const val TAG = "DatabasePerformanceMonitor"
        private const val REPORTS_FOLDER = "db_performance_reports"
        private const val MAX_REPORTS = 10
    }

    // 性能指标
    data class PerformanceMetrics(
        val timestamp: Long = System.currentTimeMillis(),
        val databaseSize: Long = 0,
        val walFileSize: Long = 0,
        val shmFileSize: Long = 0,
        val queryExecutionTime: Long = 0,
        val transactionExecutionTime: Long = 0,
        val journalMode: String = "",
        val syncMode: String = "",
        val integrityCheckResult: String = "",
        val tableCount: Int = 0,
        val indexCount: Int = 0,
        val totalRowCount: Int = 0
    )

    /**
     * 收集数据库性能指标
     */
    suspend fun collectPerformanceMetrics(): PerformanceMetrics = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "开始收集数据库性能指标")

        try {
            // 获取数据库文件大小
            val dbFile = context.getDatabasePath("ccjizhang_database_plain")
            val dbSize = if (dbFile.exists()) dbFile.length() else 0

            // 获取WAL和SHM文件大小
            val walFile = File("${dbFile.path}-wal")
            val shmFile = File("${dbFile.path}-shm")
            val walSize = if (walFile.exists()) walFile.length() else 0
            val shmSize = if (shmFile.exists()) shmFile.length() else 0

            // 测量查询执行时间
            val queryStartTime = System.currentTimeMillis()
            val cursor = appDatabase.query("SELECT count(*) FROM sqlite_master", null)
            cursor.use {
                if (cursor.moveToFirst()) {
                    // 只是为了确保查询执行
                }
            }
            val queryTime = System.currentTimeMillis() - queryStartTime

            // 测量事务执行时间
            val transactionStartTime = System.currentTimeMillis()
            appDatabase.runInTransaction {
                // 执行一个空事务，只测量事务开销
            }
            val transactionTime = System.currentTimeMillis() - transactionStartTime

            // 获取日志模式
            var journalMode = "unknown"
            var syncMode = "unknown"
            var tableCount = 0
            var indexCount = 0
            var totalRowCount = 0

            appDatabase.query("PRAGMA journal_mode", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    journalMode = cursor.getString(0)
                }
            }

            appDatabase.query("PRAGMA synchronous", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    syncMode = cursor.getString(0)
                }
            }

            // 获取表和索引数量
            appDatabase.query("SELECT type, count(*) FROM sqlite_master GROUP BY type", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val type = cursor.getString(0)
                    val count = cursor.getInt(1)
                    when (type) {
                        "table" -> tableCount = count
                        "index" -> indexCount = count
                    }
                }
            }

            // 执行完整性检查
            var integrityResult = "unknown"
            try {
                appDatabase.query("PRAGMA integrity_check", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        integrityResult = cursor.getString(0)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "完整性检查失败", e)
                integrityResult = "failed: ${e.message}"
            }

            // 估算总行数（这可能会很慢，所以只是估算主要表的行数）
            try {
                appDatabase.query("SELECT count(*) FROM transactions", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        totalRowCount += cursor.getInt(0)
                    }
                }
                appDatabase.query("SELECT count(*) FROM categories", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        totalRowCount += cursor.getInt(0)
                    }
                }
                appDatabase.query("SELECT count(*) FROM accounts", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        totalRowCount += cursor.getInt(0)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取行数失败", e)
            }

            val metrics = PerformanceMetrics(
                databaseSize = dbSize,
                walFileSize = walSize,
                shmFileSize = shmSize,
                queryExecutionTime = queryTime,
                transactionExecutionTime = transactionTime,
                journalMode = journalMode,
                syncMode = syncMode,
                integrityCheckResult = integrityResult,
                tableCount = tableCount,
                indexCount = indexCount,
                totalRowCount = totalRowCount
            )

            Log.i(TAG, "数据库性能指标收集完成，耗时: ${System.currentTimeMillis() - startTime}ms")
            Log.i(TAG, "数据库大小: ${dbSize/1024}KB, WAL文件: ${walSize/1024}KB, SHM文件: ${shmSize/1024}KB")
            Log.i(TAG, "查询时间: ${queryTime}ms, 事务时间: ${transactionTime}ms")
            Log.i(TAG, "日志模式: $journalMode, 同步模式: $syncMode")
            Log.i(TAG, "完整性检查: $integrityResult")
            Log.i(TAG, "表数量: $tableCount, 索引数量: $indexCount, 总行数: $totalRowCount")

            return@withContext metrics
        } catch (e: Exception) {
            Log.e(TAG, "收集性能指标失败", e)
            return@withContext PerformanceMetrics()
        }
    }

    /**
     * 生成性能报告
     */
    suspend fun generatePerformanceReport(): File? = withContext(Dispatchers.IO) {
        try {
            val metrics = collectPerformanceMetrics()

            // 创建报告目录
            val reportsDir = File(context.filesDir, REPORTS_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            // 创建报告文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val reportFile = File(
                reportsDir,
                "db_performance_${dateFormat.format(Date(metrics.timestamp))}.txt"
            )

            // 写入报告内容
            reportFile.writeText(
                """
                CC记账数据库性能报告
                ===========================
                生成时间: ${dateFormat.format(Date(metrics.timestamp))}

                数据库文件信息:
                - 数据库大小: ${metrics.databaseSize / 1024} KB
                - WAL文件大小: ${metrics.walFileSize / 1024} KB
                - SHM文件大小: ${metrics.shmFileSize / 1024} KB
                - 总大小: ${(metrics.databaseSize + metrics.walFileSize + metrics.shmFileSize) / 1024} KB

                数据库配置:
                - 日志模式: ${metrics.journalMode}
                - 同步模式: ${metrics.syncMode}

                性能指标:
                - 查询执行时间: ${metrics.queryExecutionTime} ms
                - 事务执行时间: ${metrics.transactionExecutionTime} ms

                数据库结构:
                - 表数量: ${metrics.tableCount}
                - 索引数量: ${metrics.indexCount}
                - 总行数(估计): ${metrics.totalRowCount}

                完整性检查:
                - 结果: ${metrics.integrityCheckResult}

                性能评估:
                ${evaluatePerformance(metrics)}

                优化建议:
                ${generateOptimizationSuggestions(metrics)}
                """.trimIndent()
            )

            // 清理旧报告
            cleanupOldReports(reportsDir)

            Log.i(TAG, "性能报告已生成: ${reportFile.path}")
            return@withContext reportFile
        } catch (e: Exception) {
            Log.e(TAG, "生成性能报告失败", e)
            return@withContext null
        }
    }

    /**
     * 评估性能
     */
    private fun evaluatePerformance(metrics: PerformanceMetrics): String {
        val evaluation = StringBuilder()

        // 评估数据库大小
        when {
            metrics.databaseSize > 10 * 1024 * 1024 -> // 10MB
                evaluation.append("- 数据库大小较大，可能需要考虑清理或归档旧数据\n")
            metrics.databaseSize > 50 * 1024 * 1024 -> // 50MB
                evaluation.append("- 数据库大小过大，强烈建议清理或归档旧数据\n")
            else ->
                evaluation.append("- 数据库大小正常\n")
        }

        // 评估WAL文件大小
        when {
            metrics.walFileSize > 5 * 1024 * 1024 -> // 5MB
                evaluation.append("- WAL文件较大，建议执行检查点操作\n")
            metrics.walFileSize > 20 * 1024 * 1024 -> // 20MB
                evaluation.append("- WAL文件过大，强烈建议执行检查点操作\n")
            else ->
                evaluation.append("- WAL文件大小正常\n")
        }

        // 评估查询性能
        when {
            metrics.queryExecutionTime > 100 -> // 100ms
                evaluation.append("- 查询执行时间较长，可能需要优化索引\n")
            metrics.queryExecutionTime > 500 -> // 500ms
                evaluation.append("- 查询执行时间过长，强烈建议优化索引和查询\n")
            else ->
                evaluation.append("- 查询执行时间正常\n")
        }

        // 评估事务性能
        when {
            metrics.transactionExecutionTime > 50 -> // 50ms
                evaluation.append("- 事务执行时间较长，可能需要优化事务管理\n")
            metrics.transactionExecutionTime > 200 -> // 200ms
                evaluation.append("- 事务执行时间过长，强烈建议优化事务管理\n")
            else ->
                evaluation.append("- 事务执行时间正常\n")
        }

        // 评估完整性
        if (metrics.integrityCheckResult != "ok") {
            evaluation.append("- 数据库完整性检查失败，需要立即修复\n")
        } else {
            evaluation.append("- 数据库完整性正常\n")
        }

        return evaluation.toString()
    }

    /**
     * 生成优化建议
     */
    private fun generateOptimizationSuggestions(metrics: PerformanceMetrics): String {
        val suggestions = StringBuilder()

        // 日志模式建议
        if (metrics.journalMode.equals("delete", ignoreCase = true)) {
            suggestions.append("- 考虑启用WAL模式以提高性能和可靠性\n")
        }

        // 同步模式建议
        if (metrics.syncMode.equals("full", ignoreCase = true)) {
            suggestions.append("- 考虑将同步模式设置为NORMAL，以平衡性能和安全性\n")
        }

        // 数据库大小建议
        if (metrics.databaseSize > 10 * 1024 * 1024) { // 10MB
            suggestions.append("- 考虑实施数据归档策略，将旧数据移至归档数据库\n")
            suggestions.append("- 定期执行VACUUM操作，回收未使用的空间\n")
        }

        // WAL文件建议
        if (metrics.walFileSize > 5 * 1024 * 1024) { // 5MB
            suggestions.append("- 定期执行检查点操作，减少WAL文件大小\n")
            suggestions.append("- 考虑设置WAL自动检查点阈值\n")
        }

        // 索引建议
        if (metrics.queryExecutionTime > 100) { // 100ms
            suggestions.append("- 检查并优化数据库索引\n")
            suggestions.append("- 分析慢查询，考虑添加适当的索引\n")
        }

        // 事务建议
        if (metrics.transactionExecutionTime > 50) { // 50ms
            suggestions.append("- 优化事务管理，避免长时间运行的事务\n")
            suggestions.append("- 考虑将大型事务分解为多个小事务\n")
        }

        // 如果没有特别的建议
        if (suggestions.isEmpty()) {
            suggestions.append("- 当前数据库性能良好，无需特别优化\n")
            suggestions.append("- 继续定期监控数据库性能\n")
        }

        return suggestions.toString()
    }

    /**
     * 清理旧报告
     */
    private fun cleanupOldReports(reportsDir: File) {
        val reports = reportsDir.listFiles()?.filter { it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (reports.size > MAX_REPORTS) {
            reports.drop(MAX_REPORTS).forEach { it.delete() }
            Log.i(TAG, "已清理 ${reports.size - MAX_REPORTS} 个旧报告")
        }
    }
}
