package com.ccjizhang.data.db.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionTag

/**
 * 交易与标签的关联查询结果
 */
data class TransactionWithTags(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val tags: List<TransactionTag>
) {
    /**
     * 获取纯文本标签列表
     */
    fun getTagStrings(): List<String> {
        return tags.map { it.tag }
    }
} 