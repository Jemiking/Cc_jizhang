package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 备份恢复功能测试
 * 测试实际的备份创建、恢复和验证功能
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreFunctionalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var dataExportImportRepository: DataExportImportRepository
    private lateinit var databaseRecoveryManager: DatabaseRecoveryManager
    private lateinit var prefs: SharedPreferences
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
        
        // 初始化SharedPreferences
        prefs = context.getSharedPreferences("backup_restore_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // 生成测试数据
        generateTestData()
    }

    @After
    fun tearDown() {
        db.close()
        prefs.edit().clear().apply()
    }

    /**
     * 生成测试数据
     */
    private fun generateTestData() = runBlocking {
        // 创建测试账户
        val account = Account(
            id = 1,
            name = "测试账户",
            balance = 1000.0,
            type = "现金",
            color = "#FF0000",
            icon = "cash",
            isDefault = true,
            sortOrder = 0,
            isArchived = false
        )
        accountRepository.insertAccount(account)
        
        // 创建测试分类
        val category = Category(
            id = 1,
            name = "测试分类",
            type = TransactionType.EXPENSE,
            color = "#00FF00",
            icon = "food",
            sortOrder = 0,
            level = 1,
            parentId = null
        )
        categoryRepository.insertCategory(category)
        
        // 创建测试交易
        val transaction = Transaction(
            id = 1,
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "测试交易",
            categoryId = category.id,
            accountId = account.id,
            toAccountId = null,
            isRecurring = false
        )
        transactionRepository.addTransaction(transaction)
    }

    /**
     * 测试手动备份创建功能
     */
    @Test
    fun testManualBackupCreation() = runBlocking {
        // 创建手动备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_manual_backup.json")
        
        // 验证备份文件创建成功
        assertNotNull(backupFile, "备份文件应该创建成功")
        assertTrue(backupFile.exists(), "备份文件应该存在")
        assertTrue(backupFile.length() > 0, "备份文件应该有内容")
        
        // 验证备份文件包含正确的数据
        val fileContent = backupFile.readText()
        assertTrue(fileContent.contains("测试账户"), "备份文件应该包含账户数据")
        assertTrue(fileContent.contains("测试分类"), "备份文件应该包含分类数据")
        assertTrue(fileContent.contains("测试交易"), "备份文件应该包含交易数据")
    }

    /**
     * 测试自动备份创建功能
     */
    @Test
    fun testScheduledBackupCreation() = runBlocking {
        // 创建计划备份
        val backupFile = databaseRecoveryManager.createScheduledBackup()
        
        // 验证备份文件创建成功
        assertNotNull(backupFile, "备份文件应该创建成功")
        assertTrue(backupFile.exists(), "备份文件应该存在")
        
        // 验证上次备份时间被更新
        val lastBackupTime = prefs.getLong("last_backup_time", 0L)
        assertTrue(lastBackupTime > 0, "上次备份时间应该被更新")
        
        // 验证备份文件包含正确的数据
        val fileContent = backupFile.readText()
        assertTrue(fileContent.contains("测试账户"), "备份文件应该包含账户数据")
        assertTrue(fileContent.contains("测试分类"), "备份文件应该包含分类数据")
        assertTrue(fileContent.contains("测试交易"), "备份文件应该包含交易数据")
    }

    /**
     * 测试从备份恢复功能
     */
    @Test
    fun testRestoreFromBackup() = runBlocking {
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_restore_backup.json")
        
        // 清空数据库
        db.clearAllTables()
        
        // 验证数据库已清空
        val accountsBeforeRestore = accountRepository.getAllAccountsSync()
        val categoriesBeforeRestore = categoryRepository.getAllCategoriesSync()
        val transactionsBeforeRestore = transactionRepository.getAllTransactionsSync()
        assertEquals(0, accountsBeforeRestore.size, "数据库应该已清空")
        assertEquals(0, categoriesBeforeRestore.size, "数据库应该已清空")
        assertEquals(0, transactionsBeforeRestore.size, "数据库应该已清空")
        
        // 从备份恢复
        val restoreResult = databaseRecoveryManager.restoreFromBackup(backupFile)
        
        // 验证恢复成功
        assertTrue(restoreResult, "从备份恢复应该成功")
        
        // 验证数据已恢复
        val accountsAfterRestore = accountRepository.getAllAccountsSync()
        val categoriesAfterRestore = categoryRepository.getAllCategoriesSync()
        val transactionsAfterRestore = transactionRepository.getAllTransactionsSync()
        assertEquals(1, accountsAfterRestore.size, "应该恢复1个账户")
        assertEquals("测试账户", accountsAfterRestore[0].name, "恢复的账户名称应该正确")
        assertEquals(1, categoriesAfterRestore.size, "应该恢复1个分类")
        assertEquals("测试分类", categoriesAfterRestore[0].name, "恢复的分类名称应该正确")
        assertEquals(1, transactionsAfterRestore.size, "应该恢复1条交易")
        assertEquals("测试交易", transactionsAfterRestore[0].note, "恢复的交易备注应该正确")
    }

    /**
     * 测试备份验证功能
     */
    @Test
    fun testBackupValidation() = runBlocking {
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_validation_backup.json")
        
        // 验证备份文件
        val validationResult = dataExportImportRepository.validateBackupFile(backupFile)
        
        // 验证验证结果
        assertTrue(validationResult.isSuccess, "备份文件验证应该成功")
        val validationData = validationResult.getOrNull()
        assertNotNull(validationData, "验证数据不应为空")
        assertEquals(1, validationData.accountCount, "验证数据应包含1个账户")
        assertEquals(1, validationData.categoryCount, "验证数据应包含1个分类")
        assertEquals(1, validationData.transactionCount, "验证数据应包含1条交易")
    }

    /**
     * 测试多次备份和清理旧备份功能
     */
    @Test
    fun testMultipleBackupsAndCleanup() = runBlocking {
        // 创建多个备份
        val backupFiles = mutableListOf<File>()
        repeat(10) { i ->
            val backupFile = databaseRecoveryManager.createManualBackup("backup_$i.json")
            backupFiles.add(backupFile)
            // 稍微延迟，确保时间戳不同
            Thread.sleep(100)
        }
        
        // 验证创建了多个备份
        val allBackups = databaseRecoveryManager.getAllBackups()
        assertTrue(allBackups.size >= 10, "应该创建了至少10个备份")
        
        // 清理旧备份
        databaseRecoveryManager.cleanupOldBackups(5)
        
        // 验证只保留了最新的备份
        val backupsAfterCleanup = databaseRecoveryManager.getAllBackups()
        assertEquals(5, backupsAfterCleanup.size, "应该只保留5个最新的备份")
        
        // 验证保留的是最新的备份
        for (i in 0 until 4) {
            assertTrue(
                backupsAfterCleanup[i].lastModified() >= backupsAfterCleanup[i + 1].lastModified(),
                "备份应该按时间降序排序"
            )
        }
    }

    /**
     * 测试备份文件管理功能
     */
    @Test
    fun testBackupFileManagement() = runBlocking {
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_management_backup.json")
        
        // 获取所有备份
        val allBackups = databaseRecoveryManager.getAllBackups()
        assertTrue(allBackups.contains(backupFile), "备份文件列表应该包含新创建的备份")
        
        // 删除备份
        val deleteResult = databaseRecoveryManager.deleteBackupFile(backupFile)
        assertTrue(deleteResult, "删除备份文件应该成功")
        
        // 验证备份已删除
        val backupsAfterDelete = databaseRecoveryManager.getAllBackups()
        assertTrue(!backupsAfterDelete.contains(backupFile), "备份文件列表不应包含已删除的备份")
    }
}
