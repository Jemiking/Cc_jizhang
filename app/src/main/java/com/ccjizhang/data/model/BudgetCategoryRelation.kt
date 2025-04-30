package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 预算与分类的多对多关系实体
 * 一个预算可以包含多个分类，一个分类也可以属于多个预算
 */
@Entity(
    tableName = "budget_category_relations",
    primaryKeys = ["budgetId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = Budget::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("budgetId"),
        Index("categoryId")
    ]
)
data class BudgetCategoryRelation(
    val budgetId: Long,
    val categoryId: Long
) 