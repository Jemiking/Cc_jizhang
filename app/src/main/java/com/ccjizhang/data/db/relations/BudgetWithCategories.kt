package com.ccjizhang.data.db.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.model.BudgetCategoryRelation
import com.ccjizhang.data.model.Category

/**
 * 预算与分类的关联查询结果
 * 使用Room提供的关系映射
 */
data class BudgetWithCategories(
    @Embedded val budget: Budget,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BudgetCategoryRelation::class,
            parentColumn = "budgetId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<Category>
) 