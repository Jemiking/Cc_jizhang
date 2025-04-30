package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.TransactionTagDao
import com.ccjizhang.data.model.TransactionTag
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

/**
 * 标签管理存储库
 * 负责所有与标签相关的数据操作
 */
@Singleton
class TagRepository @Inject constructor(
    private val transactionTagDao: TransactionTagDao
) {
    /**
     * 获取所有标签
     */
    fun getAllTags(): Flow<List<String>> {
        return transactionTagDao.getAllDistinctTags()
    }
    
    /**
     * 获取特定交易的所有标签
     */
    fun getTagsByTransactionId(transactionId: Long): Flow<List<String>> {
        return transactionTagDao.getTagsByTransactionId(transactionId)
            .map { tags -> tags.map { it.tag } }
    }
    
    /**
     * 搜索标签（模糊匹配）
     */
    fun searchTags(query: String): Flow<List<String>> {
        return transactionTagDao.getTransactionTagsByPattern("%$query%")
            .map { tags -> tags.map { it.tag }.distinct() }
    }
    
    /**
     * 添加标签到交易
     */
    suspend fun addTagToTransaction(transactionId: Long, tag: String) {
        transactionTagDao.insert(TransactionTag(transactionId, tag))
    }
    
    /**
     * 批量添加标签到交易
     */
    suspend fun addTagsToTransaction(transactionId: Long, tags: List<String>) {
        val transactionTags = tags.map { tag -> TransactionTag(transactionId, tag) }
        transactionTagDao.insertAll(transactionTags)
    }
    
    /**
     * 从交易中移除标签
     */
    suspend fun removeTagFromTransaction(transactionId: Long, tag: String) {
        transactionTagDao.delete(TransactionTag(transactionId, tag))
    }
    
    /**
     * 清除交易的所有标签
     */
    suspend fun clearTransactionTags(transactionId: Long) {
        transactionTagDao.deleteByTransactionId(transactionId)
    }
    
    /**
     * 替换交易的所有标签
     */
    suspend fun replaceTransactionTags(transactionId: Long, newTags: List<String>) {
        // 删除原有标签
        transactionTagDao.deleteByTransactionId(transactionId)
        
        // 添加新标签
        if (newTags.isNotEmpty()) {
            val transactionTags = newTags.map { tag -> TransactionTag(transactionId, tag) }
            transactionTagDao.insertAll(transactionTags)
        }
    }
    
    /**
     * 删除标签（从所有交易中移除）
     */
    suspend fun deleteTag(tag: String) {
        transactionTagDao.deleteByTag(tag)
    }
    
    /**
     * 重命名标签
     */
    suspend fun renameTag(oldTag: String, newTag: String) {
        // 获取所有使用该标签的交易ID
        val transactionIds = transactionTagDao.getTransactionIdsByTag(oldTag).first()
        
        // 添加新标签
        for (transactionId in transactionIds) {
            transactionTagDao.insert(TransactionTag(transactionId, newTag))
        }
        
        // 删除旧标签
        transactionTagDao.deleteByTag(oldTag)
    }
} 