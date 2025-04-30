package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.utils.AutoBackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import com.ccjizhang.ui.common.OperationResult
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import android.content.Intent
import android.net.Uri

/**
 * 自动备份设置状态
 */
data class AutoBackupState(
    val isEnabled: Boolean = false,
    val intervalDays: Int = AutoBackupViewModel.DEFAULT_INTERVAL_DAYS,
    val backupFiles: List<File> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val customBackupPath: String = "",
    val customBackupUri: Uri? = null,
    val displayPath: String = ""
)

/**
 * 自动备份ViewModel
 */
@HiltViewModel
class AutoBackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportImportRepository: DataExportImportRepository,
    private val autoBackupScheduler: AutoBackupWorker.Scheduler
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "auto_backup_prefs"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_INTERVAL_DAYS = "backup_interval_days"
        private const val KEY_CUSTOM_BACKUP_PATH = "custom_backup_path"
        private const val KEY_CUSTOM_BACKUP_URI = "custom_backup_uri"
        internal const val DEFAULT_INTERVAL_DAYS = 3 // Make internal or public if needed elsewhere
        private const val MAX_BACKUP_FILES = 10 // 定义最大备份文件数量
        private const val DEFAULT_BACKUP_FOLDER = "auto_backups" // 默认备份文件夹

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
    private val _autoBackupState = MutableStateFlow(loadInitialState())
    val autoBackupState: StateFlow<AutoBackupState> = _autoBackupState.asStateFlow()

    // 操作结果流
    private val _operationResult = MutableSharedFlow<com.ccjizhang.ui.common.OperationResult>()
    val operationResult = _operationResult.asSharedFlow()

    init {
        loadBackupFiles() // ViewModel 初始化时加载备份文件列表
    }

    private fun loadInitialState(): AutoBackupState {
        val isEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        val intervalDays = prefs.getInt(KEY_BACKUP_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS)
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

        return AutoBackupState(
            isEnabled = isEnabled,
            intervalDays = intervalDays,
            customBackupPath = customBackupPath,
            customBackupUri = customBackupUri,
            displayPath = displayPath
        )
    }

    /**
     * 从 SharedPreferences 加载并更新状态（如果需要）
     * 这个函数可能不需要了，因为状态在 _autoBackupState 初始化时加载
     */
    private fun refreshStateFromPrefs() {
        _autoBackupState.update {
            it.copy(
                isEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false),
                intervalDays = prefs.getInt(KEY_BACKUP_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS)
            )
        }
    }

    /**
     * 是否启用了自动备份 (从 StateFlow 获取)
     */
    fun isAutoBackupEnabled(): Boolean {
        return _autoBackupState.value.isEnabled
    }

    /**
     * 获取当前备份间隔天数 (从 StateFlow 获取)
     */
    fun getBackupIntervalDays(): Int {
        return _autoBackupState.value.intervalDays
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
                _autoBackupState.update { it.copy(isEnabled = true, intervalDays = intervalDays) }
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
                _autoBackupState.update { it.copy(isEnabled = false) }
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

                if (_autoBackupState.value.isEnabled) { // Use state value
                    autoBackupScheduler.scheduleAutoBackup(intervalDays)
                }
                _autoBackupState.update { it.copy(intervalDays = intervalDays) }
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("备份频率已更新"))
            } catch (e: Exception) {
                Timber.e(e, "更新备份频率失败")
                _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("更新备份频率失败: ${e.localizedMessage}"))
            }
        }
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

                _autoBackupState.update { it.copy(
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

                _autoBackupState.update { it.copy(
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
                val uri = _autoBackupState.value.customBackupUri
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

                _autoBackupState.update { it.copy(
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
        viewModelScope.launch(Dispatchers.IO) {
            _autoBackupState.update { it.copy(isLoadingFiles = true) }
            try {
                val state = _autoBackupState.value
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

                _autoBackupState.update { it.copy(backupFiles = files, isLoadingFiles = false) }
            } catch (e: Exception) {
                 Timber.e(e, "加载备份文件列表失败")
                _autoBackupState.update { it.copy(isLoadingFiles = false) } // 确保加载状态被重置
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
     * 获取备份文件列表 (同步方法，可能不再需要，UI 应观察 StateFlow)
     */
    @Deprecated("UI should observe autoBackupState.backupFiles instead")
    fun getBackupFiles(): List<File> {
        return _autoBackupState.value.backupFiles
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
     * 创建手动备份
     */
    suspend fun createManualBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val state = _autoBackupState.value
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

            if (result.isSuccess) {
                // 更新文件列表
                withContext(Dispatchers.Main) { // Switch back for SharedFlow
                     _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Success("手动备份创建成功"))
                     loadBackupFiles() // Reload files after successful backup & cleanup
                }
                true
            } else {
                 withContext(Dispatchers.Main) {
                     _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("手动备份创建失败: ${result.exceptionOrNull()?.localizedMessage ?: "未知错误"}"))
                 }
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "创建手动备份时发生错误")
             withContext(Dispatchers.Main) {
                 _operationResult.emit(com.ccjizhang.ui.common.OperationResult.Error("创建手动备份失败: ${e.localizedMessage}"))
             }
            false
        }
    }

    /**
     * 清理旧备份文件，只保留最新的几个
     */
    private fun cleanupOldBackups(backupDir: File) {
        val backupFiles = backupDir.listFiles()?.filter { it.name.endsWith("_autobackup.json") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (backupFiles.size > MAX_BACKUP_FILES) { // 使用常量
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
     * 获取当前备份目录
     * @return 备份目录文件对象
     */
    fun getBackupDirectory(): File {
        val state = _autoBackupState.value
        val customUri = state.customBackupUri
        val customPath = state.customBackupPath

        return when {
            customPath.isNotEmpty() -> File(customPath)
            else -> File(context.filesDir, DEFAULT_BACKUP_FOLDER)
        }
    }

    /**
     * 打开备份文件并返回其内容
     * @param file 要打开的备份文件
     * @return 文件内容或错误信息
     */
    suspend fun openBackupFile(file: File): Result<String> = withContext(Dispatchers.IO) {
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
}