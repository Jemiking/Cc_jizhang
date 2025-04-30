package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.WebDavConfig
import com.ccjizhang.utils.WebDavSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * WebDAV设置界面的ViewModel
 */
@HiltViewModel
class WebDavSettingsViewModel @Inject constructor(
    private val webDavSyncManager: WebDavSyncManager
) : ViewModel() {

    // WebDAV配置
    private val _webDavConfig = MutableStateFlow<WebDavConfig?>(null)
    val webDavConfig: StateFlow<WebDavConfig?> = _webDavConfig.asStateFlow()

    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    // 同步状态
    val syncStatus = webDavSyncManager.syncStatus

    init {
        loadWebDavConfig()
    }

    /**
     * 加载WebDAV配置
     */
    private fun loadWebDavConfig() {
        val config = webDavSyncManager.getWebDavConfig()
        _webDavConfig.value = config ?: WebDavConfig(
            serverUrl = "",
            username = "",
            password = "",
            syncFolder = "ccjizhang",
            autoSync = false,
            syncInterval = 24
        )
    }

    /**
     * 保存WebDAV配置
     */
    fun saveWebDavConfig(config: WebDavConfig) {
        viewModelScope.launch {
            try {
                webDavSyncManager.saveWebDavConfig(config)
                _webDavConfig.value = config
                _operationResult.emit(OperationResult.Success("WebDAV配置已保存"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("保存WebDAV配置失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 测试WebDAV连接
     */
    fun testConnection(config: WebDavConfig) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Testing
            try {
                val result = webDavSyncManager.testConnection(config)
                if (result) {
                    _connectionState.value = ConnectionState.Success
                    _operationResult.emit(OperationResult.Success("连接成功"))
                } else {
                    _connectionState.value = ConnectionState.Failed
                    _operationResult.emit(OperationResult.Error("连接失败，请检查服务器地址和凭据"))
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed
                _operationResult.emit(OperationResult.Error("连接测试出错: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 执行同步操作
     */
    fun sync() {
        viewModelScope.launch {
            try {
                val result = webDavSyncManager.sync()
                when (result) {
                    is WebDavSyncManager.SyncResult.Success -> {
                        _operationResult.emit(OperationResult.Success(result.message))
                    }
                    is WebDavSyncManager.SyncResult.Error -> {
                        _operationResult.emit(OperationResult.Error(result.message))
                    }
                }
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("同步失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 下载并恢复最新备份
     */
    fun downloadAndRestore() {
        viewModelScope.launch {
            try {
                // 下载最新备份
                val file = webDavSyncManager.downloadLatestBackup()
                if (file == null) {
                    _operationResult.emit(OperationResult.Error("未找到可用的备份文件"))
                    return@launch
                }
                
                // 恢复备份
                val result = webDavSyncManager.restoreFromFile(file)
                if (result) {
                    _operationResult.emit(OperationResult.Success("数据已成功恢复"))
                } else {
                    _operationResult.emit(OperationResult.Error("数据恢复失败"))
                }
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("恢复数据失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 删除WebDAV配置
     */
    fun deleteWebDavConfig() {
        viewModelScope.launch {
            try {
                webDavSyncManager.deleteWebDavConfig()
                loadWebDavConfig()
                _operationResult.emit(OperationResult.Success("WebDAV配置已删除"))
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("删除WebDAV配置失败: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * 连接状态
     */
    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Testing : ConnectionState()
        object Success : ConnectionState()
        object Failed : ConnectionState()
    }

    /**
     * 操作结果
     */
    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
} 