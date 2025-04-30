package com.ccjizhang.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件日志树，用于将日志保存到文件中
 * 可以用于诊断问题和收集用户反馈
 */
@Singleton
class FileLoggingTree @Inject constructor(
    private val context: Context
) : Timber.Tree() {
    
    private val logDir: File by lazy {
        // 使用应用的外部文件目录，不需要存储权限
        val dir = File(context.getExternalFilesDir(null), "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    
    private val logFile: File by lazy {
        // 使用日期作为文件名
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fileName = "app_log_${dateFormat.format(Date())}.txt"
        File(logDir, fileName)
    }
    
    private val logBuffer = StringBuilder()
    private val maxBufferSize = 1024 * 5 // 5KB
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.INFO) return // 只记录INFO及以上级别
        
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val priorityChar = when (priority) {
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "?"
        }
        
        val logLine = "$timeStamp $priorityChar/$tag: $message\n"
        val exceptionLog = t?.let { "${Log.getStackTraceString(it)}\n" } ?: ""
        
        synchronized(logBuffer) {
            logBuffer.append(logLine)
            if (t != null) {
                logBuffer.append(exceptionLog)
            }
            
            if (logBuffer.length > maxBufferSize) {
                flushLogs()
            }
        }
    }
    
    /**
     * 将缓冲区中的日志写入文件
     */
    fun flushLogs() {
        synchronized(logBuffer) {
            if (logBuffer.isEmpty()) return
            
            try {
                FileWriter(logFile, true).use { writer ->
                    writer.write(logBuffer.toString())
                }
                logBuffer.clear()
            } catch (e: Exception) {
                Log.e("FileLoggingTree", "写入日志文件失败", e)
            }
        }
    }
    
    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        return logDir.listFiles()?.filter { it.name.startsWith("app_log_") } ?: emptyList()
    }
    
    /**
     * 清理旧的日志文件，只保留最近的几个
     */
    fun cleanupOldLogs(keepCount: Int = 5) {
        val files = getLogFiles().sortedByDescending { it.lastModified() }
        if (files.size > keepCount) {
            files.subList(keepCount, files.size).forEach { it.delete() }
        }
    }
    
    /**
     * 获取日志目录
     */
    fun getLogDirectory(): File {
        return logDir
    }
}
