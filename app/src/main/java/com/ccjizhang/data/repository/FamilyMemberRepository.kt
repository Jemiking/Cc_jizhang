package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.FamilyMemberDao
import com.ccjizhang.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 家庭成员数据仓库
 * 负责家庭共享记账功能的数据管理
 */
@Singleton
class FamilyMemberRepository @Inject constructor(
    private val familyMemberDao: FamilyMemberDao
) {
    
    /**
     * 获取所有家庭成员
     */
    fun getAllFamilyMembers(): Flow<List<FamilyMember>> {
        return familyMemberDao.getAllFamilyMembers()
    }
    
    /**
     * 获取活跃的家庭成员（已接受邀请的）
     */
    fun getActiveFamilyMembers(): Flow<List<FamilyMember>> {
        return familyMemberDao.getActiveFamilyMembers()
    }
    
    /**
     * 获取待接受邀请的家庭成员
     */
    fun getPendingFamilyMembers(): Flow<List<FamilyMember>> {
        return familyMemberDao.getPendingFamilyMembers()
    }
    
    /**
     * 按角色获取家庭成员
     */
    fun getFamilyMembersByRole(role: Int): Flow<List<FamilyMember>> {
        return familyMemberDao.getFamilyMembersByRole(role)
    }
    
    /**
     * 搜索家庭成员
     */
    fun searchFamilyMembers(query: String): Flow<List<FamilyMember>> {
        return familyMemberDao.searchFamilyMembers(query)
    }
    
    /**
     * 根据ID获取家庭成员
     */
    suspend fun getFamilyMemberById(id: Long): FamilyMember? {
        return familyMemberDao.getById(id)
    }
    
    /**
     * 根据唯一标识符获取家庭成员
     */
    suspend fun getFamilyMemberByUniqueId(uniqueId: String): FamilyMember? {
        return familyMemberDao.getByUniqueId(uniqueId)
    }
    
    /**
     * 根据电子邮箱获取家庭成员
     */
    suspend fun getFamilyMemberByEmail(email: String): FamilyMember? {
        return familyMemberDao.getByEmail(email)
    }
    
    /**
     * 添加新的家庭成员
     */
    suspend fun addFamilyMember(familyMember: FamilyMember): Long {
        return familyMemberDao.insert(familyMember)
    }
    
    /**
     * 批量添加家庭成员
     */
    suspend fun addFamilyMembers(familyMembers: List<FamilyMember>): List<Long> {
        return familyMemberDao.insertAll(familyMembers)
    }
    
    /**
     * 更新家庭成员
     */
    suspend fun updateFamilyMember(familyMember: FamilyMember) {
        familyMemberDao.update(familyMember)
    }
    
    /**
     * 删除家庭成员
     */
    suspend fun deleteFamilyMember(familyMember: FamilyMember) {
        familyMemberDao.delete(familyMember)
    }
    
    /**
     * 更新家庭成员状态
     */
    suspend fun updateMemberStatus(id: Long, status: Int) {
        familyMemberDao.updateStatus(id, status)
    }
    
    /**
     * 更新家庭成员角色
     */
    suspend fun updateMemberRole(id: Long, role: Int) {
        familyMemberDao.updateRole(id, role)
    }
    
    /**
     * 更新家庭成员最后活跃时间
     */
    suspend fun updateMemberLastActiveTime(id: Long) {
        familyMemberDao.updateLastActiveTime(id, Date())
    }
    
    /**
     * 清理已失效的成员（已拒绝或已离开）
     */
    suspend fun cleanupInactiveMembers() {
        familyMemberDao.deleteInactiveMembers()
    }
    
    /**
     * 判断是否为家庭账本拥有者
     */
    suspend fun isOwner(id: Long): Boolean {
        val member = familyMemberDao.getById(id) ?: return false
        return member.role == 0 // 0表示拥有者
    }
    
    /**
     * 判断是否有编辑权限（拥有者、管理员和编辑者）
     */
    suspend fun hasEditPermission(id: Long): Boolean {
        val member = familyMemberDao.getById(id) ?: return false
        return member.role <= 2 // 0:拥有者, 1:管理员, 2:编辑者
    }
    
    /**
     * 判断是否存在家庭成员
     */
    suspend fun hasAnyFamilyMembers(): Boolean {
        val members = familyMemberDao.getAllFamilyMembers().first()
        return members.isNotEmpty()
    }
    
    /**
     * 创建邀请链接（示例实现）
     */
    fun createInvitationLink(email: String, role: Int): String {
        // 实际应用中，这里应该生成一个带加密信息的真实邀请链接
        return "https://ccjizhang.app/invite?email=$email&role=$role&token=${generateInviteToken(email, role)}"
    }
    
    /**
     * 生成邀请令牌（示例实现）
     */
    private fun generateInviteToken(email: String, role: Int): String {
        // 实际应用中，这里应该生成一个加密的令牌
        return "token_${email.hashCode()}_${role}_${System.currentTimeMillis()}"
    }
} 