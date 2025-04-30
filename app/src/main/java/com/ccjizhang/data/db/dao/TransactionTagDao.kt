package com.ccjizhang.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ccjizhang.data.model.TransactionTag
import kotlinx.coroutines.flow.Flow

/**
 * 交易标签关系数据访问对象
 */
@Dao
interface TransactionTagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TransactionTag)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TransactionTag>)
    
    @Delete
    suspend fun delete(tag: TransactionTag)
    
    @Query("DELETE FROM transaction_tags WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)
    
    @Query("DELETE FROM transaction_tags WHERE tag = :tag")
    suspend fun deleteByTag(tag: String)
    
    @Query("SELECT * FROM transaction_tags WHERE transactionId = :transactionId")
    fun getTagsByTransactionId(transactionId: Long): Flow<List<TransactionTag>>
    
    @Query("SELECT * FROM transaction_tags WHERE tag LIKE :tagPattern")
    fun getTransactionTagsByPattern(tagPattern: String): Flow<List<TransactionTag>>
    
    /**
     * 获取包含指定标签的所有交易ID
     */
    @Query("SELECT transactionId FROM transaction_tags WHERE tag = :tag")
    fun getTransactionIdsByTag(tag: String): Flow<List<Long>>
    
    /**
     * 获取所有标签
     */
    @Query("SELECT DISTINCT tag FROM transaction_tags ORDER BY tag")
    fun getAllDistinctTags(): Flow<List<String>>
} 