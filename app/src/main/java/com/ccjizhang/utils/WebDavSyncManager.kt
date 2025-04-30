package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ccjizhang.data.model.WebDavConfig
import com.ccjizhang.data.repository.DataExportImportRepository
import com.google.gson.Gson
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebDAV同步管理器
 * 负责WebDAV服务器连接和数据同步
 */
@Singleton
class WebDavSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportImportRepository: DataExportImportRepository
) {
    companion object {
        private const val PREFS_NAME = "webdav_sync_prefs"
        private const val KEY_CONFIG = "webdav_config"
        private const val SYNC_WORK_NAME = "WEBDAV_SYNC_WORK"
        private const val BACKUP_FOLDER = "webdav_backups"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // 同步状态
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    /**
     * 获取当前WebDAV配置
     */
    fun getWebDavConfig(): WebDavConfig? {
        val configJson = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            Gson().fromJson(configJson, WebDavConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 保存WebDAV配置
     */
    fun saveWebDavConfig(config: WebDavConfig) {
        val configJson = Gson().toJson(config)
        prefs.edit().putString(KEY_CONFIG, configJson).apply()
        
        // 如果开启了自动同步，则设置定期同步任务
        if (config.autoSync) {
            scheduleSync(config)
        } else {
            cancelSync()
        }
    }
    
    /**
     * 删除WebDAV配置
     */
    fun deleteWebDavConfig() {
        prefs.edit().remove(KEY_CONFIG).apply()
        cancelSync()
    }
    
    /**
     * 测试WebDAV连接
     */
    suspend fun testConnection(config: WebDavConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(config.username, config.password)
            
            // 检查服务器URL是否可访问
            sardine.exists(config.serverUrl)
            
            // 尝试确保同步目录存在
            val remotePath = "${config.serverUrl}/${config.syncFolder}/"
            if (!sardine.exists(remotePath)) {
                try {
                    sardine.createDirectory(remotePath)
                } catch (e: Exception) {
                    // 创建目录失败，但不一定是连接问题，可能是权限问题
                    return@withContext sardine.exists(config.serverUrl)
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 执行同步操作
     */
    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val config = getWebDavConfig() ?: return@withContext SyncResult.Error("WebDAV配置不存在")
        
        try {
            _syncStatus.value = SyncStatus.Syncing("开始同步数据...")
            
            // 创建备份文件
            val backupFile = createBackupFile()
            if (backupFile == null) {
                _syncStatus.value = SyncStatus.Idle
                return@withContext SyncResult.Error("创建备份文件失败")
            }
            
            _syncStatus.value = SyncStatus.Syncing("正在上传数据到WebDAV服务器...")
            
            // 上传备份文件
            val result = uploadBackup(config, backupFile)
            
            _syncStatus.value = SyncStatus.Idle
            
            if (result) {
                return@withContext SyncResult.Success("数据同步成功")
            } else {
                return@withContext SyncResult.Error("上传备份文件失败")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = SyncStatus.Idle
            return@withContext SyncResult.Error("同步失败: ${e.localizedMessage}")
        }
    }
    
    /**
     * 创建备份文件
     */
    private suspend fun createBackupFile(): File? = withContext(Dispatchers.IO) {
        try {
            // 确保备份目录存在
            val backupDir = File(context.filesDir, BACKUP_FOLDER).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            
            // 生成备份文件名
            val backupFileName = "ccjizhang_webdav_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())}.json"
            val backupFile = File(backupDir, backupFileName)
            
            // 创建备份
            val result = dataExportImportRepository.exportDataToJsonFile(context, backupFile)
            
            if (result.isSuccess) {
                backupFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 上传备份文件到WebDAV服务器
     */
    private suspend fun uploadBackup(config: WebDavConfig, backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val sardine = OkHttpSardine()
            sardine.setCredentials(config.username, config.password)
            
            // 确保远程目录存在
            val remotePath = "${config.serverUrl}/${config.syncFolder}/"
            if (!sardine.exists(remotePath)) {
                sardine.createDirectory(remotePath)
            }
            
            // 上传文件
            val remoteFilePath = "$remotePath${backupFile.name}"
            sardine.put(remoteFilePath, backupFile, "application/octet-stream")
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 下载最新备份文件
     */
    suspend fun downloadLatestBackup(): File? = withContext(Dispatchers.IO) {
        val config = getWebDavConfig() ?: return@withContext null
        
        try {
            _syncStatus.value = SyncStatus.Syncing("正在从WebDAV服务器下载数据...")
            
            val sardine = OkHttpSardine()
            sardine.setCredentials(config.username, config.password)
            
            // 远程目录路径
            val remotePath = "${config.serverUrl}/${config.syncFolder}/"
            if (!sardine.exists(remotePath)) {
                _syncStatus.value = SyncStatus.Idle
                return@withContext null
            }
            
            // 获取远程文件列表
            val resources = sardine.list(remotePath)
            if (resources.isEmpty()) {
                _syncStatus.value = SyncStatus.Idle
                return@withContext null
            }
            
            // 找到最新的备份文件
            val latestBackup = resources
                .filter { it.name.endsWith(".json") && it.name.contains("ccjizhang_webdav_") }
                .maxByOrNull { it.modified }
                ?: run {
                    _syncStatus.value = SyncStatus.Idle
                    return@withContext null
                }
            
            // 确保本地备份目录存在
            val backupDir = File(context.filesDir, BACKUP_FOLDER).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            
            // 下载文件
            val fileName = latestBackup.name.substring(latestBackup.name.lastIndexOf("/") + 1)
            val localFile = File(backupDir, fileName)
            
            sardine.get(latestBackup.path).use { inputStream ->
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            _syncStatus.value = SyncStatus.Idle
            
            localFile
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = SyncStatus.Idle
            null
        }
    }
    
    /**
     * 恢复备份文件
     */
    suspend fun restoreFromFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.Syncing("正在恢复数据...")
            
            // 将File转换为Uri
            val fileUri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            
            val result = dataExportImportRepository.importDataFromJson(context, fileUri)
            
            _syncStatus.value = SyncStatus.Idle
            
            result.isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = SyncStatus.Idle
            false
        }
    }
    
    /**
     * 设置定期同步任务
     */
    private fun scheduleSync(config: WebDavConfig) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<WebDavSyncWorker>(
            config.syncInterval, TimeUnit.HOURS
        )
        .setConstraints(constraints)
        .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }
    
    /**
     * 取消同步任务
     */
    private fun cancelSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }
    
    /**
     * 同步状态
     */
    sealed class SyncStatus {
        object Idle : SyncStatus()
        data class Syncing(val message: String) : SyncStatus()
    }
    
    /**
     * 同步结果
     */
    sealed class SyncResult {
        data class Success(val message: String) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
    
    /**
     * WebDAV同步Worker
     * 用于后台执行同步任务
     */
    @HiltWorker
    class WebDavSyncWorker @AssistedInject constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val webDavSyncManager: WebDavSyncManager
    ) : CoroutineWorker(appContext, workerParams) {
        
        override suspend fun doWork(): Result {
            return try {
                val syncResult = webDavSyncManager.sync()
                
                when (syncResult) {
                    is SyncResult.Success -> Result.success()
                    is SyncResult.Error -> Result.retry()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }
} 