package com.ccjizhang.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BackupFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var documentFile: DocumentFile

    @MockK
    lateinit var uri: Uri

    @MockK
    lateinit var inputStream: InputStream

    @MockK
    lateinit var outputStream: OutputStream

    private lateinit var backupFolder: File
    private lateinit var databaseRecoveryManager: DatabaseRecoveryManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        backupFolder = tempFolder.newFolder("backups")

        // 模拟Context
        every { context.getExternalFilesDir(any()) } returns backupFolder
        every { context.contentResolver } returns mockk()

        // 模拟DocumentFile
        every { DocumentFile.fromTreeUri(context, any()) } returns documentFile
        every { documentFile.name } returns "test_backup_folder"
        every { documentFile.exists() } returns true
        every { documentFile.isDirectory } returns true
        every { documentFile.createFile(any(), any()) } returns documentFile
        every { documentFile.findFile(any()) } returns documentFile
        every { documentFile.delete() } returns true

        // 模拟Uri
        every { uri.toString() } returns "content://test/backup"
        every { uri.lastPathSegment } returns "backup"

        // 模拟流
        every { context.contentResolver.openInputStream(any()) } returns inputStream
        every { context.contentResolver.openOutputStream(any()) } returns outputStream
        every { inputStream.read(any()) } returns -1
        every { outputStream.write(any<ByteArray>()) } just Runs
        every { outputStream.flush() } just Runs
        every { outputStream.close() } just Runs
        every { inputStream.close() } just Runs

        // 创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            mockk() // 模拟DataExportImportRepository
        )
    }

    @Test
    fun `getAllBackups应该返回按时间排序的备份文件列表`() {
        // 创建测试备份文件
        val file1 = File(backupFolder, "backup_1.json").apply {
            createNewFile()
            setLastModified(System.currentTimeMillis() - 1000)
        }
        val file2 = File(backupFolder, "backup_2.json").apply {
            createNewFile()
            setLastModified(System.currentTimeMillis())
        }
        val file3 = File(backupFolder, "not_a_backup.txt").apply {
            createNewFile()
        }

        // 获取备份文件列表
        val backups = databaseRecoveryManager.getAllBackups()

        // 验证结果
        assertEquals(2, backups.size)
        assertEquals(file2.name, backups[0].name) // 最新的文件应该在前面
        assertEquals(file1.name, backups[1].name)
    }

    @Test
    fun `createManualBackup应该创建新的备份文件`() {
        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<com.ccjizhang.data.repository.DataExportImportRepository>()
        every { dataExportImportRepository.exportDataToJsonString() } returns "{\"test\":\"data\"}"

        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )

        // 创建手动备份
        val backupFile = databaseRecoveryManager.createManualBackup("test_backup.json")

        // 验证结果
        assertTrue(backupFile != null)
        assertTrue(backupFile!!.exists())
        assertEquals("test_backup.json", backupFile.name)
    }

    @Test
    fun `deleteBackupFile应该删除指定的备份文件`() {
        // 创建测试备份文件
        val file = File(backupFolder, "backup_to_delete.json").apply {
            createNewFile()
        }

        // 删除备份文件
        val result = databaseRecoveryManager.deleteBackupFile(file)

        // 验证结果
        assertTrue(result)
        assertFalse(file.exists())
    }

    @Test
    fun `cleanupOldBackups应该只保留指定数量的最新备份文件`() {
        // 创建多个测试备份文件
        val files = mutableListOf<File>()
        for (i in 1..10) {
            val file = File(backupFolder, "backup_$i.json").apply {
                createNewFile()
                setLastModified(System.currentTimeMillis() - (i * 1000L))
            }
            files.add(file)
        }

        // 清理旧备份
        databaseRecoveryManager.cleanupOldBackups(5)

        // 验证结果
        val remainingFiles = backupFolder.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
        assertEquals(5, remainingFiles.size)
        
        // 验证保留的是最新的文件
        for (i in 1..5) {
            assertTrue(remainingFiles.any { it.name == "backup_$i.json" })
        }
        
        // 验证旧文件被删除
        for (i in 6..10) {
            assertFalse(remainingFiles.any { it.name == "backup_$i.json" })
        }
    }

    @Test
    fun `restoreFromBackup应该从备份文件恢复数据`() = runTest {
        // 创建测试备份文件
        val file = File(backupFolder, "backup_to_restore.json").apply {
            FileOutputStream(this).use { it.write("{\"test\":\"data\"}".toByteArray()) }
        }

        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<com.ccjizhang.data.repository.DataExportImportRepository>()
        coEvery { dataExportImportRepository.importDataFromJsonString(any()) } returns true

        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )

        // 从备份恢复
        val result = databaseRecoveryManager.restoreFromBackup(file)

        // 验证结果
        assertTrue(result)
        coVerify { dataExportImportRepository.importDataFromJsonString(any()) }
    }

    @Test
    fun `createScheduledBackup应该创建新的备份文件并清理旧备份`() {
        // 创建多个测试备份文件
        for (i in 1..10) {
            File(backupFolder, "backup_$i.json").apply {
                createNewFile()
                setLastModified(System.currentTimeMillis() - (i * 1000L))
            }
        }

        // 模拟DataExportImportRepository
        val dataExportImportRepository = mockk<com.ccjizhang.data.repository.DataExportImportRepository>()
        every { dataExportImportRepository.exportDataToJsonString() } returns "{\"test\":\"data\"}"

        // 重新创建测试对象
        databaseRecoveryManager = DatabaseRecoveryManager(
            context,
            dataExportImportRepository
        )

        // 创建计划备份
        val backupFile = databaseRecoveryManager.createScheduledBackup()

        // 验证结果
        assertTrue(backupFile != null)
        assertTrue(backupFile!!.exists())
        
        // 验证旧备份被清理
        val remainingFiles = backupFolder.listFiles()?.filter { it.name.endsWith(".json") } ?: emptyList()
        assertEquals(6, remainingFiles.size) // 5个旧文件 + 1个新文件
    }
}
