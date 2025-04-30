package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import com.ccjizhang.data.repository.DataExportImportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库恢复管理器
 * 负责数据库的备份和恢复操作
 */
@Singleton
class DatabaseRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportImportRepository: DataExportImportRepository
) {
    companion object {
        private const val TAG = "DatabaseRecoveryManager"
        private const val BACKUP_FOLDER = "db_backups"
        private const val MAX_BACKUPS = 5
    }

    /**
     * 创建定期备份
     * 将数据库内容导出为JSON文件保存在应用私有目录中
     */
    suspend fun createScheduledBackup() {
        try {
            val backupDir = File(context.filesDir, BACKUP_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            val backupFile = File(
                backupDir,
                "ccjizhang_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            )

            val result = dataExportImportRepository.exportDataToJsonFile(context, backupFile)
            if (result.isSuccess) {
                Log.i(TAG, "数据库备份成功: ${backupFile.path}")
                cleanupOldBackups(backupDir)
            } else {
                Log.e(TAG, "数据库备份失败: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建定期备份失败", e)
        }
    }

    /**
     * 从最新备份恢复
     * 从应用私有目录中找到最新的备份文件并恢复
     * @return 恢复是否成功
     */
    suspend fun restoreFromLatestBackup(): Boolean {
        try {
            val backupDir = File(context.filesDir, BACKUP_FOLDER)
            if (!backupDir.exists()) {
                Log.w(TAG, "备份目录不存在")
                return false
            }

            val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: return false

            if (backupFiles.isEmpty()) {
                Log.w(TAG, "没有找到备份文件")
                return false
            }

            val latestBackup = backupFiles[0]
            Log.i(TAG, "尝试从最新备份恢复: ${latestBackup.path}")
            
            val result = dataExportImportRepository.importDataFromJsonFile(context, latestBackup)
            if (result.isSuccess) {
                Log.i(TAG, "从备份恢复成功")
                return true
            } else {
                Log.e(TAG, "从备份恢复失败: ${result.exceptionOrNull()?.message}")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "从最新备份恢复失败", e)
            return false
        }
    }

    /**
     * 清理旧备份
     * 只保留最新的MAX_BACKUPS个备份文件
     */
    private fun cleanupOldBackups(backupDir: File) {
        try {
            val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() } ?: return

            if (backupFiles.size > MAX_BACKUPS) {
                backupFiles.drop(MAX_BACKUPS).forEach { 
                    it.delete()
                    Log.d(TAG, "删除旧备份文件: ${it.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧备份失败", e)
        }
    }
    
    /**
     * 获取所有备份文件
     * @return 备份文件列表，按时间降序排序
     */
    fun getAllBackups(): List<File> {
        val backupDir = File(context.filesDir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles()?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 从指定备份文件恢复
     * @param backupFile 备份文件
     * @return 恢复是否成功
     */
    suspend fun restoreFromBackup(backupFile: File): Boolean {
        try {
            if (!backupFile.exists() || !backupFile.name.endsWith(".json")) {
                Log.w(TAG, "无效的备份文件: ${backupFile.path}")
                return false
            }
            
            Log.i(TAG, "尝试从备份恢复: ${backupFile.path}")
            val result = dataExportImportRepository.importDataFromJsonFile(context, backupFile)
            
            if (result.isSuccess) {
                Log.i(TAG, "从备份恢复成功")
                return true
            } else {
                Log.e(TAG, "从备份恢复失败: ${result.exceptionOrNull()?.message}")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "从备份恢复失败", e)
            return false
        }
    }
    
    /**
     * 创建手动备份
     * @param fileName 备份文件名
     * @return 备份文件或null（如果备份失败）
     */
    suspend fun createManualBackup(fileName: String? = null): File? {
        try {
            val backupDir = File(context.filesDir, BACKUP_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            val backupFileName = fileName ?: "ccjizhang_manual_backup_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.json"
            
            val backupFile = File(backupDir, backupFileName)
            val result = dataExportImportRepository.exportDataToJsonFile(context, backupFile)
            
            return if (result.isSuccess) {
                Log.i(TAG, "手动备份成功: ${backupFile.path}")
                backupFile
            } else {
                Log.e(TAG, "手动备份失败: ${result.exceptionOrNull()?.message}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建手动备份失败", e)
            return null
        }
    }
}
