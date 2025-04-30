package com.ccjizhang.ui.viewmodels

/**
 * 家庭成员UI模型
 */
data class FamilyMemberUIModel(
    val id: Long,
    val name: String,
    val role: Int,
    val roleName: String,
    val status: Int,
    val statusName: String
)
