package com.ccjizhang.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.ccjizhang.data.repository.DataExportImportRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 备份恢复兼容性测试
 * 测试在不同Android版本和存储模型上的兼容性
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreCompatibilityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var databaseRecoveryManager: DatabaseRecoveryManager
    private lateinit var backupFolder: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // 创建备份文件夹
        backupFolder = tempFolder.newFolder("backups")
        
        // 初始化数据库恢复管理器
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            mockk<DataExportImportRepository>() // 模拟DataExportImportRepository
        )
    }

    @After
    fun tearDown() {
        // 清理测试文件
        backupFolder.listFiles()?.forEach { it.delete() }
    }

    /**
     * 测试在所有Android版本上的基本备份功能
     */
    @Test
    fun testBasicBackupFunctionality() {
        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<DataExportImportRepository>()
        every { dataExportImportRepository.exportDataToJsonString() } returns "{\"test\":\"data\"}"
        
        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_compatibility_backup.json")
        
        // 验证备份文件创建成功
        assertNotNull(backupFile, "备份文件应该创建成功")
        assertTrue(backupFile.exists(), "备份文件应该存在")
        assertTrue(backupFile.length() > 0, "备份文件应该有内容")
    }

    /**
     * 测试在Android 10及以上版本的分区存储兼容性
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testScopedStorageCompatibility() {
        // 在Android 10及以上版本，应用只能访问自己的私有目录和通过SAF获取的目录
        // 验证可以在应用私有目录创建备份
        val privateDir = context.getExternalFilesDir(null)
        assertNotNull(privateDir, "应用私有目录不应为空")
        assertTrue(privateDir.exists(), "应用私有目录应该存在")
        assertTrue(privateDir.canWrite(), "应用私有目录应该可写")
        
        // 创建测试文件
        val testFile = File(privateDir, "scoped_storage_test.txt")
        testFile.writeText("测试分区存储")
        
        // 验证文件创建成功
        assertTrue(testFile.exists(), "在应用私有目录中应该可以创建文件")
        assertEquals("测试分区存储", testFile.readText(), "文件内容应该正确")
        
        // 清理测试文件
        testFile.delete()
    }

    /**
     * 测试在Android 9及以下版本的传统存储兼容性
     */
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun testLegacyStorageCompatibility() {
        // 在Android 9及以下版本，应用可以访问外部存储
        // 验证可以在外部存储创建备份
        val externalDir = Environment.getExternalStorageDirectory()
        assertNotNull(externalDir, "外部存储目录不应为空")
        
        // 检查是否有外部存储访问权限
        if (externalDir.exists() && externalDir.canWrite()) {
            // 创建测试文件
            val testFile = File(externalDir, "legacy_storage_test.txt")
            testFile.writeText("测试传统存储")
            
            // 验证文件创建成功
            assertTrue(testFile.exists(), "在外部存储中应该可以创建文件")
            assertEquals("测试传统存储", testFile.readText(), "文件内容应该正确")
            
            // 清理测试文件
            testFile.delete()
        }
    }

    /**
     * 测试不同文件系统的兼容性
     */
    @Test
    fun testFileSystemCompatibility() = runBlocking {
        // 测试不同的文件名格式
        val fileNames = listOf(
            "normal_backup.json",
            "backup with spaces.json",
            "backup_with_特殊字符.json",
            "backup_with_numbers_123.json",
            "backup_with_symbols_!@#$.json"
        )
        
        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<DataExportImportRepository>()
        every { dataExportImportRepository.exportDataToJsonString() } returns "{\"test\":\"data\"}"
        
        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )
        
        // 测试每个文件名
        for (fileName in fileNames) {
            try {
                // 创建备份
                val backupFile = databaseRecoveryManager.createManualBackup(fileName)
                
                // 验证备份文件创建成功
                assertNotNull(backupFile, "备份文件 $fileName 应该创建成功")
                assertTrue(backupFile.exists(), "备份文件 $fileName 应该存在")
                
                // 清理测试文件
                backupFile.delete()
            } catch (e: Exception) {
                throw AssertionError("文件名 $fileName 测试失败: ${e.message}", e)
            }
        }
    }

    /**
     * 测试大文件处理兼容性
     */
    @Test
    fun testLargeFileCompatibility() = runBlocking {
        // 创建大型JSON字符串
        val largeJson = buildString {
            append("{\"data\":[")
            repeat(10000) { i ->
                if (i > 0) append(",")
                append("{\"id\":$i,\"name\":\"Item $i\",\"value\":\"${java.util.UUID.randomUUID()}\"}")
            }
            append("]}")
        }
        
        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<DataExportImportRepository>()
        every { dataExportImportRepository.exportDataToJsonString() } returns largeJson
        coEvery { dataExportImportRepository.importDataFromJsonString(any()) } returns true
        
        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )
        
        // 创建备份
        val backupFile = databaseRecoveryManager.createManualBackup("large_backup.json")
        
        // 验证备份文件创建成功
        assertNotNull(backupFile, "大型备份文件应该创建成功")
        assertTrue(backupFile.exists(), "大型备份文件应该存在")
        assertTrue(backupFile.length() > 1000000, "大型备份文件应该大于1MB")
        
        // 测试从大型备份恢复
        val restoreResult = databaseRecoveryManager.restoreFromBackup(backupFile)
        assertTrue(restoreResult, "从大型备份恢复应该成功")
        
        // 清理测试文件
        backupFile.delete()
    }

    /**
     * 测试不同存储权限模型的兼容性
     */
    @Test
    fun testStoragePermissionModelCompatibility() {
        // 检查应用私有目录访问
        val privateDir = context.getExternalFilesDir(null)
        assertNotNull(privateDir, "应用私有目录不应为空")
        assertTrue(privateDir.exists(), "应用私有目录应该存在")
        assertTrue(privateDir.canWrite(), "应用私有目录应该可写")
        
        // 检查缓存目录访问
        val cacheDir = context.externalCacheDir
        assertNotNull(cacheDir, "应用缓存目录不应为空")
        assertTrue(cacheDir.exists(), "应用缓存目录应该存在")
        assertTrue(cacheDir.canWrite(), "应用缓存目录应该可写")
        
        // 检查内部存储访问
        val internalDir = context.filesDir
        assertNotNull(internalDir, "应用内部存储目录不应为空")
        assertTrue(internalDir.exists(), "应用内部存储目录应该存在")
        assertTrue(internalDir.canWrite(), "应用内部存储目录应该可写")
    }
}
