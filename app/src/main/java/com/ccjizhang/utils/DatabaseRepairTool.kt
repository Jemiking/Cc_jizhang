package com.ccjizhang.utils

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Room
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.model.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库修复工具
 * 用于检测和修复数据库损坏问题
 */
@Singleton
class DatabaseRepairTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val DATABASE_NAME = "ccjizhang_database_plain"
        private const val BACKUP_SUFFIX = ".bak"
        private const val TAG = "DatabaseRepairTool"

        // 数据库状态标志文件，用于标记数据库状态
        private const val DB_STATUS_FILE = "db_status.txt"
        private const val DB_STATUS_OK = "OK"
        private const val DB_STATUS_REPAIRING = "REPAIRING"
        private const val DB_STATUS_CORRUPTED = "CORRUPTED"
    }

    /**
     * 检测数据库是否损坏
     * @return 如果数据库损坏返回true，否则返回false
     */
    suspend fun isDatabaseCorrupted(): Boolean = withContext(Dispatchers.IO) {
        // 首先检查状态文件
        if (getDatabaseStatus() == DB_STATUS_CORRUPTED) {
            Log.w(TAG, "数据库状态文件标记为已损坏")
            return@withContext true
        }

        // 验证数据库是否可以使用当前密码打开
        if (!isDatabaseValid()) {
            Log.e(TAG, "数据库无法使用当前密码打开")
            setDatabaseStatus(DB_STATUS_CORRUPTED)
            return@withContext true
        }

        try {
            // 尝试执行一个简单的查询来检测数据库是否可用
            val tempDb = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // 允许在迁移失败时重建数据库
            .build()

            try {
                // 尝试执行一个简单的查询
                tempDb.transactionDao().getAllTransactionsSync()
                tempDb.close()
                // 更新数据库状态
                setDatabaseStatus(DB_STATUS_OK)
                return@withContext false // 数据库正常
            } catch (e: SQLiteException) {
                Log.e(TAG, "数据库查询失败", e)
                tempDb.close()
                // 更新数据库状态
                setDatabaseStatus(DB_STATUS_CORRUPTED)
                return@withContext true // 数据库损坏
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据库打开失败", e)
            // 更新数据库状态
            setDatabaseStatus(DB_STATUS_CORRUPTED)
            return@withContext true // 无法打开数据库，视为损坏
        }
    }

    /**
     * 验证数据库是否可以打开
     * @return 如果数据库可以打开返回true，否则返回false
     */
    private fun isDatabaseValid(): Boolean {
        try {
            // 检查数据库文件是否存在
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                Log.i(TAG, "数据库文件不存在，将创建新数据库")
                return true // 数据库不存在，将创建新数据库
            }

            // 检查数据库文件大小
            if (dbFile.length() == 0L) {
                Log.w(TAG, "数据库文件大小为0，可能已损坏")
                return false
            }

            // 简单检查数据库文件是否可读
            if (!dbFile.canRead()) {
                Log.e(TAG, "数据库文件不可读")
                return false
            }

            // 尝试打开数据库进行读取测试
            try {
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                // 执行简单查询
                val cursor = db.rawQuery("SELECT 1", null)
                val result = cursor.moveToFirst()
                cursor.close()
                db.close()

                if (!result) {
                    Log.w(TAG, "数据库可以打开但无法执行查询")
                    return false
                }

                Log.i(TAG, "数据库文件检查通过，可以正常打开和查询")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "数据库文件无法打开或查询", e)
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "验证数据库失败", e)
            return false
        }
    }

    /**
     * 修复损坏的数据库
     * 通过删除现有数据库并创建新的数据库来修复
     * @return 修复是否成功
     */
    suspend fun repairDatabase(): Boolean = withContext(Dispatchers.IO) {
        // 设置数据库状态为修复中
        setDatabaseStatus(DB_STATUS_REPAIRING)

        try {
            Log.i(TAG, "开始修复数据库")
            // 1. 备份当前数据库（即使损坏也备份）
            backupDatabase()

            // 2. 删除当前数据库文件
            deleteDatabase()

            // 3. 重新初始化数据库
            initializeDatabase()

            // 设置数据库状态为正常
            setDatabaseStatus(DB_STATUS_OK)
            Log.i(TAG, "数据库修复完成")
            return@withContext true
        } catch (e: Exception) {
            // 修复失败
            Log.e(TAG, "数据库修复失败", e)
            // 保持数据库状态为损坏
            setDatabaseStatus(DB_STATUS_CORRUPTED)
            return@withContext false
        }
    }

    /**
     * 强制修复数据库，不进行检查，直接删除并重建
     * 用于紧急情况下的恢复
     * @return 修复是否成功
     */
    suspend fun forceRepairDatabase(): Boolean = withContext(Dispatchers.IO) {
        // 设置数据库状态为修复中
        setDatabaseStatus(DB_STATUS_REPAIRING)

        try {
            Log.i(TAG, "开始强制修复数据库")

            // 1. 先关闭所有数据库连接
            try {
                // 尝试强制关闭数据库连接
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                if (dbFile.exists()) {
                    try {
                        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                            dbFile.path,
                            null,
                            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                        )
                        db.close()
                        Log.i(TAG, "成功关闭数据库连接")
                    } catch (e: Exception) {
                        Log.w(TAG, "关闭数据库连接失败，继续修复", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "关闭数据库连接时发生异常，继续修复", e)
            }

            // 2. 尝试备份当前数据库（如果可能）
            try {
                backupDatabase()
                Log.i(TAG, "数据库备份成功")
            } catch (e: Exception) {
                Log.w(TAG, "备份数据库失败，继续修复", e)
            }

            // 3. 删除当前数据库文件
            try {
                deleteDatabase()
                Log.i(TAG, "数据库删除成功")
            } catch (e: Exception) {
                Log.w(TAG, "删除数据库失败，尝试强制删除", e)
                forceDeleteDatabase()
            }

            // 等待一小段时间，确保文件系统操作完成
            kotlinx.coroutines.delay(500)

            // 4. 重新初始化数据库
            var initSuccess = false
            try {
                Log.i(TAG, "开始初始化新数据库")
                initializeDatabase()
                initSuccess = true
                Log.i(TAG, "数据库初始化成功")
            } catch (e: Exception) {
                Log.e(TAG, "初始化数据库失败，尝试简单初始化", e)
                try {
                    simpleInitializeDatabase()
                    initSuccess = true
                    Log.i(TAG, "简单初始化数据库成功")
                } catch (e2: Exception) {
                    Log.e(TAG, "简单初始化也失败，尝试最后的恢复措施", e2)
                    // 尝试最后的恢复措施：创建一个空数据库文件
                    try {
                        val dbFile = context.getDatabasePath(DATABASE_NAME)
                        if (!dbFile.exists()) {
                            dbFile.parentFile?.mkdirs()
                            dbFile.createNewFile()
                            Log.i(TAG, "创建空数据库文件成功")
                            initSuccess = true
                        }
                    } catch (e3: Exception) {
                        Log.e(TAG, "创建空数据库文件失败", e3)
                    }
                }
            }

            // 5. 设置数据库状态
            if (initSuccess) {
                setDatabaseStatus(DB_STATUS_OK)
                Log.i(TAG, "强制数据库修复完成")
            } else {
                // 即使初始化失败，也将状态设为OK，让应用尝试正常运行
                // 这样可能会在下次启动时再次触发修复
                setDatabaseStatus(DB_STATUS_OK)
                Log.w(TAG, "数据库修复不完整，但允许应用继续运行")
            }
            return@withContext true
        } catch (e: Exception) {
            // 修复失败
            Log.e(TAG, "强制数据库修复失败", e)
            // 保持数据库状态为损坏
            setDatabaseStatus(DB_STATUS_CORRUPTED)
            return@withContext false
        }
    }

    /**
     * 清除数据库状态文件
     * 在应用完全重置时使用
     */
    fun clearDatabaseStatus() {
        try {
            val statusFile = File(context.filesDir, DB_STATUS_FILE)
            if (statusFile.exists()) {
                val deleted = statusFile.delete()
                Log.i(TAG, "清除数据库状态文件结果: $deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清除数据库状态文件失败", e)
        }
    }

    /**
     * 备份当前数据库
     */
    private fun backupDatabase() {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        if (databaseFile.exists()) {
            val backupFile = File(databaseFile.path + BACKUP_SUFFIX)
            if (backupFile.exists()) {
                backupFile.delete()
            }
            try {
                databaseFile.copyTo(backupFile, overwrite = true)
                Log.i(TAG, "数据库备份成功: ${backupFile.path}")
            } catch (e: IOException) {
                Log.e(TAG, "数据库备份失败", e)
                throw e
            }
        } else {
            Log.w(TAG, "数据库文件不存在，无法备份")
        }
    }

    /**
     * 删除当前数据库文件
     */
    private fun deleteDatabase() {
        val result = context.deleteDatabase(DATABASE_NAME)
        Log.i(TAG, "删除数据库结果: $result")

        // 删除相关的辅助文件
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val dbPath = dbFile.path
        val shmDeleted = File("$dbPath-shm").delete() // 删除共享内存文件
        val walDeleted = File("$dbPath-wal").delete() // 删除预写日志文件
        val journalDeleted = File("$dbPath-journal").delete() // 删除日志文件

        Log.i(TAG, "删除辅助文件结果: shm=$shmDeleted, wal=$walDeleted, journal=$journalDeleted")
    }

    /**
     * 强制删除数据库文件
     * 当常规删除失败时使用
     */
    private fun forceDeleteDatabase() {
        try {
            // 先删除相关的辅助文件
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbPath = dbFile.path

            // 删除辅助文件
            val shmFile = File("$dbPath-shm")
            if (shmFile.exists()) {
                val shmDeleted = shmFile.delete()
                Log.i(TAG, "删除SHM文件结果: $shmDeleted")
            }

            val walFile = File("$dbPath-wal")
            if (walFile.exists()) {
                val walDeleted = walFile.delete()
                Log.i(TAG, "删除WAL文件结果: $walDeleted")
            }

            val journalFile = File("$dbPath-journal")
            if (journalFile.exists()) {
                val journalDeleted = journalFile.delete()
                Log.i(TAG, "删除Journal文件结果: $journalDeleted")
            }

            // 等待一小段时间，确保文件系统操作完成
            Thread.sleep(100)

            // 再删除主数据库文件
            if (dbFile.exists()) {
                // 尝试先将文件内容清空，再删除
                try {
                    java.io.FileOutputStream(dbFile).use { it.channel.truncate(0) }
                    Log.i(TAG, "数据库文件内容已清空")
                } catch (e: Exception) {
                    Log.w(TAG, "清空数据库文件内容失败", e)
                }

                // 尝试删除文件
                val deleted = dbFile.delete()
                Log.i(TAG, "强制删除数据库文件结果: $deleted")

                // 如果删除失败，尝试重命名文件
                if (!deleted && dbFile.exists()) {
                    val renamedFile = File(dbFile.path + ".old")
                    val renamed = dbFile.renameTo(renamedFile)
                    Log.i(TAG, "重命名数据库文件结果: $renamed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "强制删除数据库文件失败", e)
        }
    }

    /**
     * 初始化新的数据库
     */
    private suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        Log.i(TAG, "开始初始化数据库")
        // 创建一个临时数据库实例以触发数据库创建
        val tempDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // 允许在迁移失败时重建数据库
        .build()

        // 关闭数据库连接
        tempDb.close()
        Log.i(TAG, "数据库创建成功")

        // 初始化默认数据
        try {
            Log.i(TAG, "开始初始化默认分类")
            categoryRepository.initDefaultCategories()
            Log.i(TAG, "默认分类初始化完成")

            Log.i(TAG, "开始初始化默认账户")
            accountRepository.initDefaultAccounts()
            Log.i(TAG, "默认账户初始化完成")

            // 数据一致性检查在应用启动时单独执行
            Log.i(TAG, "跳过数据一致性检查，避免循环依赖")
        } catch (e: Exception) {
            Log.e(TAG, "初始化默认数据失败", e)
            throw e
        }
    }

    /**
     * 简单初始化数据库
     * 当完整初始化失败时使用，只创建数据库结构，不添加默认数据
     * 增强版：即使在Room初始化失败的情况下，也尝试手动创建基本表结构
     */
    private suspend fun simpleInitializeDatabase() = withContext(Dispatchers.IO) {
        Log.i(TAG, "开始简单初始化数据库")
        try {
            // 创建一个临时数据库实例以触发数据库创建
            val tempDb = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // 允许在迁移失败时重建数据库
            .build()

            // 关闭数据库连接
            tempDb.close()
            Log.i(TAG, "数据库简单初始化成功")
            return@withContext
        } catch (e: Exception) {
            Log.e(TAG, "Room数据库初始化失败，尝试手动创建基本表结构", e)
            // 继续执行手动创建表的逻辑
        }

        // 如果Room初始化失败，尝试手动创建基本表结构
        try {
            Log.i(TAG, "开始手动创建基本表结构")
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
            }

            // 使用SQLite直接创建数据库和基本表
            val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(dbFile, null)

            // 创建categories表
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "icon TEXT NOT NULL, " +
                "color INTEGER NOT NULL, " +
                "isCustom INTEGER NOT NULL, " +
                "sortOrder INTEGER NOT NULL, " +
                "parentId INTEGER, " +
                "level INTEGER NOT NULL, " +
                "isIncome INTEGER NOT NULL);"
            )

            // 创建accounts表
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "balance REAL NOT NULL, " +
                "currency TEXT NOT NULL, " +
                "icon TEXT NOT NULL, " +
                "color INTEGER NOT NULL, " +
                "isDefault INTEGER NOT NULL, " +
                "includeInTotal INTEGER NOT NULL, " +
                "note TEXT);"
            )

            // 创建transactions表
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "categoryId INTEGER, " +
                "accountId INTEGER NOT NULL, " +
                "date INTEGER NOT NULL, " +
                "note TEXT NOT NULL, " +
                "isIncome INTEGER NOT NULL, " +
                "location TEXT NOT NULL, " +
                "imageUri TEXT NOT NULL, " +
                "toAccountId INTEGER);"
            )

            db.close()
            Log.i(TAG, "手动创建基本表结构成功")
        } catch (e: Exception) {
            Log.e(TAG, "手动创建基本表结构失败", e)
            // 即使这里失败，也不抛出异常，让应用继续运行
        }
    }

    /**
     * 获取数据库状态
     * @return 数据库状态
     */
    private fun getDatabaseStatus(): String {
        val statusFile = File(context.filesDir, DB_STATUS_FILE)
        return if (statusFile.exists()) {
            try {
                statusFile.readText().trim()
            } catch (e: Exception) {
                Log.e(TAG, "读取数据库状态文件失败", e)
                DB_STATUS_CORRUPTED // 如果无法读取，假设数据库已损坏
            }
        } else {
            DB_STATUS_OK // 如果状态文件不存在，假设数据库正常
        }
    }

    /**
     * 设置数据库状态
     * @param status 数据库状态
     */
    fun setDatabaseStatus(status: String) {
        val statusFile = File(context.filesDir, DB_STATUS_FILE)
        try {
            statusFile.writeText(status)
            Log.i(TAG, "数据库状态已更新为: $status")
        } catch (e: Exception) {
            Log.e(TAG, "写入数据库状态文件失败", e)
        }
    }

    /**
     * 完全重置数据库
     * 删除所有数据库文件并重新创建
     * 这是一个更强力的修复选项，用于解决其他修复方法无法解决的问题
     * @return 重置是否成功
     */
    suspend fun completelyResetDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始完全重置数据库")

            // 1. 清除数据库状态
            clearDatabaseStatus()

            // 2. 强制关闭所有数据库连接
            try {
                // 尝试关闭所有SQLite连接
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                if (dbFile.exists()) {
                    try {
                        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                            dbFile.path,
                            null,
                            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                        )
                        db.close()
                        Log.i(TAG, "成功关闭数据库连接")
                    } catch (e: Exception) {
                        Log.w(TAG, "关闭数据库连接失败", e)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "关闭数据库连接时发生异常", e)
            }

            // 3. 删除所有数据库文件
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val dbDir = dbFile.parentFile

            if (dbDir != null && dbDir.exists()) {
                // 删除目录中的所有相关文件
                dbDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(DATABASE_NAME) ||
                        file.name.endsWith(".db") ||
                        file.name.endsWith(".db-shm") ||
                        file.name.endsWith(".db-wal") ||
                        file.name.endsWith(".db-journal")) {

                        try {
                            // 先清空文件内容
                            try {
                                java.io.FileOutputStream(file).use { it.channel.truncate(0) }
                                Log.i(TAG, "清空文件内容成功: ${file.name}")
                            } catch (e: Exception) {
                                Log.w(TAG, "清空文件内容失败: ${file.name}", e)
                            }

                            // 删除文件
                            val deleted = file.delete()
                            Log.i(TAG, "删除文件: ${file.name}, 结果: $deleted")

                            // 如果删除失败，尝试重命名
                            if (!deleted && file.exists()) {
                                val renamed = file.renameTo(File(file.path + ".old"))
                                Log.i(TAG, "重命名文件: ${file.name}, 结果: $renamed")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "删除文件失败: ${file.name}", e)
                        }
                    }
                }
            }

            // 4. 等待一段时间，确保文件系统操作完成
            kotlinx.coroutines.delay(1000)

            // 5. 重新初始化数据库
            try {
                Log.i(TAG, "开始初始化新数据库")
                initializeDatabase()
                Log.i(TAG, "数据库初始化成功")
            } catch (e: Exception) {
                Log.e(TAG, "初始化数据库失败，尝试简单初始化", e)
                try {
                    simpleInitializeDatabase()
                    Log.i(TAG, "简单初始化数据库成功")
                } catch (e2: Exception) {
                    Log.e(TAG, "简单初始化也失败，尝试最后的恢复措施", e2)
                    // 尝试最后的恢复措施：创建一个空数据库文件
                    try {
                        val newDbFile = context.getDatabasePath(DATABASE_NAME)
                        if (!newDbFile.exists()) {
                            newDbFile.parentFile?.mkdirs()
                            newDbFile.createNewFile()
                            Log.i(TAG, "创建空数据库文件成功")
                        }
                    } catch (e3: Exception) {
                        Log.e(TAG, "创建空数据库文件失败", e3)
                        return@withContext false
                    }
                }
            }

            // 6. 设置数据库状态为正常
            setDatabaseStatus(DB_STATUS_OK)

            Log.i(TAG, "数据库完全重置成功")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "数据库完全重置失败", e)
            setDatabaseStatus(DB_STATUS_CORRUPTED)
            return@withContext false
        }
    }
}
