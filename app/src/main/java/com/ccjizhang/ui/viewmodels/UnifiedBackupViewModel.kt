package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.ui.common.OperationResult
import com.ccjizhang.utils.AutoBackupWorker
import com.ccjizhang.utils.BackupReminderWorker
import com.ccjizhang.utils.DatabaseRecoveryManager
import com.ccjizhang.utils.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
 * 备份文件类型
 */
enum class BackupFileType {
    AUTO, // 自动备份
    MANUAL, // 手动备份
    UNKNOWN // 未知类型
}

/**
 * 备份文件信息
 */
data class BackupFileInfo(
    val file: File,
    val type: BackupFileType,
    val date: Date,
    val size: Long,
    val isSelected: Boolean = false
)

/**
 * 统一备份状态
 */
data class UnifiedBackupState(
    // 自动备份设置
    val isAutoBackupEnabled: Boolean = false,
    val backupIntervalDays: Int = 3, // 默认3天
    val reminderEnabled: Boolean = false,
    val reminderDays: Int = 7, // 默认7天

    // 备份路径设置
    val customBackupPath: String = "",
    val customBackupUri: Uri? = null,
    val displayPath: String = "",

    // 备份文件管理
    val backupFiles: List<BackupFileInfo> = emptyList(),
    val isLoadingFiles: Boolean = false,

    // 操作状态
    val isLoading: Boolean = false,
    val selectedBackupFile: BackupFileInfo? = null,

    // UI状态
    val showFileContent: Boolean = false,
    val fileContent: String = "",
    val isFileContentLoading: Boolean = false,

    // 验证状态
    val validationResult: ValidationResult? = null
)

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val categoryCount: Int = 0,
    val accountCount: Int = 0,
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val exportTime: String? = null,
    val version: String? = null,
    val hasConsistencyIssues: Boolean = false
)

/**
 * 统一备份ViewModel
 * 整合了自动备份和数据备份恢复功能
 */
@HiltViewModel
class UnifiedBackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportImportRepository: DataExportImportRepository,
    private val autoBackupScheduler: AutoBackupWorker.Scheduler,
    private val backupReminderScheduler: BackupReminderWorker.Scheduler,
    private val databaseRecoveryManager: DatabaseRecoveryManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "unified_backup_prefs"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_INTERVAL_DAYS = "backup_interval_days"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_DAYS = "reminder_days"
        private const val KEY_CUSTOM_BACKUP_PATH = "custom_backup_path"
        private const val KEY_CUSTOM_BACKUP_URI = "custom_backup_uri"

        const val DEFAULT_INTERVAL_DAYS = 3
        const val DEFAULT_REMINDER_DAYS = 7
        private const val MAX_BACKUP_FILES = 10
        private const val DEFAULT_BACKUP_FOLDER = "backups"

        // 预设路径选项
        val PRESET_PATHS = listOf(
            "Download" to "下载文件夹",
            "Documents" to "文档文件夹",
            "Pictures" to "图片文件夹"
        )
    }

    // 共享偏好设置
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 状态流
    private val _unifiedBackupState = MutableStateFlow(loadInitialState())
    val unifiedBackupState: StateFlow<UnifiedBackupState> = _unifiedBackupState.asStateFlow()

    // 操作结果流
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    init {
        loadBackupFiles()
    }

    /**
     * 加载初始状态
     */
    private fun loadInitialState(): UnifiedBackupState {
        val isAutoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        val backupIntervalDays = prefs.getInt(KEY_BACKUP_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS)
        val reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        val reminderDays = prefs.getInt(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS)
        val customBackupPath = prefs.getString(KEY_CUSTOM_BACKUP_PATH, "") ?: ""

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

        return UnifiedBackupState(
            isAutoBackupEnabled = isAutoBackupEnabled,
            backupIntervalDays = backupIntervalDays,
            reminderEnabled = reminderEnabled,
            reminderDays = reminderDays,
            customBackupPath = customBackupPath,
            customBackupUri = customBackupUri,
            displayPath = displayPath
        )
    }

    /**
     * 从URI获取显示路径
     */
    private fun getDisplayPathFromUri(uri: Uri?): String? {
        if (uri == null) return null

        return try {
            // 尝试获取路径显示名称
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name ?: uri.lastPathSegment ?: uri.toString()
        } catch (e: Exception) {
            Timber.e(e, "获取URI显示路径失败")
            uri.toString()
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
                _unifiedBackupState.update { it.copy(isAutoBackupEnabled = true, backupIntervalDays = intervalDays) }
                _operationResult.emit(OperationResult.Success("自动备份已启用"))
            } catch (e: Exception) {
                Timber.e(e, "启用自动备份失败")
                _operationResult.emit(OperationResult.Error("启用自动备份失败: ${e.localizedMessage}"))
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
                _unifiedBackupState.update { it.copy(isAutoBackupEnabled = false) }
                _operationResult.emit(OperationResult.Success("自动备份已禁用"))
            } catch (e: Exception) {
                Timber.e(e, "禁用自动备份失败")
                _operationResult.emit(OperationResult.Error("禁用自动备份失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 设置备份提醒
     */
    fun setBackupReminder(enabled: Boolean, reminderDays: Int = DEFAULT_REMINDER_DAYS) {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putBoolean(KEY_REMINDER_ENABLED, enabled)
                    .putInt(KEY_REMINDER_DAYS, reminderDays)
                    .apply()

                if (enabled) {
                    backupReminderScheduler.scheduleBackupReminder()
                } else {
                    backupReminderScheduler.cancelBackupReminder()
                }

                _unifiedBackupState.update {
                    it.copy(
                        reminderEnabled = enabled,
                        reminderDays = if (enabled) reminderDays else it.reminderDays
                    )
                }

                val message = if (enabled) "备份提醒已启用" else "备份提醒已禁用"
                _operationResult.emit(OperationResult.Success(message))
            } catch (e: Exception) {
                Timber.e(e, "设置备份提醒失败")
                _operationResult.emit(OperationResult.Error("设置备份提醒失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 设置自定义备份路径
     */
    fun setCustomBackupPath(path: String) {
        viewModelScope.launch {
            try {
                if (path.isBlank()) {
                    _operationResult.emit(OperationResult.Error("备份路径不能为空"))
                    return@launch
                }

                // 检查路径是否存在，如果不存在则创建
                val backupDir = File(path)
                if (!backupDir.exists()) {
                    val created = backupDir.mkdirs()
                    if (!created) {
                        _operationResult.emit(OperationResult.Error("无法创建备份目录，请检查权限"))
                        return@launch
                    }
                }

                // 检查是否可写
                if (!backupDir.canWrite()) {
                    _operationResult.emit(OperationResult.Error("备份目录不可写，请检查权限"))
                    return@launch
                }

                // 保存设置
                prefs.edit()
                    .putString(KEY_CUSTOM_BACKUP_PATH, path)
                    .putString(KEY_CUSTOM_BACKUP_URI, null) // 清除URI设置
                    .apply()

                _unifiedBackupState.update {
                    it.copy(
                        customBackupPath = path,
                        customBackupUri = null,
                        displayPath = path
                    )
                }

                _operationResult.emit(OperationResult.Success("备份路径已设置"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "设置备份路径失败")
                _operationResult.emit(OperationResult.Error("设置备份路径失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 设置自定义备份URI
     */
    fun setCustomBackupUri(uri: Uri) {
        viewModelScope.launch {
            try {
                // 获取持久权限
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                // 检查是否是目录
                val docFile = DocumentFile.fromTreeUri(context, uri)
                if (docFile == null || !docFile.exists() || !docFile.isDirectory) {
                    _operationResult.emit(OperationResult.Error("选择的不是有效目录"))
                    return@launch
                }

                // 保存设置
                val uriString = uri.toString()
                prefs.edit()
                    .putString(KEY_CUSTOM_BACKUP_URI, uriString)
                    .putString(KEY_CUSTOM_BACKUP_PATH, "") // 清除路径设置
                    .apply()

                val displayPath = getDisplayPathFromUri(uri) ?: uriString
                _unifiedBackupState.update {
                    it.copy(
                        customBackupUri = uri,
                        customBackupPath = "",
                        displayPath = displayPath
                    )
                }

                _operationResult.emit(OperationResult.Success("备份目录已设置"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "设置备份URI失败")
                _operationResult.emit(OperationResult.Error("设置备份目录失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 清除自定义备份路径
     */
    fun clearCustomBackupPath() {
        viewModelScope.launch {
            try {
                prefs.edit()
                    .putString(KEY_CUSTOM_BACKUP_PATH, "")
                    .putString(KEY_CUSTOM_BACKUP_URI, null)
                    .apply()

                _unifiedBackupState.update {
                    it.copy(
                        customBackupPath = "",
                        customBackupUri = null,
                        displayPath = "默认路径"
                    )
                }

                _operationResult.emit(OperationResult.Success("已恢复默认备份路径"))

                // 重新加载备份文件列表
                loadBackupFiles()
            } catch (e: Exception) {
                Timber.e(e, "清除备份路径失败")
                _operationResult.emit(OperationResult.Error("清除备份路径失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 加载备份文件列表
     */
    fun loadBackupFiles() {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoadingFiles = true) }

                val state = _unifiedBackupState.value
                val customUri = state.customBackupUri
                val customPath = state.customBackupPath

                val backupFiles = withContext(Dispatchers.IO) {
                    val fileList = mutableListOf<BackupFileInfo>()

                    // 从不同位置加载文件
                    when {
                        // 优先使用 URI
                        customUri != null -> {
                            try {
                                // 使用 DocumentFile 加载文件
                                val docFile = DocumentFile.fromTreeUri(context, customUri)
                                if (docFile != null && docFile.exists() && docFile.isDirectory) {
                                    // 获取目录中的所有文件
                                    docFile.listFiles().forEach { documentFile ->
                                        val fileName = documentFile.name ?: ""
                                        if (documentFile.isFile) {
                                            val file = documentFileToFile(documentFile) ?: return@forEach
                                            val type = when {
                                                fileName.contains("_autobackup") -> BackupFileType.AUTO
                                                fileName.contains("_backup") -> BackupFileType.MANUAL
                                                else -> BackupFileType.UNKNOWN
                                            }

                                            fileList.add(
                                                BackupFileInfo(
                                                    file = file,
                                                    type = type,
                                                    date = Date(file.lastModified()),
                                                    size = file.length()
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "从 URI 加载文件失败: $customUri")
                            }
                        }
                        // 其次使用自定义路径
                        customPath.isNotEmpty() -> {
                            val backupDir = File(customPath)
                            if (backupDir.exists() && backupDir.isDirectory) {
                                backupDir.listFiles()?.forEach { file ->
                                    if (file.isFile) {
                                        val type = when {
                                            file.name.contains("_autobackup") -> BackupFileType.AUTO
                                            file.name.contains("_backup") -> BackupFileType.MANUAL
                                            else -> BackupFileType.UNKNOWN
                                        }

                                        fileList.add(
                                            BackupFileInfo(
                                                file = file,
                                                type = type,
                                                date = Date(file.lastModified()),
                                                size = file.length()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        // 最后使用默认路径
                        else -> {
                            val backupDir = File(context.filesDir, DEFAULT_BACKUP_FOLDER).apply {
                                if (!exists()) mkdirs()
                            }

                            backupDir.listFiles()?.forEach { file ->
                                if (file.isFile) {
                                    val type = when {
                                        file.name.contains("_autobackup") -> BackupFileType.AUTO
                                        file.name.contains("_backup") -> BackupFileType.MANUAL
                                        else -> BackupFileType.UNKNOWN
                                    }

                                    fileList.add(
                                        BackupFileInfo(
                                            file = file,
                                            type = type,
                                            date = Date(file.lastModified()),
                                            size = file.length()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 按日期排序（最新的在前）
                    fileList.sortedByDescending { it.date }
                }

                _unifiedBackupState.update { it.copy(backupFiles = backupFiles, isLoadingFiles = false) }
            } catch (e: Exception) {
                Timber.e(e, "加载备份文件列表失败")
                _unifiedBackupState.update { it.copy(isLoadingFiles = false) }
                _operationResult.emit(OperationResult.Error("加载备份文件列表失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 将DocumentFile转换为File
     */
    private suspend fun documentFileToFile(documentFile: DocumentFile): File? = withContext(Dispatchers.IO) {
        try {
            val fileName = documentFile.name ?: return@withContext null
            val tempFile = File(context.cacheDir, fileName)

            context.contentResolver.openInputStream(documentFile.uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            tempFile
        } catch (e: Exception) {
            Timber.e(e, "转换DocumentFile失败: ${documentFile.uri}")
            null
        }
    }

    /**
     * 创建手动备份
     */
    fun createManualBackup() {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val state = _unifiedBackupState.value
                val customUri = state.customBackupUri
                val customPath = state.customBackupPath

                // 生成备份文件名
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val dateString = dateFormat.format(Date())
                val backupFileName = "ccjizhang_${dateString}_backup.json"

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

                                    Result.success(Unit)
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
                        }
                        exportResult
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
                        }
                        exportResult
                    }
                }

                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("备份创建成功"))
                        // 重新加载备份文件列表
                        loadBackupFiles()
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("备份创建失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "创建备份失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("创建备份失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 清理旧备份文件（基于文件系统）
     */
    private suspend fun cleanupOldBackups(directory: File) = withContext(Dispatchers.IO) {
        try {
            val files = directory.listFiles()
                ?.filter { it.name.endsWith("_backup.json") || it.name.endsWith("_autobackup.json") }
                ?.sortedByDescending { it.lastModified() }
                ?: return@withContext

            // 保留最新的MAX_BACKUP_FILES个文件，删除其余的
            if (files.size > MAX_BACKUP_FILES) {
                files.subList(MAX_BACKUP_FILES, files.size).forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理旧备份文件失败")
        }
    }

    /**
     * 清理旧备份文件（基于URI）
     */
    private suspend fun cleanupOldBackupsFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val docFile = DocumentFile.fromTreeUri(context, uri) ?: return@withContext

            val files = docFile.listFiles()
                .filter { it.name?.endsWith("_backup.json") == true || it.name?.endsWith("_autobackup.json") == true }
                .sortedByDescending { it.lastModified() }

            // 保留最新的MAX_BACKUP_FILES个文件，删除其余的
            if (files.size > MAX_BACKUP_FILES) {
                files.subList(MAX_BACKUP_FILES, files.size).forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "清理旧备份文件失败")
        }
    }

    /**
     * 删除备份文件
     */
    fun deleteBackupFile(backupFile: BackupFileInfo) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val state = _unifiedBackupState.value
                val customUri = state.customBackupUri

                val result = withContext(Dispatchers.IO) {
                    try {
                        // 如果使用URI，需要找到对应的DocumentFile并删除
                        if (customUri != null) {
                            val docFile = DocumentFile.fromTreeUri(context, customUri)
                            if (docFile != null && docFile.exists()) {
                                val fileName = backupFile.file.name
                                val fileToDelete = docFile.findFile(fileName)
                                if (fileToDelete != null && fileToDelete.exists()) {
                                    val deleted = fileToDelete.delete()
                                    if (deleted) {
                                        Result.success(Unit)
                                    } else {
                                        Result.failure(Exception("无法删除文件"))
                                    }
                                } else {
                                    Result.failure(Exception("找不到要删除的文件"))
                                }
                            } else {
                                Result.failure(Exception("备份目录不存在"))
                            }
                        } else {
                            // 直接删除文件
                            val deleted = backupFile.file.delete()
                            if (deleted) {
                                Result.success(Unit)
                            } else {
                                Result.failure(Exception("无法删除文件"))
                            }
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }

                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("备份文件已删除"))
                        // 更新备份文件列表
                        val updatedFiles = _unifiedBackupState.value.backupFiles.filter { it != backupFile }
                        _unifiedBackupState.update { it.copy(backupFiles = updatedFiles) }
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("删除备份文件失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "删除备份文件失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("删除备份文件失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 打开备份文件并查看内容
     */
    fun openBackupFile(backupFile: BackupFileInfo) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update {
                    it.copy(
                        isFileContentLoading = true,
                        selectedBackupFile = backupFile
                    )
                }

                val content = withContext(Dispatchers.IO) {
                    try {
                        val file = backupFile.file
                        if (file.exists() && file.isFile) {
                            file.readText()
                        } else {
                            "无法读取文件内容"
                        }
                    } catch (e: Exception) {
                        "读取文件失败: ${e.localizedMessage}"
                    }
                }

                _unifiedBackupState.update {
                    it.copy(
                        isFileContentLoading = false,
                        showFileContent = true,
                        fileContent = content
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "打开备份文件失败")
                _unifiedBackupState.update {
                    it.copy(
                        isFileContentLoading = false,
                        showFileContent = false
                    )
                }
                _operationResult.emit(OperationResult.Error("打开备份文件失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 关闭文件内容查看
     */
    fun closeFileContent() {
        _unifiedBackupState.update {
            it.copy(
                showFileContent = false,
                fileContent = "",
                selectedBackupFile = null
            )
        }
    }

    /**
     * 验证备份文件
     */
    fun validateBackupFile(backupFile: BackupFileInfo) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val result = withContext(Dispatchers.IO) {
                    try {
                        val file = backupFile.file
                        if (file.exists() && file.isFile) {
                            dataExportImportRepository.validateBackupFile(context, file)
                        } else {
                            Result.failure(Exception("备份文件不存在或无效"))
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }

                result.fold(
                    onSuccess = { validationResult ->
                        if (validationResult.isValid) {
                            val message = buildString {
                                append("数据验证成功:\n")
                                append("- 分类: ${validationResult.categoryCount} 个\n")
                                append("- 账户: ${validationResult.accountCount} 个\n")
                                append("- 交易: ${validationResult.transactionCount} 个\n")
                                append("- 预算: ${validationResult.budgetCount} 个\n")

                                validationResult.exportTime?.let {
                                    append("- 导出时间: $it\n")
                                }

                                validationResult.version?.let {
                                    append("- 版本: $it\n")
                                }

                                if (validationResult.hasConsistencyIssues) {
                                    append("\n警告: 数据存在一致性问题，导入后可能需要手动修复")
                                }
                            }

                            _unifiedBackupState.update {
                                it.copy(
                                    validationResult = ValidationResult(
                                        isValid = true,
                                        categoryCount = validationResult.categoryCount,
                                        accountCount = validationResult.accountCount,
                                        transactionCount = validationResult.transactionCount,
                                        budgetCount = validationResult.budgetCount,
                                        exportTime = validationResult.exportTime,
                                        version = validationResult.version,
                                        hasConsistencyIssues = validationResult.hasConsistencyIssues
                                    )
                                )
                            }

                            _operationResult.emit(OperationResult.Success(message))
                        } else {
                            _unifiedBackupState.update {
                                it.copy(validationResult = ValidationResult(isValid = false))
                            }

                            _operationResult.emit(OperationResult.Error("数据无效: 未找到有效的数据记录"))
                        }
                    },
                    onFailure = { error ->
                        _unifiedBackupState.update {
                            it.copy(validationResult = ValidationResult(isValid = false))
                        }

                        _operationResult.emit(OperationResult.Error("验证备份文件失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "验证备份文件失败")
                _unifiedBackupState.update {
                    it.copy(
                        isLoading = false,
                        validationResult = ValidationResult(isValid = false)
                    )
                }
                _operationResult.emit(OperationResult.Error("验证备份文件失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 从备份文件恢复数据
     */
    fun restoreFromBackup(backupFile: BackupFileInfo) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val result = withContext(Dispatchers.IO) {
                    try {
                        val file = backupFile.file
                        if (file.exists() && file.isFile) {
                            dataExportImportRepository.importDataFromJsonFile(context, file)
                        } else {
                            Result.failure(Exception("备份文件不存在或无效"))
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }

                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("数据恢复成功，请重启应用以应用更改"))

                        // 显示通知
                        notificationHelper.showRestoreCompletedNotification(
                            title = "数据恢复完成",
                            message = "数据已成功恢复，请重启应用以应用更改"
                        )
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据恢复失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "从备份恢复数据失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("从备份恢复数据失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 导出数据为CSV格式
     */
    fun exportDataToCsv(baseUri: Uri) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val result = dataExportImportRepository.exportDataToCsv(context, baseUri)

                result.fold(
                    onSuccess = { uriList ->
                        _operationResult.emit(OperationResult.Success("成功导出${uriList.size}个文件"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据导出失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "导出CSV数据失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("导出CSV数据失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 从CSV文件导入数据
     */
    fun importDataFromCsv(
        categoryUri: Uri,
        accountUri: Uri,
        budgetUri: Uri,
        transactionUri: Uri
    ) {
        viewModelScope.launch {
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val result = dataExportImportRepository.importDataFromCsv(
                    context, categoryUri, accountUri, budgetUri, transactionUri
                )

                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("数据导入成功"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据导入失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "从CSV导入数据失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("从CSV导入数据失败: ${e.localizedMessage}"))
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
            try {
                _unifiedBackupState.update { it.copy(isLoading = true) }

                val result = dataExportImportRepository.cleanUpData(
                    clearTransactions,
                    clearCategories,
                    clearAccounts,
                    clearBudgets,
                    beforeDate
                )

                result.fold(
                    onSuccess = { stats ->
                        val message = "已清除 ${stats.transactionsDeleted} 条交易记录, " +
                                "${stats.categoriesDeleted} 个分类, " +
                                "${stats.accountsDeleted} 个账户, " +
                                "${stats.budgetsDeleted} 个预算"

                        _operationResult.emit(OperationResult.Success("数据清除成功\n$message"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("清除数据失败: ${error.localizedMessage}"))
                    }
                )

                _unifiedBackupState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "清除数据失败")
                _unifiedBackupState.update { it.copy(isLoading = false) }
                _operationResult.emit(OperationResult.Error("清除数据失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 获取当前备份目录
     */
    fun getBackupDirectory(): File {
        val state = _unifiedBackupState.value
        val customPath = state.customBackupPath

        return when {
            customPath.isNotEmpty() -> File(customPath)
            else -> File(context.filesDir, DEFAULT_BACKUP_FOLDER)
        }
    }
}
