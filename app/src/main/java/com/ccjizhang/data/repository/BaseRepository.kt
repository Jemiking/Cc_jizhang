package com.ccjizhang.data.repository

import android.util.Log
import com.ccjizhang.data.db.TransactionManager
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 基础仓库类
 * 提供通用的事务管理和异常处理功能
 */
abstract class BaseRepository(
    protected val transactionManager: TransactionManager,
    protected val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "BaseRepository"
    }
    
    /**
     * 在事务中执行数据库操作
     * @param operation 要执行的操作
     * @return 操作结果
     */
    protected suspend fun <T> executeInTransaction(operation: suspend () -> T): T {
        return transactionManager.executeInTransaction {
            try {
                operation()
            } catch (e: Exception) {
                Log.e(TAG, "事务操作失败", e)
                databaseExceptionHandler.handleDatabaseException(e, "事务操作")
                throw e
            }
        }
    }
    
    /**
     * 执行只读操作
     * @param operation 要执行的只读操作
     * @return 操作结果
     */
    protected suspend fun <T> executeReadOnly(operation: suspend () -> T): T {
        return transactionManager.executeReadOnly {
            try {
                operation()
            } catch (e: Exception) {
                Log.e(TAG, "只读操作失败", e)
                databaseExceptionHandler.handleDatabaseException(e, "只读操作")
                throw e
            }
        }
    }
    
    /**
     * 执行写入操作
     * @param operation 要执行的写入操作
     * @return 操作结果
     */
    protected suspend fun <T> executeWrite(operation: suspend () -> T): T {
        return transactionManager.executeWrite {
            try {
                operation()
            } catch (e: Exception) {
                Log.e(TAG, "写入操作失败", e)
                databaseExceptionHandler.handleDatabaseException(e, "写入操作")
                throw e
            }
        }
    }
    
    /**
     * 执行批量写入操作
     * @param operations 要执行的写入操作列表
     * @return 操作结果列表
     */
    protected suspend fun <T> executeBatchWrite(operations: List<suspend () -> T>): List<T> {
        return transactionManager.executeBatchWrite(operations)
    }
    
    /**
     * 包装 Flow 以添加异常处理和调度器
     * @param flowProvider 提供 Flow 的函数
     * @return 包装后的 Flow
     */
    protected fun <T> wrapFlow(flowProvider: () -> Flow<T>): Flow<T> {
        return flowProvider()
            .catch { e ->
                Log.e(TAG, "Flow 操作失败", e)
                if (e is Exception) {
                    databaseExceptionHandler.handleDatabaseException(e, "Flow 操作")
                }
                throw e
            }
            .flowOn(Dispatchers.IO)
    }
    
    /**
     * 执行数据库操作并返回 Flow
     * @param operation 要执行的操作
     * @return 操作结果的 Flow
     */
    protected fun <T> executeFlowOperation(operation: () -> Flow<T>): Flow<T> {
        return wrapFlow(operation)
    }
}
