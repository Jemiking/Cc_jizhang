package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.DataExportImportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 自动备份Worker
 * 定期执行数据库备份，保存到应用私有目录
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dataExportImportRepository: DataExportImportRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AutoBackupWorker"
        private const val BACKUP_FOLDER = "auto_backups"
        private const val MAX_BACKUPS = 5 // 最多保留的备份文件数量
        private const val PREFS_NAME = "auto_backup_prefs"
        private const val KEY_CUSTOM_BACKUP_PATH = "custom_backup_path"
        private const val KEY_CUSTOM_BACKUP_URI = "custom_backup_uri"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 获取备份设置
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val customPath = prefs.getString(KEY_CUSTOM_BACKUP_PATH, "") ?: ""
            val customUriString = prefs.getString(KEY_CUSTOM_BACKUP_URI, null)
            val customUri = if (!customUriString.isNullOrEmpty()) {
                try {
                    Uri.parse(customUriString)
                } catch (e: Exception) {
                    Log.e(TAG, "解析URI失败: $customUriString", e)
                    null
                }
            } else null

            // 生成备份文件名
            val backupFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date()) + "_autobackup.json"

            // 根据设置决定备份方式
            if (customUri != null) {
                // 使用 URI 创建备份
                try {
                    val docFile = DocumentFile.fromTreeUri(applicationContext, customUri)
                    if (docFile != null && docFile.exists() && docFile.isDirectory) {
                        // 创建新文件
                        val newFile = docFile.createFile("application/json", backupFileName)
                        if (newFile != null) {
                            Log.d(TAG, "开始创建自动备份: $backupFileName 到 ${docFile.uri}")

                            // 写入数据
                            applicationContext.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                val jsonData = dataExportImportRepository.exportDataToJson(applicationContext)
                                outputStream.write(jsonData.toByteArray())
                                outputStream.flush()
                            }

                            // 清理旧文件
                            cleanupOldBackupsFromUri(customUri)

                            Log.d(TAG, "自动备份完成: $backupFileName")
                            return@withContext Result.success()
                        } else {
                            Log.e(TAG, "无法在所选目录创建文件")
                            // 失败时回退到默认方式
                        }
                    } else {
                        Log.e(TAG, "所选目录不存在或无效")
                        // 失败时回退到默认方式
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "使用 URI 创建备份失败", e)
                    // 失败时回退到默认方式
                }
            }

            // 如果 URI 方式失败或未设置 URI，尝试使用自定义路径或默认路径
            val backupDir = if (customPath.isNotEmpty()) {
                File(customPath).apply {
                    if (!exists()) {
                        val created = mkdirs()
                        if (!created) {
                            Log.e(TAG, "无法创建自定义备份目录: $customPath")
                            // 如果无法创建自定义目录，回退到默认目录
                            File(applicationContext.filesDir, BACKUP_FOLDER).apply {
                                if (!exists()) mkdirs()
                            }
                        } else {
                            this
                        }
                    } else {
                        this
                    }
                }
            } else {
                File(applicationContext.filesDir, BACKUP_FOLDER).apply {
                    if (!exists()) mkdirs()
                }
            }

            val backupFile = File(backupDir, backupFileName)

            // 创建备份
            Log.d(TAG, "开始创建自动备份: $backupFileName 到 ${backupDir.absolutePath}")
            createBackup(backupFile)

            // 清理旧备份文件
            cleanupOldBackups(backupDir)

            Log.d(TAG, "自动备份完成: $backupFileName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "自动备份失败", e)
            Result.failure()
        }
    }

    /**
     * 创建数据库备份
     */
    private suspend fun createBackup(backupFile: File) {
        // 这里我们需要实现从数据库到JSON文件的导出逻辑
        // 由于DataExportImportRepository主要处理Uri，我们这里需要额外实现一个针对File的方法
        // 先简单使用现有仓库中的内部实现
        // 具体实现取决于项目中导出导入机制的设计

        // 使用导出方法
        val jsonData = dataExportImportRepository.exportDataToJson(applicationContext)

        // 写入文件
        FileWriter(backupFile).use { writer ->
            writer.write(jsonData)
            writer.flush()
        }
    }

    /**
     * 清理旧备份文件，只保留最新的几个
     */
    private fun cleanupOldBackups(backupDir: File) {
        val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith("_autobackup.json") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (backupFiles.size > MAX_BACKUPS) {
            backupFiles.drop(MAX_BACKUPS).forEach { file ->
                Log.d(TAG, "删除旧备份文件: ${file.name}")
                file.delete()
            }
        }
    }

    /**
     * 清理基于 URI 的旧备份文件
     */
    private fun cleanupOldBackupsFromUri(uri: Uri) {
        try {
            val docFile = DocumentFile.fromTreeUri(applicationContext, uri) ?: return
            if (!docFile.exists() || !docFile.isDirectory) return

            // 获取所有备份文件
            val backupFiles = docFile.listFiles()
                .filter { it.name?.endsWith("_autobackup.json") == true }
                .sortedByDescending { it.lastModified() }

            if (backupFiles.size > MAX_BACKUPS) {
                backupFiles.drop(MAX_BACKUPS).forEach { file ->
                    try {
                        Log.d(TAG, "删除旧备份文件: ${file.name}")
                        if (!file.delete()) {
                            Log.w(TAG, "无法删除旧备份文件: ${file.uri}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "删除旧备份文件时发生错误: ${file.uri}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理基于 URI 的旧备份文件失败", e)
        }
    }

    /**
     * 用于在应用中设置自动备份任务
     */
    class Scheduler @Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        /**
         * 设置自动备份任务
         * @param intervalDays 备份间隔天数
         */
        fun scheduleAutoBackup(intervalDays: Int = 1) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalDays.toLong(), TimeUnit.DAYS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "AUTO_BACKUP_WORK",
                ExistingPeriodicWorkPolicy.UPDATE, // 如果已存在则更新
                backupRequest
            )

            Log.d(TAG, "已设置每 $intervalDays 天执行一次自动备份")
        }

        /**
         * 取消自动备份任务
         */
        fun cancelAutoBackup() {
            WorkManager.getInstance(context).cancelUniqueWork("AUTO_BACKUP_WORK")
            Log.d(TAG, "已取消自动备份任务")
        }
    }
}