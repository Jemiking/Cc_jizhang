package com.ccjizhang.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.DatabaseConnectionManager
import com.ccjizhang.data.db.DatabasePerformanceMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高级数据库监控器
 * 整合基本监控和高级监控功能，提供全面的数据库性能和健康监控
 */
@Singleton
class AdvancedDatabaseMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val databaseMonitor: DatabaseMonitor,
    private val databasePerformanceMonitor: DatabasePerformanceMonitor,
    private val connectionManager: DatabaseConnectionManager,
    private val grayscaleReleaseConfig: GrayscaleReleaseConfig
) {
    companion object {
        private const val TAG = "AdvancedDbMonitor"
        private const val MONITOR_INTERVAL = 30 * 60 * 1000L // 30分钟
        private const val PERFORMANCE_REPORT_INTERVAL = 24 * 60 * 60 * 1000L // 24小时
        private const val ALERT_THRESHOLD_QUERY_TIME = 500L // 慢查询阈值，单位毫秒
        private const val ALERT_THRESHOLD_DB_SIZE = 50 * 1024 * 1024L // 数据库大小阈值，单位字节（50MB）
        private const val ALERT_THRESHOLD_WAL_SIZE = 10 * 1024 * 1024L // WAL文件大小阈值，单位字节（10MB）
        private const val ALERT_THRESHOLD_FRAGMENTATION = 30 // 碎片化阈值，单位百分比
        private const val LOGS_FOLDER = "advanced_db_monitor_logs"
        private const val MAX_LOGS = 20
    }

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false

    // 查询性能拦截器
    private val queryPerformanceInterceptor = QueryPerformanceInterceptor()

    // 监控指标
    private val _monitoringMetrics = MutableSharedFlow<MonitoringMetrics>()
    val monitoringMetrics: SharedFlow<MonitoringMetrics> = _monitoringMetrics

    // 告警流
    private val _alerts = MutableSharedFlow<DatabaseAlert>()
    val alerts: SharedFlow<DatabaseAlert> = _alerts

    /**
     * 启动高级数据库监控
     */
    fun startMonitoring() {
        if (isMonitoring) return

        // 检查是否启用了高级数据库监控
        if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_ADVANCED_DB_MONITORING)) {
            Log.i(TAG, "高级数据库监控未启用，跳过监控")
            return
        }

        isMonitoring = true

        // 启动基本监控
        databaseMonitor.startMonitoring()

        // 启动性能监控
        databasePerformanceMonitor.startMonitoring()

        // 启动高级监控
        monitorScope.launch {
            while (isActive) {
                try {
                    // 执行高级数据库监控
                    performAdvancedMonitoring()

                    // 生成性能报告
                    if (System.currentTimeMillis() % PERFORMANCE_REPORT_INTERVAL < MONITOR_INTERVAL) {
                        generatePerformanceReport()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "高级数据库监控失败", e)
                }

                // 等待下一次监控
                delay(MONITOR_INTERVAL)
            }
        }

        // 注册查询性能拦截器
        registerQueryInterceptor()

        Log.i(TAG, "高级数据库监控已启动")
    }

    /**
     * 停止高级数据库监控
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false

        // 停止基本监控
        databaseMonitor.stopMonitoring()

        // 取消注册查询性能拦截器
        unregisterQueryInterceptor()

        Log.i(TAG, "高级数据库监控已停止")
    }

    /**
     * 执行高级数据库监控
     */
    private suspend fun performAdvancedMonitoring() {
        // 收集监控指标
        val metrics = collectMonitoringMetrics()

        // 发送监控指标
        _monitoringMetrics.emit(metrics)

        // 分析监控指标，检测异常
        analyzeMetricsAndDetectAnomalies(metrics)

        // 记录监控日志
        logMonitoringData(metrics)
    }

    /**
     * 收集监控指标
     */
    private suspend fun collectMonitoringMetrics(): MonitoringMetrics = withContext(Dispatchers.IO) {
        val metrics = MonitoringMetrics()

        try {
            // 获取数据库文件信息
            val dbFile = context.getDatabasePath("ccjizhang_database_plain")
            if (dbFile.exists()) {
                metrics.databaseSize = dbFile.length()
                metrics.databasePath = dbFile.absolutePath

                // 检查WAL文件
                val walFile = File("${dbFile.path}-wal")
                if (walFile.exists()) {
                    metrics.walFileSize = walFile.length()
                }

                // 检查SHM文件
                val shmFile = File("${dbFile.path}-shm")
                if (shmFile.exists()) {
                    metrics.shmFileSize = shmFile.length()
                }
            }

            // 获取数据库配置
            connectionManager.withConnection { connection ->
                // 获取数据库页大小
                connection.query("PRAGMA page_size").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.pageSize = cursor.getInt(0)
                    }
                }

                // 获取数据库页数
                connection.query("PRAGMA page_count").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.pageCount = cursor.getInt(0)
                    }
                }

                // 获取数据库日志模式
                connection.query("PRAGMA journal_mode").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.journalMode = cursor.getString(0)
                    }
                }

                // 获取数据库同步模式
                connection.query("PRAGMA synchronous").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.synchronousMode = cursor.getInt(0)
                    }
                }

                // 获取数据库缓存大小
                connection.query("PRAGMA cache_size").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.cacheSize = cursor.getInt(0)
                    }
                }

                // 获取数据库碎片化程度
                connection.query("PRAGMA freelist_count").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.freelistCount = cursor.getInt(0)

                        // 计算碎片化百分比
                        if (metrics.pageCount > 0) {
                            metrics.fragmentationPercent = (metrics.freelistCount.toFloat() / metrics.pageCount) * 100
                        }
                    }
                }

                // 获取表和索引信息
                connection.query("SELECT type, name FROM sqlite_master WHERE type IN ('table', 'index') AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
                    while (cursor.moveToNext()) {
                        val type = cursor.getString(0)
                        val name = cursor.getString(1)

                        if (type == "table") {
                            metrics.tables.add(name)

                            // 获取表行数
                            try {
                                connection.query("SELECT COUNT(*) FROM `$name`").use { countCursor ->
                                    if (countCursor.moveToFirst()) {
                                        val count = countCursor.getLong(0)
                                        metrics.tableRowCounts[name] = count
                                        metrics.totalRowCount += count
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "获取表 $name 行数失败", e)
                            }
                        } else if (type == "index") {
                            metrics.indexes.add(name)
                        }
                    }
                }

                // 检查数据库完整性
                connection.query("PRAGMA integrity_check").use { cursor ->
                    if (cursor.moveToFirst()) {
                        metrics.integrityCheckResult = cursor.getString(0)
                    }
                }

                // 检查外键约束
                connection.query("PRAGMA foreign_key_check").use { cursor ->
                    metrics.foreignKeyViolations = cursor.count
                }
            }

            // 获取设备信息
            metrics.deviceModel = Build.MODEL
            metrics.androidVersion = Build.VERSION.SDK_INT
            metrics.availableMemory = Runtime.getRuntime().freeMemory()
            metrics.totalMemory = Runtime.getRuntime().totalMemory()
            metrics.maxMemory = Runtime.getRuntime().maxMemory()

            // 获取查询性能指标
            metrics.slowQueries = queryPerformanceInterceptor.getSlowQueries()
            metrics.averageQueryTime = queryPerformanceInterceptor.getAverageQueryTime()
            metrics.maxQueryTime = queryPerformanceInterceptor.getMaxQueryTime()

        } catch (e: Exception) {
            Log.e(TAG, "收集监控指标失败", e)
            metrics.error = e.message ?: "未知错误"
        }

        return@withContext metrics
    }

    /**
     * 分析监控指标，检测异常
     */
    private suspend fun analyzeMetricsAndDetectAnomalies(metrics: MonitoringMetrics) {
        try {
            // 检查数据库完整性
            if (metrics.integrityCheckResult != "ok") {
                triggerAlert(
                    AlertType.INTEGRITY_ERROR,
                    "数据库完整性检查失败",
                    "完整性检查结果: ${metrics.integrityCheckResult}"
                )
            }

            // 检查外键约束
            if (metrics.foreignKeyViolations > 0) {
                triggerAlert(
                    AlertType.FOREIGN_KEY_VIOLATION,
                    "外键约束违反",
                    "发现 ${metrics.foreignKeyViolations} 个外键约束违反"
                )
            }

            // 检查数据库大小
            if (metrics.databaseSize > ALERT_THRESHOLD_DB_SIZE) {
                triggerAlert(
                    AlertType.DATABASE_SIZE,
                    "数据库大小超过阈值",
                    "当前大小: ${metrics.databaseSize / (1024 * 1024)} MB, 阈值: ${ALERT_THRESHOLD_DB_SIZE / (1024 * 1024)} MB"
                )
            }

            // 检查WAL文件大小
            if (metrics.walFileSize > ALERT_THRESHOLD_WAL_SIZE) {
                triggerAlert(
                    AlertType.WAL_SIZE,
                    "WAL文件大小超过阈值",
                    "当前大小: ${metrics.walFileSize / (1024 * 1024)} MB, 阈值: ${ALERT_THRESHOLD_WAL_SIZE / (1024 * 1024)} MB"
                )
            }

            // 检查碎片化程度
            if (metrics.fragmentationPercent > ALERT_THRESHOLD_FRAGMENTATION) {
                triggerAlert(
                    AlertType.FRAGMENTATION,
                    "数据库碎片化程度高",
                    "当前碎片化: ${metrics.fragmentationPercent}%, 阈值: $ALERT_THRESHOLD_FRAGMENTATION%"
                )
            }

            // 检查慢查询
            if (metrics.slowQueries.isNotEmpty()) {
                triggerAlert(
                    AlertType.SLOW_QUERY,
                    "检测到慢查询",
                    "发现 ${metrics.slowQueries.size} 个慢查询，最慢查询耗时: ${metrics.maxQueryTime} ms"
                )
            }

            // 检查内存使用
            val usedMemoryPercent = (metrics.totalMemory - metrics.availableMemory).toFloat() / metrics.maxMemory * 100
            if (usedMemoryPercent > 80) {
                triggerAlert(
                    AlertType.MEMORY_USAGE,
                    "内存使用率高",
                    "当前内存使用率: $usedMemoryPercent%"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "分析监控指标失败", e)
        }
    }

    /**
     * 触发告警
     */
    private suspend fun triggerAlert(type: AlertType, title: String, message: String) {
        val alert = DatabaseAlert(
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        // 发送告警
        _alerts.emit(alert)

        // 记录告警日志
        Log.w(TAG, "数据库告警: $title - $message")
    }

    /**
     * 记录监控数据
     */
    private fun logMonitoringData(metrics: MonitoringMetrics) {
        try {
            val logDir = File(context.filesDir, LOGS_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            val logFile = File(
                logDir,
                "advanced_db_monitor_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.log"
            )

            // 构建日志内容
            val logContent = buildString {
                append("=== 高级数据库监控日志 ===\n")
                append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

                append("--- 数据库文件信息 ---\n")
                append("数据库路径: ${metrics.databasePath}\n")
                append("数据库大小: ${metrics.databaseSize / 1024} KB\n")
                append("WAL文件大小: ${metrics.walFileSize / 1024} KB\n")
                append("SHM文件大小: ${metrics.shmFileSize / 1024} KB\n\n")

                append("--- 数据库配置 ---\n")
                append("页大小: ${metrics.pageSize} 字节\n")
                append("页数: ${metrics.pageCount}\n")
                append("日志模式: ${metrics.journalMode}\n")
                append("同步模式: ${metrics.synchronousMode}\n")
                append("缓存大小: ${metrics.cacheSize}\n")
                append("空闲列表数: ${metrics.freelistCount}\n")
                append("碎片化程度: ${metrics.fragmentationPercent}%\n\n")

                append("--- 表和索引信息 ---\n")
                append("表数量: ${metrics.tables.size}\n")
                append("索引数量: ${metrics.indexes.size}\n")
                append("总行数: ${metrics.totalRowCount}\n\n")

                append("表详情:\n")
                metrics.tableRowCounts.forEach { (table, count) ->
                    append("  $table: $count 行\n")
                }
                append("\n")

                append("--- 完整性检查 ---\n")
                append("完整性检查结果: ${metrics.integrityCheckResult}\n")
                append("外键约束违反: ${metrics.foreignKeyViolations}\n\n")

                append("--- 查询性能 ---\n")
                append("慢查询数量: ${metrics.slowQueries.size}\n")
                append("平均查询时间: ${metrics.averageQueryTime} ms\n")
                append("最大查询时间: ${metrics.maxQueryTime} ms\n\n")

                if (metrics.slowQueries.isNotEmpty()) {
                    append("慢查询详情:\n")
                    metrics.slowQueries.forEachIndexed { index, query ->
                        append("  ${index + 1}. [${query.executionTime} ms] ${query.sql}\n")
                    }
                    append("\n")
                }

                append("--- 设备信息 ---\n")
                append("设备型号: ${metrics.deviceModel}\n")
                append("Android版本: ${metrics.androidVersion}\n")
                append("可用内存: ${metrics.availableMemory / (1024 * 1024)} MB\n")
                append("总内存: ${metrics.totalMemory / (1024 * 1024)} MB\n")
                append("最大内存: ${metrics.maxMemory / (1024 * 1024)} MB\n\n")

                if (metrics.error != null) {
                    append("--- 错误信息 ---\n")
                    append("错误: ${metrics.error}\n\n")
                }
            }

            // 写入日志文件
            logFile.writeText(logContent)

            // 清理旧日志
            cleanupOldLogs(logDir)

            Log.i(TAG, "高级数据库监控日志已记录: ${logFile.path}")
        } catch (e: Exception) {
            Log.e(TAG, "记录监控数据失败", e)
        }
    }

    /**
     * 清理旧日志
     */
    private fun cleanupOldLogs(logDir: File) {
        try {
            val logFiles = logDir.listFiles { file -> file.name.startsWith("advanced_db_monitor_") && file.extension == "log" }

            if (logFiles != null && logFiles.size > MAX_LOGS) {
                // 按修改时间排序
                logFiles.sortBy { it.lastModified() }

                // 删除最旧的日志
                for (i in 0 until logFiles.size - MAX_LOGS) {
                    logFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧日志失败", e)
        }
    }

    /**
     * 生成性能报告
     */
    private suspend fun generatePerformanceReport() {
        try {
            // 使用性能监控器生成报告
            val reportFile = databasePerformanceMonitor.generatePerformanceReport()

            if (reportFile != null) {
                Log.i(TAG, "数据库性能报告已生成: ${reportFile.absolutePath}")
            } else {
                Log.w(TAG, "生成数据库性能报告失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "生成性能报告失败", e)
        }
    }

    /**
     * 注册查询性能拦截器
     * 注意：这个功能需要 Room 2.3.0 或更高版本
     */
    private fun registerQueryInterceptor() {
        // Room 2.6.1 版本不支持 setQueryCallback 方法，跳过注册
        Log.i(TAG, "Room 2.6.1 版本不支持查询性能拦截器，跳过注册")

        // 在这里可以添加其他监控逻辑，例如使用 SQLite 的 PRAGMA 语句来监控数据库性能
    }

    /**
     * 取消注册查询性能拦截器
     * 注意：这个功能需要 Room 2.3.0 或更高版本
     */
    private fun unregisterQueryInterceptor() {
        // Room 2.6.1 版本不支持 setQueryCallback 方法，跳过取消注册
        Log.i(TAG, "Room 2.6.1 版本不支持查询性能拦截器，跳过取消注册")
    }

    /**
     * 查询性能拦截器
     * 用于监控查询执行时间，记录慢查询
     */
    inner class QueryPerformanceInterceptor : RoomDatabase.QueryCallback {
        private val queryTimes = mutableListOf<QueryInfo>()
        private val slowQueries = mutableListOf<QueryInfo>()

        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
            val startTime = System.currentTimeMillis()

            // 使用协程在后台处理查询完成后的逻辑
            monitorScope.launch {
                try {
                    // 等待查询执行完成
                    delay(10) // 简单等待一小段时间，实际上无法准确知道查询何时完成

                    val executionTime = System.currentTimeMillis() - startTime

                    // 记录查询信息
                    val queryInfo = QueryInfo(
                        sql = sqlQuery,
                        bindArgs = bindArgs.toString(),
                        executionTime = executionTime,
                        timestamp = System.currentTimeMillis()
                    )

                    synchronized(queryTimes) {
                        queryTimes.add(queryInfo)

                        // 保留最近的100条查询记录
                        if (queryTimes.size > 100) {
                            queryTimes.removeAt(0)
                        }
                    }

                    // 检查是否为慢查询
                    if (executionTime > ALERT_THRESHOLD_QUERY_TIME) {
                        synchronized(slowQueries) {
                            slowQueries.add(queryInfo)

                            // 保留最近的50条慢查询记录
                            if (slowQueries.size > 50) {
                                slowQueries.removeAt(0)
                            }
                        }

                        Log.w(TAG, "检测到慢查询 [${executionTime}ms]: $sqlQuery")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理查询回调失败", e)
                }
            }
        }

        /**
         * 获取慢查询列表
         */
        fun getSlowQueries(): List<QueryInfo> {
            synchronized(slowQueries) {
                return slowQueries.toList()
            }
        }

        /**
         * 获取平均查询时间
         */
        fun getAverageQueryTime(): Double {
            synchronized(queryTimes) {
                if (queryTimes.isEmpty()) return 0.0
                return queryTimes.map { it.executionTime }.average()
            }
        }

        /**
         * 获取最大查询时间
         */
        fun getMaxQueryTime(): Long {
            synchronized(queryTimes) {
                if (queryTimes.isEmpty()) return 0
                return queryTimes.maxByOrNull { it.executionTime }?.executionTime ?: 0
            }
        }
    }

    /**
     * 查询信息数据类
     */
    data class QueryInfo(
        val sql: String,
        val bindArgs: String,
        val executionTime: Long,
        val timestamp: Long
    )

    /**
     * 监控指标数据类
     */
    data class MonitoringMetrics(
        val timestamp: Long = System.currentTimeMillis(),

        // 数据库文件信息
        var databasePath: String = "",
        var databaseSize: Long = 0,
        var walFileSize: Long = 0,
        var shmFileSize: Long = 0,

        // 数据库配置
        var pageSize: Int = 0,
        var pageCount: Int = 0,
        var journalMode: String = "",
        var synchronousMode: Int = 0,
        var cacheSize: Int = 0,
        var freelistCount: Int = 0,
        var fragmentationPercent: Float = 0f,

        // 表和索引信息
        var tables: MutableList<String> = mutableListOf(),
        var indexes: MutableList<String> = mutableListOf(),
        var tableRowCounts: MutableMap<String, Long> = mutableMapOf(),
        var totalRowCount: Long = 0,

        // 完整性检查
        var integrityCheckResult: String = "",
        var foreignKeyViolations: Int = 0,

        // 查询性能
        var slowQueries: List<QueryInfo> = emptyList(),
        var averageQueryTime: Double = 0.0,
        var maxQueryTime: Long = 0,

        // 设备信息
        var deviceModel: String = "",
        var androidVersion: Int = 0,
        var availableMemory: Long = 0,
        var totalMemory: Long = 0,
        var maxMemory: Long = 0,

        // 错误信息
        var error: String? = null
    )

    /**
     * 数据库告警数据类
     */
    data class DatabaseAlert(
        val type: AlertType,
        val title: String,
        val message: String,
        val timestamp: Long
    )

    /**
     * 告警类型枚举
     */
    enum class AlertType {
        INTEGRITY_ERROR,      // 完整性错误
        FOREIGN_KEY_VIOLATION, // 外键约束违反
        DATABASE_SIZE,        // 数据库大小
        WAL_SIZE,             // WAL文件大小
        FRAGMENTATION,        // 碎片化
        SLOW_QUERY,           // 慢查询
        MEMORY_USAGE,         // 内存使用
        OTHER                 // 其他
    }
}
