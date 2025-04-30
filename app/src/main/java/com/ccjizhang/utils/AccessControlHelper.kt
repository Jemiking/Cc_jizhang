package com.ccjizhang.utils

import android.util.Log
import com.ccjizhang.data.db.dao.FamilyMemberDao
import com.ccjizhang.data.model.FamilyMember
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 访问控制助手类
 * 用于实现细粒度的数据访问控制
 */
@Singleton
class AccessControlHelper @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val databaseSecurityManager: DatabaseSecurityManager
) {
    companion object {
        private const val TAG = "AccessControlHelper"

        // 权限级别常量
        const val PERMISSION_NONE = 0
        const val PERMISSION_READ = 1
        const val PERMISSION_WRITE = 2
        const val PERMISSION_ADMIN = 3

        // 角色常量
        const val ROLE_OWNER = 0
        const val ROLE_ADMIN = 1
        const val ROLE_EDITOR = 2
        const val ROLE_VIEWER = 3
        const val ROLE_GUEST = 4
    }

    /**
     * 检查用户是否有权限访问指定资源
     * @param userId 用户ID
     * @param resourceType 资源类型（如"transaction", "account"等）
     * @param resourceId 资源ID
     * @param requiredPermission 所需权限级别
     * @return 是否有权限
     */
    suspend fun hasPermission(
        userId: Long,
        resourceType: String,
        resourceId: Long,
        requiredPermission: Int
    ): Boolean {
        // 如果访问控制未启用，直接返回true
        if (!databaseSecurityManager.isAccessControlEnabled()) {
            return true
        }

        try {
            // 获取用户角色
            val member = familyMemberDao.getById(userId) ?: return false
            val role = member.role

            // 根据角色和资源类型判断权限
            return when (role) {
                ROLE_OWNER -> true // 拥有者拥有所有权限
                ROLE_ADMIN -> requiredPermission <= PERMISSION_ADMIN // 管理员拥有管理权限
                ROLE_EDITOR -> {
                    // 编辑者拥有写入权限，但对某些敏感资源可能有限制
                    if (resourceType == "account" || resourceType == "budget") {
                        requiredPermission <= PERMISSION_READ // 只能读取账户和预算
                    } else {
                        requiredPermission <= PERMISSION_WRITE // 可以编辑其他资源
                    }
                }
                ROLE_VIEWER -> requiredPermission <= PERMISSION_READ // 查看者只有读取权限
                ROLE_GUEST -> {
                    // 访客只能访问特定资源
                    if (resourceType == "transaction") {
                        // 检查交易是否属于该用户
                        isResourceOwnedByUser(userId, resourceType, resourceId) &&
                            requiredPermission <= PERMISSION_READ
                    } else {
                        false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查权限失败", e)
            return false
        }
    }

    /**
     * 检查资源是否属于指定用户
     */
    private suspend fun isResourceOwnedByUser(
        userId: Long,
        resourceType: String,
        resourceId: Long
    ): Boolean {
        // 根据资源类型查询资源所有者
        // 这里需要根据实际数据模型实现
        // 例如，对于交易记录，可以检查创建者ID

        // 简化实现，假设所有资源都属于用户
        return true
    }

    /**
     * 获取用户角色
     * @param userId 用户ID
     * @return 用户角色，如果用户不存在则返回ROLE_GUEST
     */
    suspend fun getUserRole(userId: Long): Int {
        try {
            val member = familyMemberDao.getById(userId) ?: return ROLE_GUEST
            return member.role
        } catch (e: Exception) {
            Log.e(TAG, "获取用户角色失败", e)
            return ROLE_GUEST
        }
    }

    /**
     * 检查用户是否为拥有者
     */
    suspend fun isOwner(userId: Long): Boolean {
        return getUserRole(userId) == ROLE_OWNER
    }

    /**
     * 检查用户是否为管理员
     */
    suspend fun isAdmin(userId: Long): Boolean {
        val role = getUserRole(userId)
        return role == ROLE_OWNER || role == ROLE_ADMIN
    }

    /**
     * 检查用户是否有编辑权限
     */
    suspend fun hasEditPermission(userId: Long): Boolean {
        val role = getUserRole(userId)
        return role <= ROLE_EDITOR
    }

    /**
     * 获取所有家庭成员及其角色
     */
    suspend fun getAllMembersWithRoles(): List<FamilyMember> {
        return try {
            familyMemberDao.getAllFamilyMembers().first()
        } catch (e: Exception) {
            Log.e(TAG, "获取家庭成员失败", e)
            emptyList()
        }
    }

    /**
     * 获取当前用户ID
     * 注意：这里简化实现，返回固定值。实际应用中应该从用户会话或偏好设置中获取
     * @return 当前用户ID
     */
    fun getCurrentUserId(): Long {
        return try {
            // 这里简化实现，返回固定值
            // 实际应用中应该从用户会话或偏好设置中获取
            databaseSecurityManager.getCurrentUserId()
        } catch (e: Exception) {
            Log.e(TAG, "获取当前用户ID失败", e)
            1L // 默认返回用户ID为1
        }
    }

    /**
     * 检查当前用户是否为管理员
     * @return 是否为管理员
     */
    fun isCurrentUserAdmin(): Boolean {
        return try {
            // 这里简化实现，假设当前用户是管理员
            // 实际应用中应该根据当前用户角色判断
            databaseSecurityManager.isCurrentUserAdmin()
        } catch (e: Exception) {
            Log.e(TAG, "检查当前用户是否为管理员失败", e)
            true // 默认返回管理员权限
        }
    }
}
