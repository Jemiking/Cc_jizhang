package com.ccjizhang.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionType
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.data.repository.TransactionRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.util.Date
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * 备份恢复性能测试
 * 测试备份和恢复操作的性能
 */
@RunWith(AndroidJUnit4::class)
class BackupRestorePerformanceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var dataExportImportRepository: DataExportImportRepository
    private lateinit var databaseRecoveryManager: DatabaseRecoveryManager
    private lateinit var backupFolder: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // 创建测试数据库
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // 初始化仓库
        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())
        transactionRepository = TransactionRepository(
            db.transactionDao(),
            accountRepository,
            mockk() // 模拟DatabaseExceptionHandler
        )
        
        // 初始化数据导出导入仓库
        dataExportImportRepository = DataExportImportRepository(
            db,
            db.transactionDao(),
            db.categoryDao(),
            db.accountDao(),
            db.budgetDao()
        )
        
        // 创建备份文件夹
        backupFolder = tempFolder.newFolder("backups")
        
        // 初始化数据库恢复管理器
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 生成大量测试数据
     */
    private fun generateLargeTestData(numTransactions: Int) = runBlocking {
        // 创建测试账户
        repeat(5) { i ->
            val account = Account(
                id = i.toLong() + 1,
                name = "测试账户 $i",
                balance = 1000.0 * (i + 1),
                type = "现金",
                color = "#FF0000",
                icon = "cash",
                isDefault = i == 0,
                sortOrder = i,
                isArchived = false
            )
            accountRepository.insertAccount(account)
        }
        
        // 创建测试分类
        repeat(20) { i ->
            val category = Category(
                id = i.toLong() + 1,
                name = "测试分类 $i",
                type = if (i % 2 == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                color = "#00FF00",
                icon = "food",
                sortOrder = i,
                level = 1,
                parentId = null
            )
            categoryRepository.insertCategory(category)
        }
        
        // 创建测试交易
        repeat(numTransactions) { i ->
            val transaction = Transaction(
                id = i.toLong() + 1,
                amount = 100.0 + i,
                type = if (i % 2 == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                date = Date(System.currentTimeMillis() - i * 86400000L), // 每天一条交易
                note = "测试交易 $i",
                categoryId = (i % 20 + 1).toLong(),
                accountId = (i % 5 + 1).toLong(),
                toAccountId = null,
                isRecurring = false
            )
            transactionRepository.addTransaction(transaction)
        }
    }

    /**
     * 测试大文件备份性能
     */
    @Test
    fun testLargeBackupPerformance() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(1000)
        
        // 测量备份时间
        val backupTime = measureTimeMillis {
            databaseRecoveryManager.createManualBackup("large_backup_performance.json")
        }
        
        println("备份1000条交易记录耗时: $backupTime 毫秒")
        
        // 验证性能在可接受范围内
        assertTrue(backupTime < 10000, "备份1000条交易记录应该在10秒内完成")
    }

    /**
     * 测试大文件恢复性能
     */
    @Test
    fun testLargeRestorePerformance() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(1000)
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("large_restore_performance.json")
        
        // 清空数据库
        db.clearAllTables()
        
        // 测量恢复时间
        val restoreTime = measureTimeMillis {
            databaseRecoveryManager.restoreFromBackup(backupFile)
        }
        
        println("恢复1000条交易记录耗时: $restoreTime 毫秒")
        
        // 验证性能在可接受范围内
        assertTrue(restoreTime < 10000, "恢复1000条交易记录应该在10秒内完成")
        
        // 验证数据已恢复
        val transactionsAfterRestore = transactionRepository.getAllTransactionsSync()
        assertTrue(transactionsAfterRestore.size >= 1000, "应该恢复至少1000条交易记录")
    }

    /**
     * 测试备份文件加载性能
     */
    @Test
    fun testBackupFileLoadingPerformance() = runBlocking {
        // 创建多个备份文件
        repeat(20) { i ->
            // 生成少量测试数据
            generateLargeTestData(10)
            
            // 创建备份
            databaseRecoveryManager.createManualBackup("backup_$i.json")
            
            // 清空数据库
            db.clearAllTables()
        }
        
        // 测量加载备份文件列表的时间
        val loadingTime = measureTimeMillis {
            databaseRecoveryManager.getAllBackups()
        }
        
        println("加载20个备份文件耗时: $loadingTime 毫秒")
        
        // 验证性能在可接受范围内
        assertTrue(loadingTime < 1000, "加载20个备份文件应该在1秒内完成")
    }

    /**
     * 测试备份文件预览性能
     */
    @Test
    fun testBackupFilePreviewPerformance() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(1000)
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("preview_performance.json")
        
        // 测量预览备份文件的时间
        val previewTime = measureTimeMillis {
            val fileContent = backupFile.readText()
            val previewContent = if (fileContent.length > 1000) {
                fileContent.substring(0, 1000) + "..."
            } else {
                fileContent
            }
        }
        
        println("预览大型备份文件耗时: $previewTime 毫秒")
        
        // 验证性能在可接受范围内
        assertTrue(previewTime < 1000, "预览大型备份文件应该在1秒内完成")
    }

    /**
     * 测试备份文件验证性能
     */
    @Test
    fun testBackupFileValidationPerformance() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(1000)
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("validation_performance.json")
        
        // 测量验证备份文件的时间
        val validationTime = measureTimeMillis {
            dataExportImportRepository.validateBackupFile(backupFile)
        }
        
        println("验证大型备份文件耗时: $validationTime 毫秒")
        
        // 验证性能在可接受范围内
        assertTrue(validationTime < 5000, "验证大型备份文件应该在5秒内完成")
    }

    /**
     * 测试内存使用优化
     */
    @Test
    fun testMemoryUsageOptimization() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(5000)
        
        // 记录初始内存使用
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("memory_usage_test.json")
        
        // 记录备份后内存使用
        val afterBackupMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // 清空数据库
        db.clearAllTables()
        
        // 从备份恢复
        databaseRecoveryManager.restoreFromBackup(backupFile)
        
        // 记录恢复后内存使用
        val afterRestoreMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // 强制垃圾回收
        System.gc()
        Thread.sleep(1000)
        
        // 记录垃圾回收后内存使用
        val afterGcMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        println("初始内存使用: ${initialMemory / 1024 / 1024} MB")
        println("备份后内存使用: ${afterBackupMemory / 1024 / 1024} MB")
        println("恢复后内存使用: ${afterRestoreMemory / 1024 / 1024} MB")
        println("垃圾回收后内存使用: ${afterGcMemory / 1024 / 1024} MB")
        
        // 验证内存使用在可接受范围内
        assertTrue(afterGcMemory - initialMemory < 50 * 1024 * 1024, "备份和恢复操作不应导致超过50MB的内存泄漏")
    }

    /**
     * 测试电池使用优化
     */
    @Test
    fun testBatteryUsageOptimization() = runBlocking {
        // 生成大量测试数据
        generateLargeTestData(1000)
        
        // 测量备份和恢复的总时间
        val totalTime = measureTimeMillis {
            // 创建备份
            val backupFile = databaseRecoveryManager.createManualBackup("battery_usage_test.json")
            
            // 清空数据库
            db.clearAllTables()
            
            // 从备份恢复
            databaseRecoveryManager.restoreFromBackup(backupFile)
        }
        
        println("备份和恢复1000条交易记录总耗时: $totalTime 毫秒")
        
        // 验证总时间在可接受范围内
        assertTrue(totalTime < 15000, "备份和恢复1000条交易记录总耗时应该在15秒内完成")
    }
}
