package com.ccjizhang.ui.viewmodels

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.FamilyMemberRepository
import com.ccjizhang.utils.AccessControlHelper
import com.ccjizhang.utils.DatabaseMigrationHelper
import com.ccjizhang.utils.DatabaseSecurityManager
import com.ccjizhang.ui.viewmodels.FamilyMemberUIModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 增强的安全设置视图模型
 * 提供数据库安全和访问控制相关功能
 */
@HiltViewModel
class EnhancedSecuritySettingsViewModel @Inject constructor(
    private val databaseSecurityManager: DatabaseSecurityManager,
    private val accessControlHelper: AccessControlHelper,
    private val familyMemberRepository: FamilyMemberRepository,
    private val databaseMigrationHelper: DatabaseMigrationHelper
) : ViewModel() {

    companion object {
        private const val TAG = "SecuritySettingsVM"
    }

    // 安全状态
    private val _securityStatus = MutableStateFlow<Map<String, Any>>(emptyMap())
    val securityStatus: StateFlow<Map<String, Any>> = _securityStatus

    // 家庭成员列表
    private val _familyMembers = MutableStateFlow<List<FamilyMemberUIModel>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMemberUIModel>> = _familyMembers

    // 操作结果
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult: SharedFlow<String> = _operationResult

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
                val status = databaseSecurityManager.getSecurityReport()
                _securityStatus.value = status
            } catch (e: Exception) {
                Log.e(TAG, "加载安全状态失败", e)
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
                _familyMembers.value = members.map { member ->
                    FamilyMemberUIModel(
                        id = member.id,
                        name = member.name,
                        role = member.role,
                        roleName = getRoleName(member.role),
                        status = member.status,
                        statusName = getStatusName(member.status)
                    )
                }
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
                databaseSecurityManager.setEncryptionEnabled(enabled)
                loadSecurityStatus()
                emitOperationResult("数据库加密已${if (enabled) "启用" else "禁用"}")
            } catch (e: Exception) {
                Log.e(TAG, "设置数据库加密状态失败", e)
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
                databaseSecurityManager.setAccessControlEnabled(enabled)
                loadSecurityStatus()
                emitOperationResult("访问控制已${if (enabled) "启用" else "禁用"}")
            } catch (e: Exception) {
                Log.e(TAG, "设置访问控制状态失败", e)
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
                // 获取当前密码
                val currentPassword = databaseSecurityManager.getDatabasePassword()

                // 重置密码，生成新密码
                val newPassword = databaseSecurityManager.resetDatabasePassword()

                // 使用旧密码重新加密数据库
                val result = databaseMigrationHelper.reencryptDatabase(currentPassword)

                if (result) {
                    loadSecurityStatus()
                    emitOperationResult("数据库密码已重置")
                } else {
                    emitOperationResult("重置密码失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "重置数据库密码失败", e)
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
                val member = familyMemberRepository.getFamilyMemberById(memberId)
                if (member != null) {
                    familyMemberRepository.updateMemberRole(memberId, newRole)
                    loadFamilyMembers()
                    emitOperationResult("成员角色已更新")
                } else {
                    emitOperationResult("找不到指定成员")
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新成员角色失败", e)
                emitOperationResult("更新成员角色失败: ${e.localizedMessage}")
            }
        }
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

// FamilyMemberUIModel 已移至单独的文件
