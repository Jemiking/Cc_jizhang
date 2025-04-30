package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.utils.DatabaseEncryptionManager
import com.ccjizhang.utils.DatabaseMigrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 安全设置ViewModel
 */
@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val encryptionManager: DatabaseEncryptionManager,
    private val databaseMigrationHelper: DatabaseMigrationHelper
) : ViewModel() {
    
    // 操作结果
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult: SharedFlow<String> = _operationResult
    
    /**
     * 重置数据库密码
     */
    suspend fun resetDatabasePassword(): Boolean {
        try {
            // 获取当前密码
            val currentPassword = encryptionManager.getDatabasePassword()
            
            // 重置密码，生成新密码
            val newPassword = encryptionManager.resetDatabasePassword()
            
            // 使用旧密码重新加密数据库
            val result = databaseMigrationHelper.reencryptDatabase(currentPassword)
            
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _operationResult.emit("重置密码失败: ${e.localizedMessage}")
            }
            return false
        }
    }
    
    /**
     * 检查数据库是否已加密
     */
    fun isDatabaseEncrypted(): Boolean {
        return databaseMigrationHelper.isDatabaseEncrypted()
    }
} 