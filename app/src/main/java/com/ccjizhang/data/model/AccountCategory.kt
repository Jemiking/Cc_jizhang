package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账户分类实体类
 * 用于对账户进行分类管理
 *
 * @param id 分类ID
 * @param name 分类名称
 * @param icon 分类图标
 * @param color 分类颜色
 * @param sortOrder 排序顺序
 * @param isDefault 是否为默认分类
 */
@Entity(
    tableName = "account_categories",
    indices = [
        Index("sortOrder"),
        Index("isDefault")
    ]
)
data class AccountCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val icon: String = "",
    val color: Int = 0,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)
