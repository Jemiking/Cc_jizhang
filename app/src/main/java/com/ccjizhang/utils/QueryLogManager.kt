package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询日志管理器
 * 用于管理和分析 Room 数据库的查询日志
 */
@Singleton
class QueryLogManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val grayscaleReleaseConfig: GrayscaleReleaseConfig
) {
    companion object {
        private const val TAG = "QueryLogManager"
        private const val QUERY_LOG_FOLDER = "query_logs"
        private const val MAX_LOGS = 10
        private const val MAX_QUERIES_PER_LOG = 1000
    }

    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isLoggingEnabled = false

    // 查询日志
    private val queryLogs = mutableListOf<QueryLog>()

    // 查询日志拦截器
    private val queryLogInterceptor = QueryLogInterceptor()

    /**
     * 启用查询日志
     */
    fun enableQueryLogging(database: RoomDatabase) {
        if (isLoggingEnabled) return

        // 检查是否启用了查询日志
        if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_QUERY_LOGGING)) {
            Log.i(TAG, "查询日志未启用，跳过")
            return
        }

        // 在所有模式下都启用查询日志
        // 实际应用中可能需要根据调试模式来决定是否启用

        isLoggingEnabled = true

        // 注册查询日志拦截器
        registerQueryInterceptor(database)

        Log.i(TAG, "查询日志已启用")
    }

    /**
     * 禁用查询日志
     */
    fun disableQueryLogging(database: RoomDatabase) {
        if (!isLoggingEnabled) return

        isLoggingEnabled = false

        // 取消注册查询日志拦截器
        unregisterQueryInterceptor(database)

        // 保存当前日志
        saveCurrentLog()

        Log.i(TAG, "查询日志已禁用")
    }

    /**
     * 注册查询日志拦截器
     * 注意：这个功能需要 Room 2.3.0 或更高版本
     */
    private fun registerQueryInterceptor(database: RoomDatabase) {
        // Room 2.6.1 版本不支持 setQueryCallback 方法，跳过注册
        Log.i(TAG, "Room 2.6.1 版本不支持查询日志拦截器，跳过注册")

        // 在这里可以添加其他日志记录逻辑，例如使用 SQLite 的 PRAGMA 语句来记录查询
    }

    /**
     * 取消注册查询日志拦截器
     * 注意：这个功能需要 Room 2.3.0 或更高版本
     */
    private fun unregisterQueryInterceptor(database: RoomDatabase) {
        // Room 2.6.1 版本不支持 setQueryCallback 方法，跳过取消注册
        Log.i(TAG, "Room 2.6.1 版本不支持查询日志拦截器，跳过取消注册")
    }

    /**
     * 保存当前日志
     */
    private fun saveCurrentLog() {
        if (queryLogs.isEmpty()) return

        logScope.launch {
            try {
                val logDir = File(context.filesDir, QUERY_LOG_FOLDER).apply {
                    if (!exists()) mkdirs()
                }

                val logFile = File(
                    logDir,
                    "query_log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.log"
                )

                // 构建日志内容
                val logContent = buildString {
                    append("=== 查询日志 ===\n")
                    append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    append("查询数量: ${queryLogs.size}\n\n")

                    queryLogs.forEachIndexed { index, log ->
                        append("${index + 1}. [${SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))}] ")
                        append("[${log.executionTime}ms] ")
                        append("${log.sql}\n")

                        if (log.bindArgs.isNotEmpty()) {
                            append("   参数: ${log.bindArgs}\n")
                        }

                        if (log.stackTrace.isNotEmpty()) {
                            append("   调用栈:\n")
                            log.stackTrace.forEach { line ->
                                append("     $line\n")
                            }
                        }

                        append("\n")
                    }
                }

                // 写入日志文件
                logFile.writeText(logContent)

                // 清理旧日志
                cleanupOldLogs(logDir)

                // 清空当前日志
                synchronized(queryLogs) {
                    queryLogs.clear()
                }

                Log.i(TAG, "查询日志已保存: ${logFile.path}")
            } catch (e: Exception) {
                Log.e(TAG, "保存查询日志失败", e)
            }
        }
    }

    /**
     * 清理旧日志
     */
    private fun cleanupOldLogs(logDir: File) {
        try {
            val logFiles = logDir.listFiles { file -> file.name.startsWith("query_log_") && file.extension == "log" }

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
     * 分析查询日志
     * 返回查询日志分析结果
     */
    fun analyzeQueryLogs(): QueryLogAnalysisResult {
        val result = QueryLogAnalysisResult()

        synchronized(queryLogs) {
            if (queryLogs.isEmpty()) {
                return result
            }

            // 计算总查询数
            result.totalQueries = queryLogs.size

            // 计算平均执行时间
            result.averageExecutionTime = queryLogs.map { it.executionTime }.average()

            // 计算最大执行时间
            result.maxExecutionTime = queryLogs.maxByOrNull { it.executionTime }?.executionTime ?: 0

            // 计算最小执行时间
            result.minExecutionTime = queryLogs.minByOrNull { it.executionTime }?.executionTime ?: 0

            // 统计查询类型
            queryLogs.forEach { log ->
                val sql = log.sql.trim().uppercase()
                when {
                    sql.startsWith("SELECT") -> result.selectCount++
                    sql.startsWith("INSERT") -> result.insertCount++
                    sql.startsWith("UPDATE") -> result.updateCount++
                    sql.startsWith("DELETE") -> result.deleteCount++
                    else -> result.otherCount++
                }
            }

            // 查找最慢的查询
            result.slowestQueries = queryLogs
                .sortedByDescending { it.executionTime }
                .take(10)
                .map { log ->
                    SlowQuery(
                        sql = log.sql,
                        executionTime = log.executionTime,
                        timestamp = log.timestamp,
                        stackTrace = log.stackTrace
                    )
                }

            // 查找最频繁的查询
            val queryCounts = mutableMapOf<String, Int>()
            queryLogs.forEach { log ->
                val sql = log.sql.trim()
                queryCounts[sql] = (queryCounts[sql] ?: 0) + 1
            }

            result.frequentQueries = queryCounts.entries
                .sortedByDescending { it.value }
                .take(10)
                .map { (sql, count) ->
                    FrequentQuery(
                        sql = sql,
                        count = count,
                        averageExecutionTime = queryLogs
                            .filter { it.sql.trim() == sql }
                            .map { it.executionTime }
                            .average()
                    )
                }
        }

        return result
    }

    /**
     * 查询日志拦截器
     */
    inner class QueryLogInterceptor : RoomDatabase.QueryCallback {
        override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
            if (!isLoggingEnabled) return

            val startTime = System.currentTimeMillis()

            // 使用协程在后台处理查询完成后的逻辑
            logScope.launch {
                try {
                    // 等待查询执行完成（这里只是简单等待，实际上无法准确知道查询何时完成）
                    kotlinx.coroutines.delay(10)

                    val executionTime = System.currentTimeMillis() - startTime

                    // 获取调用栈
                    val stackTrace = Thread.currentThread().stackTrace
                        .drop(3) // 跳过前3个无关的栈帧
                        .take(10) // 只取前10个栈帧
                        .map { it.toString() }
                        .filter { it.contains("com.ccjizhang") } // 只保留应用代码的栈帧

                    // 记录查询日志
                    val queryLog = QueryLog(
                        sql = sqlQuery,
                        bindArgs = bindArgs.toString(),
                        executionTime = executionTime,
                        timestamp = startTime,
                        stackTrace = stackTrace
                    )

                    synchronized(queryLogs) {
                        queryLogs.add(queryLog)

                        // 如果日志数量超过限制，保存当前日志
                        if (queryLogs.size >= MAX_QUERIES_PER_LOG) {
                            saveCurrentLog()
                        }
                    }

                    // 记录慢查询
                    if (executionTime > 100) {
                        Log.w(TAG, "慢查询 [${executionTime}ms]: $sqlQuery")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理查询日志失败", e)
                }
            }
        }
    }

    /**
     * 查询日志数据类
     */
    data class QueryLog(
        val sql: String,
        val bindArgs: String,
        val executionTime: Long,
        val timestamp: Long,
        val stackTrace: List<String>
    )

    /**
     * 查询日志分析结果数据类
     */
    data class QueryLogAnalysisResult(
        var totalQueries: Int = 0,
        var averageExecutionTime: Double = 0.0,
        var maxExecutionTime: Long = 0,
        var minExecutionTime: Long = 0,
        var selectCount: Int = 0,
        var insertCount: Int = 0,
        var updateCount: Int = 0,
        var deleteCount: Int = 0,
        var otherCount: Int = 0,
        var slowestQueries: List<SlowQuery> = emptyList(),
        var frequentQueries: List<FrequentQuery> = emptyList()
    )

    /**
     * 慢查询数据类
     */
    data class SlowQuery(
        val sql: String,
        val executionTime: Long,
        val timestamp: Long,
        val stackTrace: List<String>
    )

    /**
     * 频繁查询数据类
     */
    data class FrequentQuery(
        val sql: String,
        val count: Int,
        val averageExecutionTime: Double
    )
}
