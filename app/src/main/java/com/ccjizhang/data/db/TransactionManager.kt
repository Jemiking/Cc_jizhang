package com.ccjizhang.data.db

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 事务管理器
 * 负责管理数据库事务的创建、提交和回滚，优化事务粒度和性能
 */
@Singleton
class TransactionManager @Inject constructor(
    private val databaseConnectionManager: DatabaseConnectionManager,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "TransactionManager"

        // 事务嵌套级别的线程本地变量
        private val transactionNestingLevel = ThreadLocal.withInitial { AtomicInteger(0) }

        // 事务是否成功的线程本地变量
        private val transactionSuccess = ThreadLocal.withInitial { false }
    }

    /**
     * 在事务中执行数据库操作
     * 自动处理事务的开始、提交和回滚
     * @param operation 要在事务中执行的操作
     * @return 操作的结果
     */
    suspend fun <T> executeInTransaction(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        // 获取当前线程的事务嵌套级别
        val nestingLevel = transactionNestingLevel.get()
        val isOutermostTransaction = nestingLevel.get() == 0

        // 增加事务嵌套级别
        nestingLevel.incrementAndGet()

        // 如果是最外层事务，开始新事务
        val connection = if (isOutermostTransaction) {
            val conn = databaseConnectionManager.getConnection()
            try {
                // 开始事务
                conn.beginTransaction()
                Log.d(TAG, "开始新事务")

                // 重置事务成功标志
                transactionSuccess.set(false)

                conn
            } catch (e: Exception) {
                // 如果开始事务失败，释放连接并抛出异常
                databaseConnectionManager.releaseConnection(conn)
                Log.e(TAG, "开始事务失败", e)
                databaseExceptionHandler.handleDatabaseException(e, "开始事务")
                throw e
            }
        } else {
            // 如果是嵌套事务，不需要获取新连接
            Log.d(TAG, "嵌套事务级别: ${nestingLevel.get()}")
            null
        }

        try {
            // 执行操作
            val result = operation()

            // 如果是最外层事务，标记事务成功
            if (isOutermostTransaction) {
                transactionSuccess.set(true)
            }

            return@withContext result
        } catch (e: Exception) {
            // 如果操作失败，记录错误并重新抛出
            Log.e(TAG, "事务操作失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "事务操作")
            throw e
        } finally {
            // 减少事务嵌套级别
            nestingLevel.decrementAndGet()

            // 如果是最外层事务，结束事务
            if (isOutermostTransaction && connection != null) {
                try {
                    if (transactionSuccess.get()) {
                        // 如果事务成功，提交事务
                        connection.setTransactionSuccessful()
                        Log.d(TAG, "事务成功，提交事务")

                        // 强制执行同步，确保数据写入磁盘
                        try {
                            connection.query("PRAGMA wal_checkpoint(FULL)", emptyArray()).close()
                            Log.d(TAG, "执行检查点操作，确保数据持久化")
                        } catch (e: Exception) {
                            Log.w(TAG, "执行检查点操作失败", e)
                            // 即使检查点操作失败，也继续提交事务
                        }
                    } else {
                        Log.d(TAG, "事务失败，回滚事务")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "设置事务成功状态失败", e)
                    databaseExceptionHandler.handleDatabaseException(e, "设置事务成功状态")
                } finally {
                    try {
                        // 结束事务（提交或回滚）
                        connection.endTransaction()
                        Log.d(TAG, "结束事务")
                    } catch (e: Exception) {
                        Log.e(TAG, "结束事务失败", e)
                        databaseExceptionHandler.handleDatabaseException(e, "结束事务")
                    } finally {
                        // 释放连接
                        databaseConnectionManager.releaseConnection(connection)
                    }
                }
            }
        }
    }

    /**
     * 执行批量操作
     * 在单一事务中执行多个操作，提高性能
     * @param operations 要执行的操作列表
     * @return 操作结果列表
     */
    suspend fun <T> executeBatch(operations: List<suspend () -> T>): List<T> = withContext(Dispatchers.IO) {
        executeInTransaction {
            val results = mutableListOf<T>()

            for (operation in operations) {
                try {
                    val result = operation()
                    results.add(result)
                } catch (e: Exception) {
                    Log.e(TAG, "批量操作中的单个操作失败", e)
                    databaseExceptionHandler.handleDatabaseException(e, "批量操作")
                    throw e // 抛出异常以回滚整个事务
                }
            }

            results
        }
    }

    /**
     * 执行只读操作
     * 不开启事务，适用于查询操作
     * @param operation 要执行的只读操作
     * @return 操作的结果
     */
    suspend fun <T> executeReadOnly(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        val connection = databaseConnectionManager.getConnection()

        try {
            operation()
        } catch (e: Exception) {
            Log.e(TAG, "只读操作失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "只读操作")
            throw e
        } finally {
            databaseConnectionManager.releaseConnection(connection)
        }
    }

    /**
     * 执行写入操作
     * 开启事务，适用于插入、更新、删除操作
     * @param operation 要执行的写入操作
     * @return 操作的结果
     */
    suspend fun <T> executeWrite(operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        executeInTransaction {
            operation()
        }
    }

    /**
     * 执行批量写入操作
     * 在单一事务中执行多个写入操作，提高性能
     * @param operations 要执行的写入操作列表
     * @return 操作结果列表
     */
    suspend fun <T> executeBatchWrite(operations: List<suspend () -> T>): List<T> = withContext(Dispatchers.IO) {
        executeBatch(operations)
    }

    /**
     * 设置当前事务为成功
     * 只有在事务中调用才有效
     */
    fun setTransactionSuccessful() {
        if (transactionNestingLevel.get().get() > 0) {
            transactionSuccess.set(true)
            Log.d(TAG, "设置事务成功标志")
        } else {
            Log.w(TAG, "尝试在事务外部设置事务成功标志")
        }
    }

    /**
     * 检查是否在事务中
     * @return 是否在事务中
     */
    fun isInTransaction(): Boolean {
        return transactionNestingLevel.get().get() > 0
    }

    /**
     * 获取当前事务嵌套级别
     * @return 事务嵌套级别
     */
    fun getTransactionNestingLevel(): Int {
        return transactionNestingLevel.get().get()
    }
}
