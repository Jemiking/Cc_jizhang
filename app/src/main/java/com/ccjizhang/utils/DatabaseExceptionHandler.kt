package com.ccjizhang.utils

import android.database.sqlite.SQLiteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库异常处理器
 * 用于捕获和处理数据库操作中的异常
 */
@Singleton
class DatabaseExceptionHandler @Inject constructor(
    private val databaseRepairTool: DatabaseRepairTool
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "DatabaseExceptionHandler"

    /**
     * 处理数据库异常
     * @param exception 捕获到的异常
     * @param operation 操作名称，用于日志记录
     * @return 是否成功处理异常
     */
    fun handleDatabaseException(exception: Exception, operation: String): Boolean {
        Log.e(TAG, "数据库操作异常: $operation", exception)

        // 检查是否需要修复数据库
        val needsRepair = when {
            // 检查是否是数据库I/O错误
            exception is SQLiteException && (
                exception.message?.contains("disk I/O error") == true ||
                exception.message?.contains("SQLITE_IOERR") == true ||
                exception.message?.contains("no such table") == true ||
                exception.message?.contains("database disk image is malformed") == true ||
                exception.message?.contains("database is locked") == true ||
                exception.message?.contains("database or disk is full") == true ||
                exception.message?.contains("unable to open database file") == true ||
                exception.message?.contains("attempt to write a readonly database") == true ||
                exception.message?.contains("SQLITE_CORRUPT") == true ||
                exception.message?.contains("SQLITE_CANTOPEN") == true ||
                exception.message?.contains("SQLITE_READONLY") == true
            ) -> {
                Log.w(TAG, "检测到SQLite数据库错误，需要修复")
                true
            }

            // 检查是否是SQLCipher相关错误
            exception.message?.contains("file is not a database") == true ||
            exception.message?.contains("file is encrypted") == true ||
            exception.message?.contains("invalid password") == true ||
            exception.message?.contains("SQLITE_NOTADB") == true ||
            exception.message?.contains("PRAGMA journal_mode=PERSIST") == true -> {
                Log.w(TAG, "检测到SQLCipher加密相关错误，需要修复")
                true
            }

            // 检查是否是文件IO错误
            exception is IOException ||
            exception.message?.contains("I/O error") == true ||
            exception.message?.contains("Permission denied") == true ||
            exception.message?.contains("failed to open") == true -> {
                Log.w(TAG, "检测到文件I/O错误，需要修复")
                true
            }

            // 其他严重错误
            exception is OutOfMemoryError ||
            exception.message?.contains("database corruption") == true ||
            exception.message?.contains("statement aborts") == true -> {
                Log.w(TAG, "检测到严重错误，需要修复")
                true
            }

            // 默认不需要修复
            else -> false
        }

        if (needsRepair) {
            // 在后台线程中修复数据库
            scope.launch {
                try {
                    Log.i(TAG, "开始修复数据库，错误原因: ${exception.message}")

                    // 先等待一小段时间，确保其他操作完成
                    kotlinx.coroutines.delay(500)

                    val repaired = databaseRepairTool.forceRepairDatabase()
                    if (repaired) {
                        Log.i(TAG, "数据库修复成功")
                    } else {
                        Log.e(TAG, "数据库修复失败")
                        // 如果修复失败，再尝试一次
                        kotlinx.coroutines.delay(1000)
                        val secondAttempt = databaseRepairTool.forceRepairDatabase()
                        if (secondAttempt) {
                            Log.i(TAG, "数据库二次修复成功")
                        } else {
                            Log.e(TAG, "数据库二次修复也失败")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "数据库修复过程中发生错误", e)
                }
            }
            return true
        }

        return false
    }

    /**
     * 包装数据库操作，自动处理异常
     * @param operation 操作名称，用于日志记录
     * @param block 要执行的数据库操作
     * @return 操作结果，如果发生异常则返回null
     */
    suspend fun <T> withDatabaseExceptionHandling(operation: String, block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            handleDatabaseException(e, operation)
            null
        }
    }
}
