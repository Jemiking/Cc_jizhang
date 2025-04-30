package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 家庭成员实体类
 * 用于家庭共享记账功能
 */
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 成员名称
    val name: String,
    
    // 成员头像URI
    val avatarUri: String? = null,
    
    // 成员角色：拥有者(0)、管理员(1)、编辑者(2)、查看者(3)
    val role: Int = 2,
    
    // 成员电子邮箱（用于邀请和登录）
    val email: String = "",
    
    // 成员手机号
    val phone: String = "",
    
    // 成员唯一标识符（用于云端同步）
    val uniqueId: String? = null,
    
    // 成员状态：已接受(0)、待接受邀请(1)、已拒绝(2)、已离开(3)
    val status: Int = 0,
    
    // 最后活跃时间
    val lastActiveTime: Date? = null,
    
    // 备注
    val note: String? = null,
    
    // 创建时间
    val createdAt: Date = Date(),
    
    // 更新时间
    val updatedAt: Date = Date(),
    
    // 是否为账本拥有者
    val isOwner: Boolean = false,
    
    // 是否可以编辑交易
    val canEditTransactions: Boolean = false,
    
    // 是否可以查看所有交易
    val canViewAllTransactions: Boolean = true,
    
    // 消费限额
    val spendingLimit: Double = 0.0
) 