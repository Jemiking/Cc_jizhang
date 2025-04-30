package com.ccjizhang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日志查看和分享工具类
 * 用于帮助用户查看和分享应用日志
 */
@Singleton
class LogViewerHelper @Inject constructor(
    private val context: Context,
    private val fileLoggingTree: FileLoggingTree
) {
    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        return fileLoggingTree.getLogFiles().sortedByDescending { it.lastModified() }
    }

    /**
     * 获取最新的日志文件
     */
    fun getLatestLogFile(): File? {
        return getLogFiles().firstOrNull()
    }

    /**
     * 读取日志文件内容
     */
    fun readLogFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Timber.e(e, "读取日志文件失败: ${file.name}")
            "无法读取日志文件: ${e.localizedMessage}"
        }
    }

    /**
     * 分享日志文件
     */
    fun shareLogFile(file: File): Intent? {
        return try {
            // 确保日志缓冲区已刷新
            fileLoggingTree.flushLogs()

            // 创建分享Intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Timber.e(e, "创建分享日志文件Intent失败: ${file.name}")
            null
        }
    }

    /**
     * 清理所有日志文件
     */
    fun clearAllLogs() {
        try {
            getLogFiles().forEach { it.delete() }
            Timber.i("所有日志文件已清除")
        } catch (e: Exception) {
            Timber.e(e, "清除日志文件失败")
        }
    }

    /**
     * 获取日志目录
     */
    fun getLogDirectory(): File {
        return fileLoggingTree.getLogDirectory()
    }

    /**
     * 收集诊断信息
     */
    fun collectDiagnosticInfo(): String {
        return buildString {
            append("应用版本: ${context.packageName} (${context.packageManager.getPackageInfo(context.packageName, 0).versionName})\n")
            append("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
            append("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
            append("可用内存: ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB\n")
            append("日志文件数量: ${getLogFiles().size}\n")
            append("日志目录: ${getLogDirectory().absolutePath}\n")
        }
    }
}
