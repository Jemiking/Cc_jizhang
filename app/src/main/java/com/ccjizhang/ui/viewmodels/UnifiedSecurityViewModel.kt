package com.ccjizhang.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.FamilyMemberRepository
import com.ccjizhang.utils.AccessControlHelper
import com.ccjizhang.utils.DatabaseEncryptionManager
import com.ccjizhang.utils.DatabaseMigrationHelper
import com.ccjizhang.utils.DatabaseSecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统一安全设置状态
 */
data class UnifiedSecurityState(
    // 基本状态
    val isLoading: Boolean = false,
    
    // 数据库加密状态
    val isEncryptionEnabled: Boolean = false,
    val encryptionType: String = "",
    val encryptionStrength: String = "",
    val lastPasswordChange: String = "",
    
    // 访问控制状态
    val isAccessControlEnabled: Boolean = false,
    val familyMembers: List<FamilyMemberUIModel> = emptyList(),
    
    // 安全报告
    val securityScore: Int = 0,
    val securityRecommendations: List<String> = emptyList(),
    
    // 对话框状态
    val showResetPasswordDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false
)

/**
 * 统一安全设置ViewModel
 * 整合了SecuritySettingsViewModel和EnhancedSecuritySettingsViewModel的功能
 */
@HiltViewModel
class UnifiedSecurityViewModel @Inject constructor(
    private val databaseSecurityManager: DatabaseSecurityManager,
    private val databaseEncryptionManager: DatabaseEncryptionManager,
    private val accessControlHelper: AccessControlHelper,
    private val familyMemberRepository: FamilyMemberRepository,
    private val databaseMigrationHelper: DatabaseMigrationHelper
) : ViewModel() {

    companion object {
        private const val TAG = "UnifiedSecurityVM"
    }

    // 安全状态
    private val _securityState = MutableStateFlow(UnifiedSecurityState())
    val securityState: StateFlow<UnifiedSecurityState> = _securityState.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult: SharedFlow<String> = _operationResult.asSharedFlow()

    // 初始化
    init {
        loadSecurityStatus()
        loadFamilyMembers()
    }

    /**
     * 加载安全状态
     */
    fun loadSecurityStatus() {
        viewModelScope.launch {
            try {
                _securityState.update { it.copy(isLoading = true) }
                
                // 获取安全报告
                val securityReport = databaseSecurityManager.getSecurityReport()
                
                // 提取加密状态
                val isEncryptionEnabled = securityReport["encryption_enabled"] as? Boolean ?: false
                val encryptionType = securityReport["encryption_type"] as? String ?: "未知"
                val encryptionStrength = securityReport["encryption_strength"] as? String ?: "未知"
                val lastPasswordChange = securityReport["last_password_change"] as? String ?: "未知"
                
                // 提取访问控制状态
                val isAccessControlEnabled = securityReport["access_control_enabled"] as? Boolean ?: false
                
                // 提取安全评分和建议
                val securityScore = securityReport["security_score"] as? Int ?: 0
                val recommendations = securityReport["recommendations"] as? List<String> ?: emptyList()
                
                _securityState.update { state ->
                    state.copy(
                        isLoading = false,
                        isEncryptionEnabled = isEncryptionEnabled,
                        encryptionType = encryptionType,
                        encryptionStrength = encryptionStrength,
                        lastPasswordChange = lastPasswordChange,
                        isAccessControlEnabled = isAccessControlEnabled,
                        securityScore = securityScore,
                        securityRecommendations = recommendations
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载安全状态失败", e)
                _securityState.update { it.copy(isLoading = false) }
                emitOperationResult("加载安全状态失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 加载家庭成员列表
     */
    fun loadFamilyMembers() {
        viewModelScope.launch {
            try {
                val members = accessControlHelper.getAllMembersWithRoles()
                val memberModels = members.map { member ->
                    FamilyMemberUIModel(
                        id = member.id,
                        name = member.name,
                        role = member.role,
                        roleName = getRoleName(member.role),
                        status = member.status,
                        statusName = getStatusName(member.status)
                    )
                }
                
                _securityState.update { it.copy(familyMembers = memberModels) }
            } catch (e: Exception) {
                Log.e(TAG, "加载家庭成员失败", e)
                emitOperationResult("加载家庭成员失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 设置数据库加密状态
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _securityState.update { it.copy(isLoading = true) }
                
                databaseSecurityManager.setEncryptionEnabled(enabled)
                
                _securityState.update { it.copy(
                    isLoading = false,
                    isEncryptionEnabled = enabled
                ) }
                
                loadSecurityStatus()
                emitOperationResult("数据库加密已${if (enabled) "启用" else "禁用"}")
            } catch (e: Exception) {
                Log.e(TAG, "设置数据库加密状态失败", e)
                _securityState.update { it.copy(isLoading = false) }
                emitOperationResult("设置数据库加密状态失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 设置访问控制状态
     */
    fun setAccessControlEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _securityState.update { it.copy(isLoading = true) }
                
                databaseSecurityManager.setAccessControlEnabled(enabled)
                
                _securityState.update { it.copy(
                    isLoading = false,
                    isAccessControlEnabled = enabled
                ) }
                
                loadSecurityStatus()
                emitOperationResult("访问控制已${if (enabled) "启用" else "禁用"}")
            } catch (e: Exception) {
                Log.e(TAG, "设置访问控制状态失败", e)
                _securityState.update { it.copy(isLoading = false) }
                emitOperationResult("设置访问控制状态失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 重置数据库密码
     */
    fun resetDatabasePassword() {
        viewModelScope.launch {
            try {
                _securityState.update { it.copy(isLoading = true) }
                
                // 获取当前密码
                val currentPassword = databaseEncryptionManager.getDatabasePassword()
                
                // 重置密码，生成新密码
                val newPassword = databaseEncryptionManager.resetDatabasePassword()
                
                // 使用旧密码重新加密数据库
                val result = databaseMigrationHelper.reencryptDatabase(currentPassword)
                
                if (result) {
                    loadSecurityStatus()
                    emitOperationResult("数据库密码已重置")
                } else {
                    emitOperationResult("重置密码失败")
                }
                
                _securityState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "重置数据库密码失败", e)
                _securityState.update { it.copy(isLoading = false) }
                emitOperationResult("重置数据库密码失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 更新家庭成员角色
     */
    fun updateMemberRole(memberId: Long, newRole: Int) {
        viewModelScope.launch {
            try {
                _securityState.update { it.copy(isLoading = true) }
                
                val member = familyMemberRepository.getFamilyMemberById(memberId)
                if (member != null) {
                    familyMemberRepository.updateMemberRole(memberId, newRole)
                    loadFamilyMembers()
                    emitOperationResult("成员角色已更新")
                } else {
                    emitOperationResult("找不到指定成员")
                }
                
                _securityState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "更新成员角色失败", e)
                _securityState.update { it.copy(isLoading = false) }
                emitOperationResult("更新成员角色失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 检查数据库是否已加密
     */
    fun isDatabaseEncrypted(): Boolean {
        return databaseMigrationHelper.isDatabaseEncrypted()
    }

    /**
     * 显示重置密码对话框
     */
    fun showResetPasswordDialog() {
        _securityState.update { it.copy(showResetPasswordDialog = true) }
    }

    /**
     * 隐藏重置密码对话框
     */
    fun hideResetPasswordDialog() {
        _securityState.update { it.copy(showResetPasswordDialog = false) }
    }

    /**
     * 显示修改密码对话框
     */
    fun showChangePasswordDialog() {
        _securityState.update { it.copy(showChangePasswordDialog = true) }
    }

    /**
     * 隐藏修改密码对话框
     */
    fun hideChangePasswordDialog() {
        _securityState.update { it.copy(showChangePasswordDialog = false) }
    }

    /**
     * 获取角色名称
     */
    private fun getRoleName(role: Int): String {
        return when (role) {
            AccessControlHelper.ROLE_OWNER -> "拥有者"
            AccessControlHelper.ROLE_ADMIN -> "管理员"
            AccessControlHelper.ROLE_EDITOR -> "编辑者"
            AccessControlHelper.ROLE_VIEWER -> "查看者"
            AccessControlHelper.ROLE_GUEST -> "访客"
            else -> "未知角色"
        }
    }

    /**
     * 获取状态名称
     */
    private fun getStatusName(status: Int): String {
        return when (status) {
            0 -> "活跃"
            1 -> "已邀请"
            2 -> "已离开"
            3 -> "已拒绝"
            else -> "未知状态"
        }
    }

    /**
     * 发送操作结果
     */
    private suspend fun emitOperationResult(message: String) {
        _operationResult.emit(message)
    }
}
