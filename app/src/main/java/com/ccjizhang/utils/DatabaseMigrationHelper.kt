package com.ccjizhang.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import net.sqlcipher.DatabaseErrorHandler
import net.sqlcipher.database.SQLiteException

// Define the named hook class at the top level
private class MyDatabaseHook : SQLiteDatabaseHook {
    override fun preKey(database: SQLiteDatabase?) {
        // Implementation if needed, e.g., Log.i("MyDatabaseHook", "preKey called")
    }
    override fun postKey(database: SQLiteDatabase?) {
        // Implementation if needed, e.g., Log.i("MyDatabaseHook", "postKey called")
    }
}

// Define the named error handler class at the top level
private class MyDatabaseErrorHandler : DatabaseErrorHandler {
    override fun onCorruption(dbObj: SQLiteDatabase?) {
        // Handle corruption, e.g., Log.w("MyDatabaseErrorHandler", "Database corrupted!")
    }
}

/**
 * 数据库迁移助手
 * 用于处理数据库重加密等高级迁移操作
 */
@Singleton
class DatabaseMigrationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: DatabaseEncryptionManager
) {

    companion object {
        private const val DATABASE_NAME = "ccjizhang_database"
        private const val TEMP_DATABASE_NAME = "ccjizhang_database_temp"
    }

    /**
     * 重新加密数据库
     * 当密码更改或需要从非加密迁移到加密数据库时使用
     */
    suspend fun reencryptDatabase(oldPassword: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            // 数据库文件
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            // 如果数据库不存在，不需要重加密
            if (!databaseFile.exists()) {
                return@withContext true
            }

            // 临时数据库文件
            val tempDatabaseFile = context.getDatabasePath(TEMP_DATABASE_NAME)
            if (tempDatabaseFile.exists()) {
                tempDatabaseFile.delete()
            }

            // 新密码
            val newPassword = encryptionManager.getDatabasePassword()

            // 打开源数据库
            val sourceDb = if (oldPassword != null) {
                // 已加密数据库，使用旧密码
                net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    databaseFile.path,
                    oldPassword,
                    null,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE
                )
            } else {
                // 未加密数据库
                net.sqlcipher.database.SQLiteDatabase.openOrCreateDatabase(
                    databaseFile.path,
                    "",
                    null,
                    object : SQLiteDatabaseHook {
                        override fun preKey(database: SQLiteDatabase) {}
                        override fun postKey(database: SQLiteDatabase) {}
                    },
                    null
                )
            }

            // 创建新加密数据库
            val destinationDb = net.sqlcipher.database.SQLiteDatabase.openOrCreateDatabase(
                tempDatabaseFile.path,
                newPassword,
                null,
                object : SQLiteDatabaseHook {
                    override fun preKey(database: SQLiteDatabase) {}
                    override fun postKey(database: SQLiteDatabase) {}
                },
                null
            )

            // 附加源数据库到目标数据库
            sourceDb.rawExecSQL("ATTACH DATABASE '${tempDatabaseFile.path}' AS encrypted KEY '${newPassword}';")

            // 获取表列表
            val cursor = sourceDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%';", null)
            val tables = mutableListOf<String>()

            cursor.use {
                while (it.moveToNext()) {
                    tables.add(it.getString(0))
                }
            }

            // 复制每个表
            for (table in tables) {
                sourceDb.rawExecSQL("CREATE TABLE encrypted.$table AS SELECT * FROM $table;")
            }

            // --- START: Added Validation Step ---
            var validationDb: SQLiteDatabase? = null
            try {
                // 尝试重新打开临时数据库并验证表数量
                validationDb = SQLiteDatabase.openDatabase(
                    tempDatabaseFile.path,
                    newPassword,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val validationCursor = validationDb.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%';", null)
                var copiedTableCount = 0
                validationCursor.use {
                    if (it.moveToFirst()) {
                        copiedTableCount = it.getInt(0)
                    }
                }
                if (copiedTableCount != tables.size) {
                    throw IllegalStateException("Validation failed: Table count mismatch after copy. Expected ${tables.size}, found $copiedTableCount")
                }
                // Log.i("DatabaseMigrationHelper", "Temporary database validation successful.")
            } catch (validationError: Exception) {
                // Log.e("DatabaseMigrationHelper", "Temporary database validation failed.", validationError)
                // 验证失败，需要触发外部 catch 进行回滚
                throw IllegalStateException("Temporary database validation failed after copy.", validationError)
            } finally {
                validationDb?.close()
            }
            // --- END: Added Validation Step ---

            // 关闭数据库 (Close sourceDb first before detaching destination)
            sourceDb.close()
            destinationDb.rawExecSQL("DETACH DATABASE encrypted;") // Detach should be on destinationDb
            destinationDb.close()

            // 备份原始数据库
            val backupFile = File(databaseFile.path + ".bak")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            databaseFile.renameTo(backupFile)

            // 替换为新数据库
            tempDatabaseFile.renameTo(databaseFile)

            true
        } catch (e: Exception) {
            // Log.e("DatabaseMigrationHelper", "Database re-encryption failed.", e) // 记录详细错误

            // --- START: Added Rollback Logic ---
            val databaseFile = context.getDatabasePath(DATABASE_NAME) // Get paths again just in case
            val tempDatabaseFile = context.getDatabasePath(TEMP_DATABASE_NAME)
            val backupFile = File(databaseFile.path + ".bak")

            // 1. 删除可能不完整的临时文件
            if (tempDatabaseFile.exists()) {
                if (tempDatabaseFile.delete()) {
                    // Log.i("DatabaseMigrationHelper", "Rollback: Deleted temporary database file.")
                } else {
                    // Log.w("DatabaseMigrationHelper", "Rollback: Failed to delete temporary database file.")
                }
            }

            // 2. 如果原始文件已备份但未被替换，尝试恢复备份
            if (backupFile.exists() && !databaseFile.exists()) {
                if (backupFile.renameTo(databaseFile)) {
                    // Log.i("DatabaseMigrationHelper", "Rollback: Restored original database from backup.")
                } else {
                    // Log.e("DatabaseMigrationHelper", "Rollback: CRITICAL - Failed to restore original database from backup.")
                    // Consider notifying the user or taking other critical action
                }
            }
            // --- END: Added Rollback Logic ---

            false
        }
    }

    /**
     * 检查数据库是否已加密
     */
    fun isDatabaseEncrypted(): Boolean {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (!databaseFile.exists()) {
            // 文件不存在，肯定未加密（或者说是初始状态）
            return false
        }

        // 首先获取密码
        val password = try {
            encryptionManager.getDatabasePassword()
        } catch (e: Exception) {
            // 如果获取密码失败，无法判断，当作未加密处理或记录错误
            // Log.e("DatabaseMigrationHelper", "Failed to get database password", e)
            return false // 无法检查，保守返回 false
        }

        // 如果密码为空，无法通过此方法检查加密状态
        if (password.isNullOrEmpty()) {
            // Log.w("DatabaseMigrationHelper", "Password is null or empty, cannot reliably check encryption status via openDatabase.")
            return false
        }

        var db: SQLiteDatabase? = null
        return try {
            // Instantiate the named classes
            val hookInstance = MyDatabaseHook()
            val errorHandlerInstance = MyDatabaseErrorHandler()

            // Convert the String password to CharArray
            val passwordChars = password.toCharArray()

            // Remove the commented out anonymous class definitions
            /*
            val hook = object : SQLiteDatabaseHook { // Provide an empty hook implementation
                override fun preKey(database: SQLiteDatabase?) {}
                override fun postKey(database: SQLiteDatabase?) {}
            }
            val errorHandler = object : android.database.DatabaseErrorHandler {
                override fun onCorruption(dbObj: android.database.sqlite.SQLiteDatabase?) {
                    // 如果SQLite报告损坏，也认为状态有问题
                    // Log.w("DatabaseMigrationHelper", "Database corrupted during encryption check.")
                }
            }
            */

            // 尝试使用预期密码以只读方式打开数据库
            // 使用简化的方法调用，避免参数不匹配问题
            db = SQLiteDatabase.openDatabase(
                databaseFile.path,
                String(passwordChars), // 将 CharArray 转换为 String
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            // 如果能成功打开，说明数据库已使用预期密码加密
            true
        } catch (e: SQLiteException) {
            // 捕获 SQLCipher 特有的异常 (Now catching standard Android SQLiteException)
            // 检查常见的表示"未加密"或"密码错误"的情况
            val message = e.message ?: ""
            // SQLCipher 密码错误通常会包含 "invalid password" 或 错误码 26 (SQLITE_NOTADB)
            if (message.contains("invalid password", ignoreCase = true) || e.message?.contains("file is not a database") == true || message.contains("(code 26)")) {
                // 密码无效或文件格式不对，当作未加密处理
                false
            } else {
                // 其他 SQLiteException，可能指示更严重的问题（如磁盘IO错误），记录日志
                // Log.e("DatabaseMigrationHelper", "Unexpected SQLiteException during encryption check", e)
                false // 保守返回 false
            }
        } catch (e: Exception) {
            // 捕获其他通用异常
            // Log.e("DatabaseMigrationHelper", "Unexpected error during encryption check", e)
            false // 保守返回 false
        } finally {
            db?.close() // 确保数据库连接被关闭
        }
    }
}