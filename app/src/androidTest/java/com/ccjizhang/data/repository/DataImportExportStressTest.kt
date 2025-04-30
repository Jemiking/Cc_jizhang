package com.ccjizhang.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionType
import com.ccjizhang.utils.DatabaseRecoveryManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.UUID
import kotlin.system.measureTimeMillis

/**
 * 数据导入导出压力测试
 * 测试大数据量导入导出的性能和稳定性
 */
@RunWith(AndroidJUnit4::class)
class DataImportExportStressTest {

    private lateinit var db: AppDatabase
    private lateinit var dataExportImportRepository: DataExportImportRepository
    private lateinit var databaseRecoveryManager: DatabaseRecoveryManager
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // 创建内存数据库用于测试
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()

        // 初始化仓库
        val accountRepository = AccountRepository(db.accountDao())
        val categoryRepository = CategoryRepository(db.categoryDao())
        val transactionRepository = TransactionRepository(
            db.transactionDao(),
            accountRepository,
            categoryRepository,
            // 创建DatabaseRepairTool
            DatabaseExceptionHandler(
                DatabaseRepairTool(
                    context,
                    categoryRepository,
                    accountRepository
                )
            )
        )

        dataExportImportRepository = DataExportImportRepository(
            db,
            db.transactionDao(),
            db.categoryDao(),
            db.accountDao(),
            db.budgetDao()
        )

        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()

        // 清理测试生成的文件
        val backupDir = File(context.filesDir, "db_backups")
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    }

    /**
     * 生成大量测试数据
     */
    private suspend fun generateLargeTestData(
        numAccounts: Int = 10,
        numCategories: Int = 50,
        numTransactions: Int = 1000
    ) {
        // 生成账户
        val accounts = List(numAccounts) { i ->
            Account(
                id = i.toLong() + 1,
                name = "账户${i + 1}",
                balance = 10000.0,
                type = "现金",
                color = "#FF0000",
                icon = "cash",
                isDefault = i == 0,
                sortOrder = i,
                isArchived = false
            )
        }
        db.accountDao().insertAll(accounts)

        // 生成分类
        val categories = List(numCategories) { i ->
            Category(
                id = i.toLong() + 1,
                name = "分类${i + 1}",
                type = if (i % 2 == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                color = "#00FF00",
                icon = "food",
                sortOrder = i,
                level = 1,
                parentId = null
            )
        }
        db.categoryDao().insertAll(categories)

        // 生成交易
        val transactions = List(numTransactions) { i ->
            Transaction(
                id = i.toLong() + 1,
                amount = (Math.random() * 1000).toDouble(),
                type = if (i % 3 == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                date = Date(System.currentTimeMillis() - (i * 86400000L)), // 每天一笔交易
                note = "交易${i + 1} ${UUID.randomUUID()}",
                categoryId = (i % numCategories + 1).toLong(),
                accountId = (i % numAccounts + 1).toLong(),
                toAccountId = if (i % 10 == 0) ((i % numAccounts) + 2).toLong() else null,
                isRecurring = i % 20 == 0
            )
        }
        db.transactionDao().insertAll(transactions)
    }

    /**
     * 测试大数据量导出性能
     */
    @Test
    fun testLargeDataExport() = runBlocking {
        // 生成大量测试数据
        val numTransactions = 5000
        generateLargeTestData(
            numAccounts = 20,
            numCategories = 100,
            numTransactions = numTransactions
        )

        // 验证数据已生成
        val allTransactions = db.transactionDao().getAllTransactionsSync()
        assertEquals("应该生成正确数量的交易", numTransactions, allTransactions.size)

        // 测量导出时间
        val exportFile = File(context.cacheDir, "large_export_test.json")
        val exportTime = measureTimeMillis {
            val result = dataExportImportRepository.exportDataToJsonFile(context, exportFile)
            assertTrue("导出应该成功", result.isSuccess)
        }

        println("导出 $numTransactions 条交易记录耗时: ${exportTime}ms")

        // 验证导出文件存在且大小合理
        assertTrue("导出文件应该存在", exportFile.exists())
        assertTrue("导出文件应该有合理的大小", exportFile.length() > 1000)

        // 清理数据库
        db.clearAllTables()

        // 验证数据库已清空
        val transactionsAfterClear = db.transactionDao().getAllTransactionsSync()
        assertEquals("数据库应该已清空", 0, transactionsAfterClear.size)

        // 测量导入时间
        val importTime = measureTimeMillis {
            val result = dataExportImportRepository.importDataFromJsonFile(context, exportFile)
            assertTrue("导入应该成功", result.isSuccess)
        }

        println("导入 $numTransactions 条交易记录耗时: ${importTime}ms")

        // 验证数据已导入
        val transactionsAfterImport = db.transactionDao().getAllTransactionsSync()
        assertEquals("应该导入正确数量的交易", numTransactions, transactionsAfterImport.size)
    }

    /**
     * 测试备份和恢复功能
     */
    @Test
    fun testBackupAndRestore() = runBlocking {
        // 生成测试数据
        val numTransactions = 1000
        generateLargeTestData(
            numAccounts = 5,
            numCategories = 20,
            numTransactions = numTransactions
        )

        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_backup.json")
        assertTrue("备份文件应该创建成功", backupFile != null && backupFile.exists())

        // 清理数据库
        db.clearAllTables()

        // 从备份恢复
        val restoreResult = databaseRecoveryManager.restoreFromBackup(backupFile!!)
        assertTrue("从备份恢复应该成功", restoreResult)

        // 验证数据已恢复
        val transactionsAfterRestore = db.transactionDao().getAllTransactionsSync()
        assertEquals("应该恢复正确数量的交易", numTransactions, transactionsAfterRestore.size)

        val accountsAfterRestore = db.accountDao().getAllAccountsSync()
        assertEquals("应该恢复正确数量的账户", 5, accountsAfterRestore.size)

        val categoriesAfterRestore = db.categoryDao().getAllCategoriesSync()
        assertEquals("应该恢复正确数量的分类", 20, categoriesAfterRestore.size)
    }

    /**
     * 测试多次备份和清理旧备份功能
     */
    @Test
    fun testMultipleBackupsAndCleanup() = runBlocking {
        // 生成测试数据
        generateLargeTestData(numTransactions = 100)

        // 创建多个备份
        repeat(10) { i ->
            databaseRecoveryManager.createManualBackup("backup_$i.json")
            // 稍微延迟，确保时间戳不同
            Thread.sleep(100)
        }

        // 获取所有备份
        val backups = databaseRecoveryManager.getAllBackups()
        assertTrue("应该创建了多个备份", backups.size > 5)

        // 验证备份按时间排序
        for (i in 0 until backups.size - 1) {
            assertTrue(
                "备份应该按时间降序排序",
                backups[i].lastModified() >= backups[i + 1].lastModified()
            )
        }

        // 创建一个新备份，触发清理旧备份
        databaseRecoveryManager.createScheduledBackup()

        // 验证旧备份已被清理
        val backupsAfterCleanup = databaseRecoveryManager.getAllBackups()
        assertEquals("应该只保留最新的备份", 5, backupsAfterCleanup.size)
    }
}
