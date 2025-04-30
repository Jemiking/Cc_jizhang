package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.MainActivity
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.utils.AutoBackupWorker
import com.ccjizhang.utils.BackupReminderWorker
import com.ccjizhang.utils.DatabaseRecoveryManager
import com.ccjizhang.utils.NotificationHelper
import android.app.PendingIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 备份恢复状态
 */
data class BackupRestoreState(
    // 自动备份相关状态
    val isAutoBackupEnabled: Boolean = false,
    val backupIntervalDays: Int = BackupRestoreViewModel.DEFAULT_INTERVAL_DAYS,
    val backupFiles: List<File> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val customBackupPath: String = "",
    val customBackupUri: Uri? = null,
    val displayPath: String = "",

    // 备份提醒相关状态
    val isBackupReminderEnabled: Boolean = false,
    val backupReminderDays: Int = 7,

    // 数据导出导入相关状态
    val isLoading: Boolean = false,
    val lastBackupTime: Long = 0L
)

/**
 * 备份恢复ViewModel
 * 整合了自动备份和数据备份恢复功能
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportImportRepository: DataExportImportRepository,
    private val autoBackupScheduler: AutoBackupWorker.Scheduler,
    private val databaseRecoveryManager: DatabaseRecoveryManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "backup_restore_prefs"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_INTERVAL_DAYS = "backup_interval_days"
        private const val KEY_CUSTOM_BACKUP_PATH = "custom_backup_path"
        private const val KEY_CUSTOM_BACKUP_URI = "custom_backup_uri"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_BACKUP_REMINDER_ENABLED = "backup_reminder_enabled"
        private const val KEY_BACKUP_REMINDER_DAYS = "backup_reminder_days"
        internal const val DEFAULT_INTERVAL_DAYS = 3
        private const val DEFAULT_REMINDER_DAYS = 7
        private const val MAX_BACKUP_FILES = 10
        private const val DEFAULT_BACKUP_FOLDER = "auto_backups"

        // 预设路径选项
        val PRESET_PATHS = listOf(
            "Download" to "下载文件夹",
            "Documents" to "文档文件夹",
            "Pictures" to "图片文件夹"
        )
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 状态流
    private val _backupRestoreState = MutableStateFlow(loadInitialState())
    val backupRestoreState: StateFlow<BackupRestoreState> = _backupRestoreState.asStateFlow()

    // 操作结果流
    private val _operationResult = MutableSharedFlow<com.ccjizhang.ui.common.OperationResult>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadBackupFiles() // ViewModel 初始化时加载备份文件列表
    }

    /**
     * 加载初始状态
     */
    private fun loadInitialState(): BackupRestoreState {
        val isEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        val intervalDays = prefs.getInt(KEY_BACKUP_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS)
        val customBackupPath = prefs.getString(KEY_CUSTOM_BACKUP_PATH, "") ?: ""
        val lastBackupTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)

        // 加载备份提醒设置
        val isReminderEnabled = prefs.getBoolean(KEY_BACKUP_REMINDER_ENABLED, false)
        val reminderDays = prefs.getInt(KEY_BACKUP_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)

        // 加载URI
        val uriString = prefs.getString(KEY_CUSTOM_BACKUP_URI, null)
        val customBackupUri = if (!uriString.isNullOrEmpty()) {
            try {
                Uri.parse(uriString)
            } catch (e: Exception) {
                Timber.e(e, "解析URI失败: $uriString")
                null
            }
        } else null

        // 生成显示路径
        val displayPath = getDisplayPathFromUri(customBackupUri) ?: customBackupPath

        return BackupRestoreState(
            isAutoBackupEnabled = isEnabled,
            backupIntervalDays = intervalDays,
            customBackupPath = customBackupPath,
            customBackupUri = customBackupUri,
            displayPath = displayPath,
            lastBackupTime = lastBackupTime,
            isBackupReminderEnabled = isReminderEnabled,
            backupReminderDays = reminderDays
        )
    }

    /**
     * 从 URI 获取可读的路径显示
     */
    private fun getDisplayPathFromUri(uri: Uri?): String? {
        if (uri == null) return null

        try {
            val docFile = DocumentFile.fromTreeUri(context, uri) ?: return null
            return docFile.name ?: uri.lastPathSegment ?: uri.toString()
        } catch (e: Exception) {
            Timber.e(e, "获取显示路径失败")
            return uri.toString()
        }
    }

    /**
     * 启用自动备份
     */
    fun enableAutoBackup(intervalDays: Int) {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putBoolean(KEY_AUTO_BACKUP_ENABLED, true)
                    .putInt(KEY_BACKUP_INTERVAL_DAYS, intervalDays)
                    .apply()

                autoBackupScheduler.scheduleAutoBackup(intervalDays)
                _backupRestoreState.update { it.copy(isAutoBackupEnabled = true, backupIntervalDays = intervalDays) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("自动备份已启用"))
            } catch (e: Exception) {
                Timber.e(e, "启用自动备份失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("启用自动备份失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 禁用自动备份
     */
    fun disableAutoBackup() {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putBoolean(KEY_AUTO_BACKUP_ENABLED, false)
                    .apply()

                autoBackupScheduler.cancelAutoBackup()
                _backupRestoreState.update { it.copy(isAutoBackupEnabled = false) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("自动备份已禁用"))
            } catch (e: Exception) {
                Timber.e(e, "禁用自动备份失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("禁用自动备份失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 更新备份频率
     */
    fun updateBackupInterval(intervalDays: Int) {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putInt(KEY_BACKUP_INTERVAL_DAYS, intervalDays)
                    .apply()

                if (_backupRestoreState.value.isAutoBackupEnabled) {
                    autoBackupScheduler.scheduleAutoBackup(intervalDays)
                }
                _backupRestoreState.update { it.copy(backupIntervalDays = intervalDays) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份频率已更新"))
            } catch (e: Exception) {
                Timber.e(e, "更新备份频率失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("更新备份频率失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 设置自定义备份 URI
     */
    fun setCustomBackupUri(uri: Uri) {
        viewModelScope.launch {
            try {
                // 获取持久性权限
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // 检查权限
                val canWrite = context.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isWritePermission
                }

                if (!canWrite) {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("无法获取写入权限，请重新选择文件夹"))
                    return@launch
                }

                // 获取显示路径
                val displayPath = getDisplayPathFromUri(uri) ?: uri.toString()

                // 保存设置
                prefs.edit()
                    .putString(KEY_CUSTOM_BACKUP_URI, uri.toString())
                    .putString(KEY_CUSTOM_BACKUP_PATH, "") // 清除旧的路径设置
                    .apply()

                _backupRestoreState.update { it.copy(
                    customBackupUri = uri,
                    customBackupPath = "",
                    displayPath = displayPath
                ) }

                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份路径已更新为: $displayPath"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "设置备份 URI 失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("设置备份路径失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 设置自定义备份路径
     */
    fun setCustomBackupPath(path: String) {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putString(KEY_CUSTOM_BACKUP_PATH, path)
                    .putString(KEY_CUSTOM_BACKUP_URI, null) // 清除 URI 设置
                    .apply()

                _backupRestoreState.update { it.copy(
                    customBackupPath = path,
                    customBackupUri = null,
                    displayPath = path
                ) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份路径已更新"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "设置备份路径失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("设置备份路径失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 清除自定义备份路径，恢复使用默认路径
     */
    fun clearCustomBackupPath() {
        viewModelScope.launch {
            try {
                // 释放所有持久性权限
                val uri = _backupRestoreState.value.customBackupUri
                if (uri != null) {
                    try {
                        val persistedUriPermissions = context.contentResolver.persistedUriPermissions
                        for (persistedPermission in persistedUriPermissions) {
                            if (persistedPermission.uri == uri) {
                                context.contentResolver.releasePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "释放 URI 权限失败")
                    }
                }

                prefs.edit()
                    .remove(KEY_CUSTOM_BACKUP_PATH)
                    .remove(KEY_CUSTOM_BACKUP_URI)
                    .apply()

                _backupRestoreState.update { it.copy(
                    customBackupPath = "",
                    customBackupUri = null,
                    displayPath = ""
                ) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("已恢复使用默认备份路径"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "清除备份路径失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("清除备份路径失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 加载备份文件列表并更新状态
     */
    fun loadBackupFiles() {
        refreshBackupFiles()
    }

    /**
     * 刷新备份文件列表
     */
    fun refreshBackupFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _backupRestoreState.update { it.copy(isLoadingFiles = true) }
            try {
                val state = _backupRestoreState.value
                val customUri = state.customBackupUri
                val customPath = state.customBackupPath

                val files = when {
                    // 优先使用 URI
                    customUri != null -> {
                        try {
                            // 使用 DocumentFile 加载文件
                            val docFile = DocumentFile.fromTreeUri(context, customUri)
                            if (docFile != null && docFile.exists() && docFile.isDirectory) {
                                // 获取目录中的所有文件
                                docFile.listFiles()
                                    .filter { it.name?.endsWith("_autobackup.json") == true }
                                    .mapNotNull { documentFileToFile(it) }
                                    .sortedByDescending { it.lastModified() }
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "从 URI 加载文件失败: $customUri")
                            emptyList()
                        }
                    }
                    // 其次使用自定义路径
                    customPath.isNotEmpty() -> {
                        val backupDir = File(customPath)
                        if (!backupDir.exists()) {
                            backupDir.mkdirs()
                            emptyList()
                        } else {
                            backupDir.listFiles()
                                ?.filter { it.name.endsWith("_autobackup.json") }
                                ?.sortedByDescending { it.lastModified() }
                                ?: emptyList()
                        }
                    }
                    // 最后使用默认路径
                    else -> {
                        val backupDir = File(context.filesDir, DEFAULT_BACKUP_FOLDER).apply {
                            if (!exists()) mkdirs()
                        }
                        backupDir.listFiles()
                            ?.filter { it.name.endsWith("_autobackup.json") }
                            ?.sortedByDescending { it.lastModified() }
                            ?: emptyList()
                    }
                }

                _backupRestoreState.update { it.copy(backupFiles = files, isLoadingFiles = false) }
            } catch (e: Exception) {
                 Timber.e(e, "加载备份文件列表失败")
                _backupRestoreState.update { it.copy(isLoadingFiles = false) } // 确保加载状态被重置
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("加载备份文件列表失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 将 DocumentFile 转换为 File
     * 注意：这会创建一个临时文件的副本
     */
    private suspend fun documentFileToFile(documentFile: DocumentFile): File? = withContext(Dispatchers.IO) {
        try {
            if (!documentFile.isFile) return@withContext null

            val name = documentFile.name ?: return@withContext null
            val uri = documentFile.uri
            val lastModified = documentFile.lastModified()

            // 创建临时文件
            val tempFile = File(context.cacheDir, name)

            // 复制文件内容
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 设置最后修改时间
            tempFile.setLastModified(lastModified)

            tempFile
        } catch (e: Exception) {
            Timber.e(e, "转换 DocumentFile 失败: ${documentFile.uri}")
            null
        }
    }

    /**
     * 删除指定的备份文件
     */
    fun deleteBackupFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists() && file.isFile) {
                    if (file.delete()) {
                        _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份文件 ${file.name} 已删除"))
                        // 更新文件列表状态
                        loadBackupFiles()
                    } else {
                        _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("无法删除备份文件 ${file.name}"))
                    }
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("备份文件 ${file.name} 不存在或不是有效文件"))
                }
            } catch (e: SecurityException) {
                Timber.e(e, "删除备份文件时出现安全异常")
                 _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("删除备份文件 ${file.name} 失败：权限不足"))
            } catch (e: Exception) {
                Timber.e(e, "删除备份文件时发生错误")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("删除备份文件 ${file.name} 失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 打开备份文件并返回其内容
     * @param file 要打开的备份文件
     * @return 文件内容或错误信息
     */
    suspend fun openBackupFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        return@withContext readBackupFileContent(file)
    }

    /**
     * 读取备份文件内容
     * @param file 要读取的备份文件
     * @return 文件内容或错误信息
     */
    suspend fun readBackupFileContent(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.isFile) {
                return@withContext Result.failure(Exception("文件不存在或不是有效文件"))
            }

            val content = file.readText()
            Result.success(content)
        } catch (e: Exception) {
            Timber.e(e, "打开备份文件失败: ${file.name}")
            Result.failure(e)
        }
    }

    /**
     * 创建手动备份
     */
    suspend fun createManualBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val state = _backupRestoreState.value
            val customUri = state.customBackupUri
            val customPath = state.customBackupPath

            // 生成备份文件名
            val backupFileName = "manual_${System.currentTimeMillis()}_autobackup.json"

            // 根据设置决定备份方式
            val result = when {
                // 使用 URI
                customUri != null -> {
                    try {
                        val docFile = DocumentFile.fromTreeUri(context, customUri)
                        if (docFile != null && docFile.exists() && docFile.isDirectory) {
                            // 创建新文件
                            val newFile = docFile.createFile("application/json", backupFileName)
                            if (newFile != null) {
                                // 写入数据
                                context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                    val jsonData = dataExportImportRepository.exportDataToJson(context)
                                    outputStream.write(jsonData.toByteArray())
                                    outputStream.flush()
                                }

                                // 清理旧文件
                                cleanupOldBackupsFromUri(customUri)

                                // 创建临时文件副本供显示
                                val tempFile = documentFileToFile(newFile)
                                if (tempFile != null) {
                                    Result.success(tempFile)
                                } else {
                                    Result.failure(Exception("无法创建临时文件副本"))
                                }
                            } else {
                                Result.failure(Exception("无法在所选目录创建文件"))
                            }
                        } else {
                            Result.failure(Exception("所选目录不存在或无效"))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "使用 URI 创建备份失败")
                        Result.failure(e)
                    }
                }
                // 使用自定义路径
                customPath.isNotEmpty() -> {
                    val backupDir = File(customPath).apply {
                        if (!exists()) {
                            mkdirs()
                        }
                    }
                    val backupFile = File(backupDir, backupFileName)
                    val exportResult = dataExportImportRepository.exportDataToJsonFile(context, backupFile)
                    if (exportResult.isSuccess) {
                        cleanupOldBackups(backupDir)
                        Result.success(backupFile)
                    } else {
                        Result.failure(exportResult.exceptionOrNull() ?: Exception("导出数据失败"))
                    }
                }
                // 使用默认路径
                else -> {
                    val backupDir = File(context.filesDir, DEFAULT_BACKUP_FOLDER).apply {
                        if (!exists()) {
                            mkdirs()
                        }
                    }
                    val backupFile = File(backupDir, backupFileName)
                    val exportResult = dataExportImportRepository.exportDataToJsonFile(context, backupFile)
                    if (exportResult.isSuccess) {
                        cleanupOldBackups(backupDir)
                        Result.success(backupFile)
                    } else {
                        Result.failure(exportResult.exceptionOrNull() ?: Exception("导出数据失败"))
                    }
                }
            }

            // 更新最后备份时间
            if (result.isSuccess) {
                val currentTime = System.currentTimeMillis()
                prefs.edit().putLong(KEY_LAST_BACKUP_TIME, currentTime).apply()
                _backupRestoreState.update { it.copy(lastBackupTime = currentTime) }

                // 重新加载备份文件列表
                loadBackupFiles()
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "创建手动备份失败")
            Result.failure(e)
        }
    }

    /**
     * 清理旧备份文件，只保留最新的几个
     */
    private fun cleanupOldBackups(backupDir: File) {
        val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith("_autobackup.json") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (backupFiles.size > MAX_BACKUP_FILES) {
            backupFiles.drop(MAX_BACKUP_FILES).forEach { file ->
                try {
                    if (!file.delete()) {
                         Timber.w("无法删除旧备份文件: ${file.absolutePath}")
                    }
                } catch (e: SecurityException) {
                    Timber.e(e, "删除旧备份文件时出现安全异常: ${file.absolutePath}")
                } catch (e: Exception) {
                    Timber.e(e, "删除旧备份文件时发生错误: ${file.absolutePath}")
                }
            }
        }
    }

    /**
     * 清理基于 URI 的旧备份文件
     */
    private fun cleanupOldBackupsFromUri(uri: Uri) {
        try {
            val docFile = DocumentFile.fromTreeUri(context, uri) ?: return
            if (!docFile.exists() || !docFile.isDirectory) return

            // 获取所有备份文件
            val backupFiles = docFile.listFiles()
                .filter { it.name?.endsWith("_autobackup.json") == true }
                .sortedByDescending { it.lastModified() }

            if (backupFiles.size > MAX_BACKUP_FILES) {
                backupFiles.drop(MAX_BACKUP_FILES).forEach { file ->
                    try {
                        if (!file.delete()) {
                            Timber.w("无法删除旧备份文件: ${file.uri}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "删除旧备份文件时发生错误: ${file.uri}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理基于 URI 的旧备份文件失败")
        }
    }

    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(extension: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        return "ccjizhang_backup_${dateString}.$extension"
    }

    /**
     * 将数据导出为JSON文件
     */
    fun exportDataToJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.exportDataToJsonUri(context, uri)
                if (result.isSuccess) {
                    // 更新最后备份时间
                    val currentTime = System.currentTimeMillis()
                    prefs.edit().putLong(KEY_LAST_BACKUP_TIME, currentTime).apply()
                    _backupRestoreState.update { it.copy(lastBackupTime = currentTime) }

                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("数据导出成功"))
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据导出失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "导出数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("导出数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 将数据导出为CSV文件
     */
    fun exportDataToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.exportDataToCsv(context, uri)
                if (result.isSuccess) {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("数据导出成功"))
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据导出失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "导出数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("导出数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 从 JSON 文件导入数据
     */
    fun importDataFromJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.importDataFromJsonUri(context, uri)
                if (result.isSuccess) {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("数据导入成功"))
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据导入失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "导入数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("导入数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 验证导入数据
     */
    fun validateImportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.validateImportData(context, uri)
                if (result.isSuccess) {
                    val validationResult = result.getOrNull()
                    if (validationResult != null && validationResult.isValid) {
                        _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("数据验证成功，可以安全导入"))
                    } else {
                        _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据验证失败，文件可能损坏或格式不正确"))
                    }
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据验证失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "验证数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("验证数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 从 CSV 文件导入数据
     */
    fun importDataFromCsv(
        context: Context,
        categoryUri: Uri,
        accountUri: Uri,
        budgetUri: Uri,
        transactionUri: Uri
    ) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.importDataFromCsv(
                    context,
                    categoryUri,
                    accountUri,
                    budgetUri,
                    transactionUri
                )
                if (result.isSuccess) {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("数据导入成功"))
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据导入失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "导入数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("导入数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 清理数据
     */
    fun cleanupData(
        clearTransactions: Boolean,
        clearCategories: Boolean,
        clearAccounts: Boolean,
        clearBudgets: Boolean,
        beforeDate: Date? = null
    ) {
        viewModelScope.launch {
            _backupRestoreState.update { it.copy(isLoading = true) }
            try {
                val result = dataExportImportRepository.cleanUpData(
                    clearTransactions,
                    clearCategories,
                    clearAccounts,
                    clearBudgets,
                    beforeDate
                )
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    val message = buildString {
                        append("数据清理成功\n")
                        if (clearTransactions) {
                            append("删除交易记录: ${stats?.transactionsDeleted ?: 0} 条\n")
                        }
                        if (clearCategories) {
                            append("删除自定义分类: ${stats?.categoriesDeleted ?: 0} 条\n")
                        }
                        if (clearAccounts) {
                            append("删除账户: ${stats?.accountsDeleted ?: 0} 个\n")
                        }
                        if (clearBudgets) {
                            append("删除预算: ${stats?.budgetsDeleted ?: 0} 个")
                        }
                    }
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success(message))
                } else {
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("数据清理失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "清理数据失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("清理数据失败: ${e.localizedMessage}"))
            } finally {
                _backupRestoreState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * 检查是否长时间未备份，并显示提醒通知
     * @param reminderDays 提醒天数，默认为7天
     */
    fun checkBackupReminder(reminderDays: Int = 7) {
        viewModelScope.launch {
            try {
                val lastBackupTime = _backupRestoreState.value.lastBackupTime
                if (lastBackupTime == 0L) {
                    // 从未备份过，显示提醒
                    showBackupReminderNotification("您尚未备份数据", "为避免数据丢失，建议立即备份您的数据")
                    return@launch
                }

                // 计算距离上次备份的天数
                val currentTime = System.currentTimeMillis()
                val daysSinceLastBackup = (currentTime - lastBackupTime) / (1000 * 60 * 60 * 24)

                if (daysSinceLastBackup >= reminderDays) {
                    // 超过指定天数未备份，显示提醒
                    showBackupReminderNotification(
                        "您已超过 $daysSinceLastBackup 天未备份数据",
                        "为避免数据丢失，建议立即备份您的数据"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "检查备份提醒失败")
            }
        }
    }

    /**
     * 显示备份提醒通知
     */
    private fun showBackupReminderNotification(title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = NotificationHelper.ACTION_OPEN_SETTINGS
                putExtra("OPEN_BACKUP_SCREEN", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            notificationHelper.showBackupReminderNotification(title, message, intent)
        } catch (e: Exception) {
            Timber.e(e, "显示备份提醒通知失败")
        }
    }

    /**
     * 设置备份提醒
     * @param enabled 是否启用备份提醒
     */
    fun setBackupReminder(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val reminderScheduler = BackupReminderWorker.Scheduler(context)

                // 保存设置
                prefs.edit()
                    .putBoolean(KEY_BACKUP_REMINDER_ENABLED, enabled)
                    .apply()

                // 更新状态
                _backupRestoreState.update { it.copy(isBackupReminderEnabled = enabled) }

                if (enabled) {
                    reminderScheduler.scheduleBackupReminder()
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份提醒已启用"))
                } else {
                    reminderScheduler.cancelBackupReminder()
                    _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份提醒已禁用"))
                }
            } catch (e: Exception) {
                Timber.e(e, "设置备份提醒失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("设置备份提醒失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 更新备份提醒天数
     * @param days 提醒天数
     */
    fun updateBackupReminderDays(days: Int) {
        viewModelScope.launch {
            try {
                // 保存设置
                prefs.edit()
                    .putInt(KEY_BACKUP_REMINDER_DAYS, days)
                    .apply()

                // 更新状态
                _backupRestoreState.update { it.copy(backupReminderDays = days) }

                // 如果已启用提醒，重新调度
                if (_backupRestoreState.value.isBackupReminderEnabled) {
                    val reminderScheduler = BackupReminderWorker.Scheduler(context)
                    reminderScheduler.scheduleBackupReminder()
                }

                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份提醒天数已更新"))
            } catch (e: Exception) {
                Timber.e(e, "更新备份提醒天数失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("更新备份提醒天数失败: ${e.localizedMessage}"))
            }
        }
    }
}
