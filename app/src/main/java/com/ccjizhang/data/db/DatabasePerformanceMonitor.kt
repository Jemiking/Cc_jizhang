package com.ccjizhang.data.db

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库性能监控器
 * 负责监控数据库性能、收集性能指标并生成性能报告
 */
@Singleton
class DatabasePerformanceMonitor @Inject constructor(
    private val context: Context,
    private val connectionManager: DatabaseConnectionManager,
    private val appDatabase: AppDatabase
) {
    companion object {
        private const val TAG = "DbPerformanceMonitor"
        private const val REPORTS_FOLDER = "db_performance_reports"
        private const val QUERY_TIMEOUT_MS = 5000L // 查询超时时间（5秒）
    }

    // 性能指标
    private val performanceMetrics = mutableMapOf<String, Long>()

    // 查询执行时间记录
    private val queryExecutionTimes = mutableListOf<QueryExecutionTime>()

    /**
     * 收集数据库性能指标
     */
    suspend fun collectPerformanceMetrics(): DatabasePerformanceReport = withContext(Dispatchers.IO) {
        Log.i(TAG, "开始收集数据库性能指标")

        val startTime = System.currentTimeMillis()
        val report = DatabasePerformanceReport()

        try {
            // 获取数据库连接
            connectionManager.withConnection { connection ->
                // 收集数据库文件信息
                collectDatabaseFileInfo(report)

                // 收集数据库配置信息
                collectDatabaseConfig(report)

                // 收集表和索引信息
                collectTableAndIndexInfo(connection, report)

                // 执行性能测试查询
                performTestQueries(connection, report)

                // 收集连接统计信息
                collectConnectionStats(report)
            }

            // 计算总耗时
            val totalTime = System.currentTimeMillis() - startTime
            report.collectionTimeMs = totalTime

            Log.i(TAG, "数据库性能指标收集完成，耗时 $totalTime ms")
        } catch (e: Exception) {
            Log.e(TAG, "收集数据库性能指标失败", e)
            report.error = e.message ?: "未知错误"
        }

        return@withContext report
    }

    /**
     * 收集数据库文件信息
     */
    private fun collectDatabaseFileInfo(report: DatabasePerformanceReport) {
        try {
            // 获取数据库文件
            val dbFile = context.getDatabasePath("ccjizhang_database_plain")

            if (dbFile.exists()) {
                report.databaseFileExists = true
                report.databaseFileSize = dbFile.length()
                report.databaseFilePath = dbFile.absolutePath

                // 检查 WAL 文件
                val walFile = File("${dbFile.path}-wal")
                if (walFile.exists()) {
                    report.walFileExists = true
                    report.walFileSize = walFile.length()
                }

                // 检查 SHM 文件
                val shmFile = File("${dbFile.path}-shm")
                if (shmFile.exists()) {
                    report.shmFileExists = true
                    report.shmFileSize = shmFile.length()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "收集数据库文件信息失败", e)
        }
    }

    /**
     * 收集数据库配置信息
     */
    private fun collectDatabaseConfig(report: DatabasePerformanceReport) {
        try {
            // 获取数据库配置
            val config = connectionManager.getDatabaseConfig()
            report.databaseConfig.putAll(config)

            // 获取连接统计信息
            val stats = connectionManager.getConnectionStats()
            report.activeConnections = stats.activeConnections
            report.maxConnections = stats.maxConnections
        } catch (e: Exception) {
            Log.e(TAG, "收集数据库配置信息失败", e)
        }
    }

    /**
     * 收集表和索引信息
     */
    private fun collectTableAndIndexInfo(connection: SupportSQLiteDatabase, report: DatabasePerformanceReport) {
        try {
            // 获取表信息
            connection.query("SELECT name, type FROM sqlite_master WHERE type IN ('table', 'index') ORDER BY type, name").use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    val type = cursor.getString(1)

                    if (type == "table" && !name.startsWith("sqlite_") && !name.startsWith("android_")) {
                        report.tables.add(name)

                        // 获取表行数
                        connection.query("SELECT COUNT(*) FROM `$name`").use { countCursor ->
                            if (countCursor.moveToFirst()) {
                                val count = countCursor.getLong(0)
                                report.tableRowCounts[name] = count
                            }
                        }
                    } else if (type == "index" && !name.startsWith("sqlite_") && !name.startsWith("android_")) {
                        report.indexes.add(name)
                    }
                }
            }

            // 获取数据库页大小和页数
            connection.query("PRAGMA page_size").use { cursor ->
                if (cursor.moveToFirst()) {
                    report.pageSize = cursor.getInt(0)
                }
            }

            connection.query("PRAGMA page_count").use { cursor ->
                if (cursor.moveToFirst()) {
                    report.pageCount = cursor.getInt(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "收集表和索引信息失败", e)
        }
    }

    /**
     * 执行性能测试查询
     */
    private fun performTestQueries(connection: SupportSQLiteDatabase, report: DatabasePerformanceReport) {
        try {
            // 创建一个局部的查询执行时间列表，避免并发修改问题
            val localQueryExecutionTimes = mutableListOf<QueryExecutionTime>()

            // 首先检查数据库是否可用
            try {
                // 执行简单查询来检查数据库连接
                connection.query("SELECT 1").close()
                Log.i(TAG, "数据库连接正常")
            } catch (e: Exception) {
                Log.e(TAG, "数据库连接异常，跳过性能测试", e)
                report.error = "数据库连接异常: ${e.message}"
                return
            }

            // 检查表是否存在
            val tablesExist = try {
                val cursor = connection.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transactions'")
                val exists = cursor.count > 0
                cursor.close()
                exists
            } catch (e: Exception) {
                Log.e(TAG, "检查表是否存在失败", e)
                false
            }

            if (!tablesExist) {
                Log.w(TAG, "transactions表不存在，跳过相关测试")
                report.error = "transactions表不存在"
                return
            }

            // 安全地执行测试查询，每个查询单独处理异常
            safelyMeasureQueryLocal(connection, "SELECT COUNT(*) FROM transactions", "简单计数查询", localQueryExecutionTimes)
            safelyMeasureQueryLocal(connection, "SELECT * FROM transactions WHERE id = 1 LIMIT 1", "主键索引查询", localQueryExecutionTimes)
            safelyMeasureQueryLocal(connection, "SELECT * FROM transactions WHERE isIncome = 1 LIMIT 5", "条件查询", localQueryExecutionTimes)
            safelyMeasureQueryLocal(connection, "SELECT * FROM transactions ORDER BY date DESC LIMIT 5", "排序查询", localQueryExecutionTimes)
            safelyMeasureQueryLocal(connection, "SELECT categoryId, COUNT(*) FROM transactions GROUP BY categoryId LIMIT 10", "分组查询", localQueryExecutionTimes)

            // 检查categories表是否存在
            val categoriesExist = try {
                val cursor = connection.query("SELECT name FROM sqlite_master WHERE type='table' AND name='categories'")
                val exists = cursor.count > 0
                cursor.close()
                exists
            } catch (e: Exception) {
                Log.e(TAG, "检查categories表是否存在失败", e)
                false
            }

            if (categoriesExist) {
                safelyMeasureQueryLocal(connection, "SELECT t.id, c.name FROM transactions t LEFT JOIN categories c ON t.categoryId = c.id LIMIT 5", "连接查询", localQueryExecutionTimes)
            } else {
                Log.w(TAG, "categories表不存在，跳过连接查询测试")
            }

            // 将查询执行时间添加到报告中
            report.queryExecutionTimes.addAll(localQueryExecutionTimes)

            // 计算平均查询时间（只考虑成功的查询）
            val successfulQueries = localQueryExecutionTimes.filter { it.executionTimeMs >= 0 }
            if (successfulQueries.isNotEmpty()) {
                report.averageQueryTimeMs = successfulQueries.map { it.executionTimeMs }.average().toLong()
                report.successfulQueriesCount = successfulQueries.size
                report.failedQueriesCount = localQueryExecutionTimes.size - successfulQueries.size
            }

            // 更新全局查询执行时间列表
            synchronized(queryExecutionTimes) {
                queryExecutionTimes.clear()
                queryExecutionTimes.addAll(localQueryExecutionTimes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行性能测试查询失败", e)
            report.error = "执行性能测试查询失败: ${e.message}"
        }
    }

    /**
     * 安全地测量查询时间，使用局部列表存储结果
     */
    private fun safelyMeasureQueryLocal(
        connection: SupportSQLiteDatabase,
        query: String,
        description: String,
        localQueryExecutionTimes: MutableList<QueryExecutionTime>
    ) {
        try {
            measureQueryTimeLocal(connection, query, description, localQueryExecutionTimes)
        } catch (e: Exception) {
            Log.e(TAG, "测量查询 '$description' 时发生异常", e)
            // 记录失败的查询，但不影响其他查询的执行
            localQueryExecutionTimes.add(
                QueryExecutionTime(
                    query = query,
                    description = description,
                    executionTimeMs = -1,
                    error = e.message ?: "未知错误"
                )
            )
        }
    }

    /**
     * 测量查询执行时间，使用局部列表存储结果
     */
    private fun measureQueryTimeLocal(
        connection: SupportSQLiteDatabase,
        query: String,
        description: String,
        localQueryExecutionTimes: MutableList<QueryExecutionTime>
    ) {
        try {
            val startTime = System.currentTimeMillis()

            // 执行查询
            connection.query(query).use { cursor ->
                // 移动游标以确保查询被执行
                cursor.moveToFirst()
            }

            val executionTime = System.currentTimeMillis() - startTime

            // 记录查询执行时间
            localQueryExecutionTimes.add(
                QueryExecutionTime(
                    query = query,
                    description = description,
                    executionTimeMs = executionTime
                )
            )

            Log.d(TAG, "查询 '$description' 执行时间: $executionTime ms")
        } catch (e: Exception) {
            Log.e(TAG, "测量查询 '$description' 执行时间失败", e)

            // 记录失败的查询
            localQueryExecutionTimes.add(
                QueryExecutionTime(
                    query = query,
                    description = description,
                    executionTimeMs = -1,
                    error = e.message ?: "未知错误"
                )
            )
        }
    }

    /**
     * 安全地测量查询时间，单独处理每个查询的异常
     * @deprecated 使用 safelyMeasureQueryLocal 替代
     */
    @Deprecated("使用 safelyMeasureQueryLocal 替代", ReplaceWith("safelyMeasureQueryLocal(connection, query, description, queryExecutionTimes)"))
    private fun safelyMeasureQuery(connection: SupportSQLiteDatabase, query: String, description: String) {
        // 这个方法已被 safelyMeasureQueryLocal 替代
        Log.w(TAG, "使用已废弃的 safelyMeasureQuery 方法")
        synchronized(queryExecutionTimes) {
            safelyMeasureQueryLocal(connection, query, description, queryExecutionTimes)
        }
    }

    /**
     * 测量查询执行时间
     * @deprecated 使用 measureQueryTimeLocal 替代
     */
    @Deprecated("使用 measureQueryTimeLocal 替代", ReplaceWith("measureQueryTimeLocal(connection, query, description, queryExecutionTimes)"))
    private fun measureQueryTime(connection: SupportSQLiteDatabase, query: String, description: String) {
        // 这个方法已被 measureQueryTimeLocal 替代
        Log.w(TAG, "使用已废弃的 measureQueryTime 方法")
        synchronized(queryExecutionTimes) {
            measureQueryTimeLocal(connection, query, description, queryExecutionTimes)
        }
    }

    /**
     * 收集连接统计信息
     */
    private fun collectConnectionStats(report: DatabasePerformanceReport) {
        try {
            val stats = connectionManager.getConnectionStats()
            report.activeConnections = stats.activeConnections
            report.trackedConnections = stats.trackedConnections
            report.maxConnections = stats.maxConnections
        } catch (e: Exception) {
            Log.e(TAG, "收集连接统计信息失败", e)
        }
    }

    /**
     * 生成性能报告
     */
    suspend fun generatePerformanceReport(): File? = withContext(Dispatchers.IO) {
        try {
            // 收集性能指标
            val report = collectPerformanceMetrics()

            // 创建报告目录
            val reportsDir = File(context.filesDir, REPORTS_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            // 创建报告文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val reportFile = File(
                reportsDir,
                "db_performance_${dateFormat.format(Date())}.txt"
            )

            // 写入报告内容
            reportFile.bufferedWriter().use { writer ->
                writer.write("数据库性能报告\n")
                writer.write("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

                writer.write("数据库文件信息:\n")
                writer.write("- 数据库文件: ${if (report.databaseFileExists) "存在" else "不存在"}\n")
                writer.write("- 数据库文件大小: ${FileUtils.formatFileSize(report.databaseFileSize)}\n")
                writer.write("- 数据库文件路径: ${report.databaseFilePath}\n")
                writer.write("- WAL文件: ${if (report.walFileExists) "存在 (${FileUtils.formatFileSize(report.walFileSize)})" else "不存在"}\n")
                writer.write("- SHM文件: ${if (report.shmFileExists) "存在 (${FileUtils.formatFileSize(report.shmFileSize)})" else "不存在"}\n\n")

                writer.write("数据库配置:\n")
                report.databaseConfig.forEach { (key, value) ->
                    writer.write("- $key: $value\n")
                }
                writer.write("\n")

                writer.write("连接统计:\n")
                writer.write("- 活跃连接数: ${report.activeConnections}\n")
                writer.write("- 跟踪连接数: ${report.trackedConnections}\n")
                writer.write("- 最大连接数: ${report.maxConnections}\n\n")

                writer.write("表信息:\n")
                report.tables.forEach { table ->
                    val rowCount = report.tableRowCounts[table] ?: 0
                    writer.write("- $table: $rowCount 行\n")
                }
                writer.write("\n")

                writer.write("索引信息:\n")
                report.indexes.forEach { index ->
                    writer.write("- $index\n")
                }
                writer.write("\n")

                writer.write("页信息:\n")
                writer.write("- 页大小: ${FileUtils.formatFileSize(report.pageSize.toLong())}\n")
                writer.write("- 页数: ${report.pageCount}\n\n")

                writer.write("查询性能:\n")
                writer.write("- 平均查询时间: ${report.averageQueryTimeMs} ms\n")
                writer.write("- 成功查询数: ${report.successfulQueriesCount}\n")
                writer.write("- 失败查询数: ${report.failedQueriesCount}\n\n")

                writer.write("查询执行时间详情:\n")
                report.queryExecutionTimes.forEach { queryTime ->
                    writer.write("- ${queryTime.description}: ${queryTime.executionTimeMs} ms\n")
                    writer.write("  查询: ${queryTime.query}\n")
                    if (queryTime.error != null) {
                        writer.write("  错误: ${queryTime.error}\n")
                    }
                    writer.write("\n")
                }

                writer.write("性能指标收集耗时: ${report.collectionTimeMs} ms\n")

                if (report.error != null) {
                    writer.write("\n错误信息: ${report.error}\n")
                }
            }

            Log.i(TAG, "性能报告已生成: ${reportFile.absolutePath}")
            return@withContext reportFile
        } catch (e: Exception) {
            Log.e(TAG, "生成性能报告失败", e)
            return@withContext null
        }
    }

    /**
     * 启动性能监控
     */
    fun startMonitoring() {
        Log.i(TAG, "启动数据库性能监控")

        // 在后台生成性能报告
        CoroutineScope(Dispatchers.IO).launch {
            generatePerformanceReport()
        }
    }

    /**
     * 查询执行时间数据类
     */
    data class QueryExecutionTime(
        val query: String,
        val description: String,
        val executionTimeMs: Long,
        val error: String? = null
    )

    /**
     * 数据库性能报告数据类
     */
    data class DatabasePerformanceReport(
        // 数据库文件信息
        var databaseFileExists: Boolean = false,
        var databaseFileSize: Long = 0,
        var databaseFilePath: String = "",
        var walFileExists: Boolean = false,
        var walFileSize: Long = 0,
        var shmFileExists: Boolean = false,
        var shmFileSize: Long = 0,

        // 数据库配置
        val databaseConfig: MutableMap<String, Any> = mutableMapOf(),

        // 连接统计
        var activeConnections: Int = 0,
        var trackedConnections: Int = 0,
        var maxConnections: Int = 0,

        // 表和索引信息
        val tables: MutableList<String> = mutableListOf(),
        val indexes: MutableList<String> = mutableListOf(),
        val tableRowCounts: MutableMap<String, Long> = mutableMapOf(),

        // 页信息
        var pageSize: Int = 0,
        var pageCount: Int = 0,

        // 查询性能
        var averageQueryTimeMs: Long = 0,
        val queryExecutionTimes: MutableList<QueryExecutionTime> = mutableListOf(),
        var successfulQueriesCount: Int = 0,
        var failedQueriesCount: Int = 0,

        // 性能指标收集耗时
        var collectionTimeMs: Long = 0,

        // 错误信息
        var error: String? = null
    )
}
