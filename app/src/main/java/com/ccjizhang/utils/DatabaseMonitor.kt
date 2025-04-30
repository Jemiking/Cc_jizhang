package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import com.ccjizhang.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库监控工具
 * 用于监控数据库性能和健康状况
 */
@Singleton
class DatabaseMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val grayscaleReleaseConfig: GrayscaleReleaseConfig
) {
    companion object {
        private const val TAG = "DatabaseMonitor"
        private const val MONITOR_INTERVAL = 60 * 60 * 1000L // 1小时
        private const val LOG_FOLDER = "db_monitor_logs"
        private const val MAX_LOGS = 10
        
        // 性能指标阈值
        private const val THRESHOLD_DB_SIZE_MB = 50 // 数据库大小阈值，单位MB
        private const val THRESHOLD_WAL_SIZE_MB = 10 // WAL文件大小阈值，单位MB
        private const val THRESHOLD_QUERY_TIME_MS = 500 // 查询时间阈值，单位毫秒
    }
    
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    
    /**
     * 启动数据库监控
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        // 检查是否启用了数据库监控
        if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING)) {
            Log.i(TAG, "数据库监控未启用，跳过监控")
            return
        }
        
        isMonitoring = true
        
        monitorScope.launch {
            while (isActive) {
                try {
                    // 执行数据库监控
                    monitorDatabaseHealth()
                    
                    // 记录监控日志
                    logMonitoringData()
                } catch (e: Exception) {
                    Log.e(TAG, "数据库监控失败", e)
                }
                
                // 等待下一次监控
                delay(MONITOR_INTERVAL)
            }
        }
        
        Log.i(TAG, "数据库监控已启动")
    }
    
    /**
     * 停止数据库监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        Log.i(TAG, "数据库监控已停止")
    }
    
    /**
     * 监控数据库健康状况
     */
    private suspend fun monitorDatabaseHealth() {
        // 检查数据库完整性
        checkDatabaseIntegrity()
        
        // 检查数据库大小
        checkDatabaseSize()
        
        // 检查WAL文件大小
        checkWalFileSize()
        
        // 检查数据库性能
        checkDatabasePerformance()
    }
    
    /**
     * 检查数据库完整性
     */
    private suspend fun checkDatabaseIntegrity() {
        try {
            val cursor = appDatabase.query("PRAGMA integrity_check", null)
            cursor.use {
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    if (result == "ok") {
                        Log.i(TAG, "数据库完整性检查通过")
                    } else {
                        Log.e(TAG, "数据库完整性检查失败: $result")
                        // 触发告警
                        triggerAlert("数据库完整性检查失败", "数据库可能已损坏，建议执行修复操作")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据库完整性检查异常", e)
            // 触发告警
            triggerAlert("数据库完整性检查异常", e.message ?: "未知错误")
        }
    }
    
    /**
     * 检查数据库大小
     */
    private fun checkDatabaseSize() {
        try {
            val dbFile = context.getDatabasePath("ccjizhang_database")
            if (dbFile.exists()) {
                val dbSizeMB = dbFile.length() / (1024 * 1024).toDouble()
                Log.i(TAG, "数据库大小: $dbSizeMB MB")
                
                // 如果数据库大小超过阈值，触发告警
                if (dbSizeMB > THRESHOLD_DB_SIZE_MB) {
                    triggerAlert("数据库大小超过阈值", "当前大小: $dbSizeMB MB, 阈值: $THRESHOLD_DB_SIZE_MB MB")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查数据库大小失败", e)
        }
    }
    
    /**
     * 检查WAL文件大小
     */
    private fun checkWalFileSize() {
        try {
            // 检查是否启用了WAL模式
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE)) {
                val dbFile = context.getDatabasePath("ccjizhang_database")
                val walFile = File(dbFile.path + "-wal")
                
                if (walFile.exists()) {
                    val walSizeMB = walFile.length() / (1024 * 1024).toDouble()
                    Log.i(TAG, "WAL文件大小: $walSizeMB MB")
                    
                    // 如果WAL文件大小超过阈值，触发告警
                    if (walSizeMB > THRESHOLD_WAL_SIZE_MB) {
                        triggerAlert("WAL文件大小超过阈值", "当前大小: $walSizeMB MB, 阈值: $THRESHOLD_WAL_SIZE_MB MB")
                        
                        // 执行检查点操作，减小WAL文件大小
                        appDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)", null)
                        Log.i(TAG, "执行WAL检查点操作，截断WAL文件")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查WAL文件大小失败", e)
        }
    }
    
    /**
     * 检查数据库性能
     */
    private fun checkDatabasePerformance() {
        try {
            // 执行一个简单的查询，测量查询时间
            val startTime = System.currentTimeMillis()
            val cursor = appDatabase.query("SELECT count(*) FROM sqlite_master", null)
            cursor.use {
                cursor.moveToFirst()
                val count = cursor.getInt(0)
            }
            val endTime = System.currentTimeMillis()
            val queryTime = endTime - startTime
            
            Log.i(TAG, "查询耗时: $queryTime ms")
            
            // 如果查询时间超过阈值，触发告警
            if (queryTime > THRESHOLD_QUERY_TIME_MS) {
                triggerAlert("数据库查询性能下降", "查询耗时: $queryTime ms, 阈值: $THRESHOLD_QUERY_TIME_MS ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查数据库性能失败", e)
        }
    }
    
    /**
     * 记录监控数据
     */
    private fun logMonitoringData() {
        try {
            val logDir = File(context.filesDir, LOG_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            val logFile = File(
                logDir,
                "db_monitor_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.log"
            )
            
            // 收集监控数据
            val monitorData = StringBuilder()
            monitorData.append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            
            // 数据库大小
            val dbFile = context.getDatabasePath("ccjizhang_database")
            if (dbFile.exists()) {
                val dbSizeMB = dbFile.length() / (1024 * 1024).toDouble()
                monitorData.append("数据库大小: $dbSizeMB MB\n")
            }
            
            // WAL文件大小
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE)) {
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) {
                    val walSizeMB = walFile.length() / (1024 * 1024).toDouble()
                    monitorData.append("WAL文件大小: $walSizeMB MB\n")
                }
            }
            
            // 数据库统计信息
            val statsCursor = appDatabase.query("PRAGMA database_list", null)
            statsCursor.use {
                monitorData.append("数据库列表:\n")
                while (statsCursor.moveToNext()) {
                    val seq = statsCursor.getInt(0)
                    val name = statsCursor.getString(1)
                    val file = statsCursor.getString(2)
                    monitorData.append("  $seq: $name - $file\n")
                }
            }
            
            // 表统计信息
            val tablesCursor = appDatabase.query("SELECT name FROM sqlite_master WHERE type='table'", null)
            tablesCursor.use {
                monitorData.append("表列表:\n")
                while (tablesCursor.moveToNext()) {
                    val tableName = tablesCursor.getString(0)
                    monitorData.append("  $tableName\n")
                    
                    // 获取表中的记录数
                    try {
                        val countCursor = appDatabase.query("SELECT count(*) FROM $tableName", null)
                        countCursor.use { countCursor ->
                            if (countCursor.moveToFirst()) {
                                val count = countCursor.getInt(0)
                                monitorData.append("    记录数: $count\n")
                            }
                        }
                    } catch (e: Exception) {
                        // 忽略系统表查询错误
                    }
                }
            }
            
            // 写入日志文件
            logFile.writeText(monitorData.toString())
            
            // 清理旧日志
            cleanupOldLogs(logDir)
            
            Log.i(TAG, "数据库监控日志已记录: ${logFile.path}")
        } catch (e: Exception) {
            Log.e(TAG, "记录监控数据失败", e)
        }
    }
    
    /**
     * 清理旧日志
     */
    private fun cleanupOldLogs(logDir: File) {
        try {
            val logFiles = logDir.listFiles()?.filter { it.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() } ?: return
            
            if (logFiles.size > MAX_LOGS) {
                logFiles.drop(MAX_LOGS).forEach { 
                    it.delete()
                    Log.d(TAG, "删除旧监控日志: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧监控日志失败", e)
        }
    }
    
    /**
     * 触发告警
     */
    private fun triggerAlert(title: String, message: String) {
        Log.w(TAG, "数据库告警: $title - $message")
        
        // 在实际应用中，这里可以实现更复杂的告警机制，如发送通知、记录到服务器等
        // 目前仅记录到日志中
    }
}
