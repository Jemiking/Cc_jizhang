package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ccjizhang.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 家庭成员数据访问对象
 */
@Dao
interface FamilyMemberDao {
    
    /**
     * 插入新的家庭成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(familyMember: FamilyMember): Long
    
    /**
     * 批量插入家庭成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(familyMembers: List<FamilyMember>): List<Long>
    
    /**
     * 更新家庭成员
     */
    @Update
    suspend fun update(familyMember: FamilyMember)
    
    /**
     * 删除家庭成员
     */
    @Delete
    suspend fun delete(familyMember: FamilyMember)
    
    /**
     * 根据ID获取家庭成员
     */
    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: Long): FamilyMember?
    
    /**
     * 获取所有家庭成员
     */
    @Query("SELECT * FROM family_members ORDER BY role ASC, name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMember>>
    
    /**
     * 获取活跃的家庭成员（已接受邀请的）
     */
    @Query("SELECT * FROM family_members WHERE status = 0 ORDER BY role ASC, name ASC")
    fun getActiveFamilyMembers(): Flow<List<FamilyMember>>
    
    /**
     * 获取待接受邀请的家庭成员
     */
    @Query("SELECT * FROM family_members WHERE status = 1 ORDER BY createdAt DESC")
    fun getPendingFamilyMembers(): Flow<List<FamilyMember>>
    
    /**
     * 按角色获取家庭成员
     */
    @Query("SELECT * FROM family_members WHERE role = :role AND status = 0 ORDER BY name ASC")
    fun getFamilyMembersByRole(role: Int): Flow<List<FamilyMember>>
    
    /**
     * 搜索家庭成员
     */
    @Query("""
        SELECT * FROM family_members 
        WHERE (name LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
        ORDER BY role ASC, name ASC
    """)
    fun searchFamilyMembers(query: String): Flow<List<FamilyMember>>
    
    /**
     * 根据唯一标识符获取家庭成员
     */
    @Query("SELECT * FROM family_members WHERE uniqueId = :uniqueId")
    suspend fun getByUniqueId(uniqueId: String): FamilyMember?
    
    /**
     * 根据电子邮箱获取家庭成员
     */
    @Query("SELECT * FROM family_members WHERE email = :email")
    suspend fun getByEmail(email: String): FamilyMember?
    
    /**
     * 更新家庭成员状态
     */
    @Query("UPDATE family_members SET status = :status, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, updateTime: Date = Date())
    
    /**
     * 更新家庭成员角色
     */
    @Query("UPDATE family_members SET role = :role, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateRole(id: Long, role: Int, updateTime: Date = Date())
    
    /**
     * 更新家庭成员最后活跃时间
     */
    @Query("UPDATE family_members SET lastActiveTime = :lastActiveTime, updatedAt = :lastActiveTime WHERE id = :id")
    suspend fun updateLastActiveTime(id: Long, lastActiveTime: Date = Date())
    
    /**
     * 删除所有已拒绝或已离开的成员
     */
    @Query("DELETE FROM family_members WHERE status IN (2, 3)")
    suspend fun deleteInactiveMembers()
} 