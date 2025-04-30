package com.ccjizhang.data.archive

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.data.archive.ArchiveAccountDao
import com.ccjizhang.data.archive.ArchiveCategoryDao
import com.ccjizhang.data.archive.ArchiveTransactionDao
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.TransactionManager
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.utils.DatabaseExceptionHandler
import com.ccjizhang.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据归档管理器
 * 负责管理数据归档功能，包括创建归档数据库、归档旧数据和查询归档数据
 */
@Singleton
class DatabaseArchiveManager @Inject constructor(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val transactionManager: TransactionManager,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "DatabaseArchiveManager"
        private const val ARCHIVE_DB_PREFIX = "ccjizhang_archive_"
        private const val ARCHIVE_FOLDER = "archives"
        private const val DEFAULT_ARCHIVE_THRESHOLD_MONTHS = 12 // 默认归档阈值：12个月
    }

    // 归档数据库缓存
    private val archiveDatabaseCache = mutableMapOf<String, ArchiveDatabase>()

    /**
     * 初始化归档管理器
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "初始化数据归档管理器")

            // 创建归档文件夹
            val archiveDir = File(context.filesDir, ARCHIVE_FOLDER)
            if (!archiveDir.exists()) {
                archiveDir.mkdirs()
                Log.i(TAG, "创建归档文件夹: ${archiveDir.absolutePath}")
            }

            // 检查是否需要执行自动归档
            checkAndPerformAutoArchive()

            Log.i(TAG, "数据归档管理器初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "初始化数据归档管理器失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "初始化数据归档管理器")
        }
    }

    /**
     * 检查并执行自动归档
     */
    private suspend fun checkAndPerformAutoArchive() {
        try {
            // 获取上次归档时间
            val lastArchiveTime = getLastArchiveTime()
            val currentTime = System.currentTimeMillis()

            // 如果从未归档或者距离上次归档已经超过3个月，则执行自动归档
            if (lastArchiveTime == 0L || (currentTime - lastArchiveTime) > 90 * 24 * 60 * 60 * 1000L) {
                Log.i(TAG, "执行自动归档")

                // 获取归档阈值日期（默认12个月前的数据）
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -DEFAULT_ARCHIVE_THRESHOLD_MONTHS)
                val thresholdDate = calendar.time

                // 执行归档
                val archiveResult = archiveDataBeforeDate(thresholdDate)

                // 更新上次归档时间
                saveLastArchiveTime(currentTime)

                Log.i(TAG, "自动归档完成，归档记录数: ${archiveResult.archivedCount}")
            } else {
                Log.i(TAG, "无需执行自动归档")
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行自动归档失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "执行自动归档")
        }
    }

    /**
     * 获取上次归档时间
     */
    private fun getLastArchiveTime(): Long {
        val prefs = context.getSharedPreferences("archive_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_archive_time", 0L)
    }

    /**
     * 保存上次归档时间
     */
    private fun saveLastArchiveTime(time: Long) {
        val prefs = context.getSharedPreferences("archive_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_archive_time", time).apply()
    }

    /**
     * 归档指定日期之前的数据
     * @param date 归档日期阈值，该日期之前的数据将被归档
     * @return 归档结果
     */
    suspend fun archiveDataBeforeDate(date: Date): ArchiveResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "开始归档 ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)} 之前的数据")

        val result = ArchiveResult()

        try {
            // 获取归档数据库
            val archiveDb = getOrCreateArchiveDatabase(date)

            // 在事务中执行归档操作
            transactionManager.executeInTransaction {
                // 1. 查询需要归档的交易记录
                val transactionsToArchive = appDatabase.transactionDao().getTransactionsBeforeDate(date).first()

                if (transactionsToArchive.isNotEmpty()) {
                    Log.i(TAG, "找到 ${transactionsToArchive.size} 条需要归档的交易记录")

                    // 2. 获取相关的分类和账户数据
                    // 获取交易相关的分类和账户ID
                    val categoryIds = transactionsToArchive.mapNotNull { it.categoryId }.distinct()
                    val accountIds = transactionsToArchive.flatMap { transaction ->
                        listOfNotNull(transaction.accountId, transaction.toAccountId)
                    }.distinct()

                    // 从主数据库中获取分类和账户数据
                    val categories = if (categoryIds.isNotEmpty()) {
                        appDatabase.categoryDao().getCategoriesByIds(categoryIds)
                    } else {
                        emptyList()
                    }

                    val accounts = if (accountIds.isNotEmpty()) {
                        appDatabase.accountDao().getAccountsByIds(accountIds).first()
                    } else {
                        emptyList()
                    }

                    Log.i(TAG, "找到 ${categories.size} 个相关分类和 ${accounts.size} 个相关账户")

                    // 3. 将数据写入归档数据库
                    if (categories.isNotEmpty()) {
                        archiveDb.categoryDao().insertAll(categories)
                    }

                    if (accounts.isNotEmpty()) {
                        archiveDb.accountDao().insertAll(accounts)
                    }

                    archiveDb.transactionDao().insertAll(transactionsToArchive)

                    // 4. 从主数据库中删除已归档的交易数据
                    val transactionIds = transactionsToArchive.map { it.id }
                    appDatabase.transactionDao().deleteByIds(transactionIds)

                    result.archivedCount = transactionsToArchive.size
                    Log.i(TAG, "成功归档 ${result.archivedCount} 条交易记录、${categories.size} 个分类和 ${accounts.size} 个账户")
                } else {
                    Log.i(TAG, "没有找到需要归档的交易记录")
                }
            }

            // 归档完成后执行VACUUM操作优化数据库
            appDatabase.runInTransaction {
                appDatabase.openHelper.writableDatabase.execSQL("VACUUM")
            }

            Log.i(TAG, "归档操作完成，已执行VACUUM优化")
            result.success = true
        } catch (e: Exception) {
            Log.e(TAG, "归档数据失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "归档数据")
            result.error = e.message ?: "未知错误"
        }

        return@withContext result
    }

    /**
     * 获取或创建归档数据库
     * @param date 归档日期，用于生成归档数据库名称
     * @return 归档数据库实例
     */
    private fun getOrCreateArchiveDatabase(date: Date): ArchiveDatabase {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
        val dbName = "${ARCHIVE_DB_PREFIX}${year}"

        // 如果缓存中已有该数据库实例，直接返回
        archiveDatabaseCache[dbName]?.let { return it }

        // 初始化 SQLCipher
        SQLiteDatabase.loadLibs(context)

        // 数据库密码（实际应用中应该从安全存储中获取）
        val passphrase = SQLiteDatabase.getBytes("ccjizhang_archive_password".toCharArray())
        val factory = SupportFactory(passphrase)

        // 创建归档数据库
        val archiveDb = Room.databaseBuilder(
            context,
            ArchiveDatabase::class.java,
            dbName
        )
            .fallbackToDestructiveMigration()
            .openHelperFactory(factory)
            .build()

        // 将数据库实例添加到缓存
        archiveDatabaseCache[dbName] = archiveDb

        Log.i(TAG, "创建归档数据库: $dbName")
        return archiveDb
    }

    /**
     * 查询归档数据
     * @param year 归档年份
     * @return 归档数据的Flow
     */
    fun queryArchivedTransactions(year: String): Flow<List<Transaction>> {
        try {
            val archiveDb = getOrCreateArchiveDatabase(
                SimpleDateFormat("yyyy", Locale.getDefault()).parse(year) ?: Date()
            )
            return archiveDb.transactionDao().getAllTransactions()
        } catch (e: Exception) {
            Log.e(TAG, "查询归档数据失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "查询归档数据")
            throw e
        }
    }

    /**
     * 获取所有可用的归档年份
     * @return 可用的归档年份列表
     */
    suspend fun getAvailableArchiveYears(): List<String> = withContext(Dispatchers.IO) {
        try {
            val archiveDir = File(context.filesDir, ARCHIVE_FOLDER)
            if (!archiveDir.exists()) return@withContext emptyList()

            val archiveFiles = archiveDir.listFiles { file ->
                file.name.startsWith(ARCHIVE_DB_PREFIX) && file.name.endsWith(".db")
            } ?: return@withContext emptyList()

            return@withContext archiveFiles.mapNotNull { file ->
                val match = Regex("${ARCHIVE_DB_PREFIX}(\\d{4})").find(file.name)
                match?.groupValues?.get(1)
            }.sorted()
        } catch (e: Exception) {
            Log.e(TAG, "获取可用归档年份失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取可用归档年份")
            return@withContext emptyList()
        }
    }

    /**
     * 获取归档数据库信息
     * @return 归档数据库信息列表
     */
    suspend fun getArchiveDatabaseInfo(): List<ArchiveDatabaseInfo> = withContext(Dispatchers.IO) {
        try {
            val archiveDir = File(context.filesDir, ARCHIVE_FOLDER)
            if (!archiveDir.exists()) return@withContext emptyList()

            val archiveFiles = archiveDir.listFiles { file ->
                file.name.startsWith(ARCHIVE_DB_PREFIX) && file.name.endsWith(".db")
            } ?: return@withContext emptyList()

            return@withContext archiveFiles.map { file ->
                val match = Regex("${ARCHIVE_DB_PREFIX}(\\d{4})").find(file.name)
                val year = match?.groupValues?.get(1) ?: "未知"

                ArchiveDatabaseInfo(
                    year = year,
                    fileName = file.name,
                    fileSize = file.length(),
                    formattedSize = FileUtils.formatFileSize(file.length()),
                    lastModified = Date(file.lastModified())
                )
            }.sortedByDescending { it.year }
        } catch (e: Exception) {
            Log.e(TAG, "获取归档数据库信息失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "获取归档数据库信息")
            return@withContext emptyList()
        }
    }

    /**
     * 删除归档数据库
     * @param year 要删除的归档数据库年份
     * @return 是否删除成功
     */
    suspend fun deleteArchiveDatabase(year: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 关闭数据库连接
            val dbName = "${ARCHIVE_DB_PREFIX}${year}"
            archiveDatabaseCache[dbName]?.close()
            archiveDatabaseCache.remove(dbName)

            // 删除数据库文件
            val dbFile = context.getDatabasePath(dbName)
            val walFile = File("${dbFile.path}-wal")
            val shmFile = File("${dbFile.path}-shm")

            var success = true
            if (dbFile.exists()) success = success && dbFile.delete()
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            Log.i(TAG, "删除归档数据库: $dbName, 结果: $success")
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "删除归档数据库失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "删除归档数据库")
            return@withContext false
        }
    }

    /**
     * 关闭所有归档数据库连接
     */
    fun closeAll() {
        try {
            for ((name, db) in archiveDatabaseCache) {
                db.close()
                Log.i(TAG, "关闭归档数据库: $name")
            }
            archiveDatabaseCache.clear()
        } catch (e: Exception) {
            Log.e(TAG, "关闭归档数据库失败", e)
            databaseExceptionHandler.handleDatabaseException(e, "关闭归档数据库")
        }
    }

    /**
     * 归档结果数据类
     */
    data class ArchiveResult(
        var success: Boolean = false,
        var archivedCount: Int = 0,
        var error: String? = null
    )

    /**
     * 归档数据库信息数据类
     */
    data class ArchiveDatabaseInfo(
        val year: String,
        val fileName: String,
        val fileSize: Long,
        val formattedSize: String,
        val lastModified: Date
    )
}
