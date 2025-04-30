package com.ccjizhang.data.model

import androidx.room.Entity
import androidx.room.Index

/**
 * 交易与标签的多对多关系实体
 * 一个交易可以有多个标签
 * 注意：移除了外键约束以避免循环依赖，使用索引来维护关系
 */
@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tag"],
    indices = [
        Index("transactionId")
    ]
)
data class TransactionTag(
    val transactionId: Long,
    val tag: String
) 