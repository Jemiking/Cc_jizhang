package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类类型枚举
 */
enum class CategoryType {
    EXPENSE, // 支出类别
    INCOME   // 收入类别
}

/**
 * 分类实体类
 *
 * @param id 分类ID
 * @param name 分类名称
 * @param type 分类类型（支出/收入）
 * @param icon 图标名称
 * @param color 颜色值
 * @param isCustom 是否为用户自定义分类
 * @param sortOrder 排序优先级
 * @param parentId 父分类ID，为null表示顶级分类
 * @param level 分类层级，0表示顶级分类，1表示二级分类，依此类推
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("parentId"),
        Index("type"),
        Index("level"),
        Index("isCustom"),
        Index("sortOrder"),
        // 复合索引
        Index(value = ["type", "level"]),
        Index(value = ["parentId", "level"])
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val type: CategoryType = CategoryType.EXPENSE,
    val icon: String = "",
    val color: Int = 0,

    // 是否是用户自定义类别
    val isCustom: Boolean = false,

    // 排序优先级
    val sortOrder: Int = 0,

    // 父分类ID，为null表示顶级分类
    val parentId: Long? = null,

    // 分类层级，0表示顶级分类，1表示二级分类，依此类推
    val level: Int = 0,

    // 兼容CategoryViewModel中的简单数据模型
    val isIncome: Boolean = (type == CategoryType.INCOME)
)