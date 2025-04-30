package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.ui.common.OperationResult
import com.ccjizhang.utils.AutoBackupWorker
import com.ccjizhang.utils.BackupReminderWorker
import com.ccjizhang.utils.DatabaseRecoveryManager
import com.ccjizhang.utils.NotificationHelper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BackupRestoreViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var dataExportImportRepository: DataExportImportRepository

    @MockK
    lateinit var autoBackupScheduler: AutoBackupWorker.Scheduler

    @MockK
    lateinit var databaseRecoveryManager: DatabaseRecoveryManager

    @MockK
    lateinit var notificationHelper: NotificationHelper

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var viewModel: BackupRestoreViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // 模拟SharedPreferences
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putBoolean(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putInt(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs

        // 模拟初始设置
        every { sharedPreferences.getBoolean("auto_backup_enabled", any()) } returns false
        every { sharedPreferences.getInt("backup_interval_days", any()) } returns 3
        every { sharedPreferences.getString("custom_backup_path", any()) } returns ""
        every { sharedPreferences.getLong("last_backup_time", any()) } returns 0L
        every { sharedPreferences.getBoolean("backup_reminder_enabled", any()) } returns false
        every { sharedPreferences.getInt("backup_reminder_days", any()) } returns 7
        every { sharedPreferences.getString("custom_backup_uri", any()) } returns null

        // 模拟备份文件列表
        every { databaseRecoveryManager.getAllBackups() } returns emptyList()

        // 创建ViewModel
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态应该正确加载`() = runTest {
        // 验证初始状态
        val state = viewModel.backupRestoreState.value
        assertFalse(state.isAutoBackupEnabled)
        assertEquals(3, state.backupIntervalDays)
        assertEquals("", state.customBackupPath)
        assertEquals(null, state.customBackupUri)
        assertEquals("", state.displayPath)
        assertEquals(0L, state.lastBackupTime)
        assertFalse(state.isBackupReminderEnabled)
        assertEquals(7, state.backupReminderDays)
        assertEquals(emptyList(), state.backupFiles)
    }

    @Test
    fun `启用自动备份应该更新状态并调度备份任务`() = runTest {
        // 模拟调度器
        every { autoBackupScheduler.scheduleAutoBackup(any()) } just Runs

        // 启用自动备份
        viewModel.setAutoBackupEnabled(true)
        testScheduler.advanceUntilIdle()

        // 验证状态更新
        val state = viewModel.backupRestoreState.value
        assertTrue(state.isAutoBackupEnabled)

        // 验证调度器被调用
        verify { autoBackupScheduler.scheduleAutoBackup(3) }
        verify { sharedPreferencesEditor.putBoolean("auto_backup_enabled", true) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `禁用自动备份应该更新状态并取消备份任务`() = runTest {
        // 先启用自动备份
        every { autoBackupScheduler.scheduleAutoBackup(any()) } just Runs
        every { sharedPreferences.getBoolean("auto_backup_enabled", any()) } returns true
        every { autoBackupScheduler.cancelAutoBackup() } just Runs

        // 重新创建ViewModel以使用新的模拟设置
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )

        // 禁用自动备份
        viewModel.setAutoBackupEnabled(false)
        testScheduler.advanceUntilIdle()

        // 验证状态更新
        val state = viewModel.backupRestoreState.value
        assertFalse(state.isAutoBackupEnabled)

        // 验证调度器被调用
        verify { autoBackupScheduler.cancelAutoBackup() }
        verify { sharedPreferencesEditor.putBoolean("auto_backup_enabled", false) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `更新备份频率应该更新状态并重新调度备份任务`() = runTest {
        // 模拟启用自动备份
        every { sharedPreferences.getBoolean("auto_backup_enabled", any()) } returns true
        every { autoBackupScheduler.scheduleAutoBackup(any()) } just Runs

        // 重新创建ViewModel以使用新的模拟设置
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )

        // 更新备份频率
        viewModel.updateBackupInterval(7)
        testScheduler.advanceUntilIdle()

        // 验证状态更新
        val state = viewModel.backupRestoreState.value
        assertEquals(7, state.backupIntervalDays)

        // 验证调度器被调用
        verify { autoBackupScheduler.scheduleAutoBackup(7) }
        verify { sharedPreferencesEditor.putInt("backup_interval_days", 7) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `创建手动备份应该调用数据库恢复管理器并更新状态`() = runTest {
        // 创建测试备份文件
        val backupFile = tempFolder.newFile("test_backup.json")
        every { databaseRecoveryManager.createManualBackup(any()) } returns backupFile
        every { databaseRecoveryManager.getAllBackups() } returns listOf(backupFile)

        // 创建手动备份
        viewModel.createManualBackup()
        testScheduler.advanceUntilIdle()

        // 验证数据库恢复管理器被调用
        verify { databaseRecoveryManager.createManualBackup(any()) }

        // 验证备份文件列表被更新
        val state = viewModel.backupRestoreState.value
        assertEquals(1, state.backupFiles.size)
        assertEquals(backupFile, state.backupFiles[0])
    }

    @Test
    fun `删除备份文件应该调用数据库恢复管理器并更新状态`() = runTest {
        // 创建测试备份文件
        val backupFile = tempFolder.newFile("test_backup.json")
        every { databaseRecoveryManager.getAllBackups() } returns listOf(backupFile) andThen emptyList()
        every { databaseRecoveryManager.deleteBackupFile(any()) } returns true

        // 先加载备份文件列表
        viewModel.loadBackupFiles()
        testScheduler.advanceUntilIdle()

        // 删除备份文件
        viewModel.deleteBackupFile(backupFile)
        testScheduler.advanceUntilIdle()

        // 验证数据库恢复管理器被调用
        verify { databaseRecoveryManager.deleteBackupFile(backupFile) }

        // 验证备份文件列表被更新
        val state = viewModel.backupRestoreState.value
        assertEquals(0, state.backupFiles.size)
    }

    @Test
    fun `从备份恢复应该调用数据库恢复管理器并发出成功结果`() = runTest {
        // 创建测试备份文件
        val backupFile = tempFolder.newFile("test_backup.json")
        every { databaseRecoveryManager.restoreFromBackup(any()) } returns true

        // 从备份恢复
        viewModel.restoreFromBackup(backupFile)
        testScheduler.advanceUntilIdle()

        // 验证数据库恢复管理器被调用
        verify { databaseRecoveryManager.restoreFromBackup(backupFile) }

        // 验证操作结果
        val result = viewModel.operationResult.first()
        assertTrue(result is OperationResult.Success)
        assertEquals("数据恢复成功", (result as OperationResult.Success).message)
    }

    @Test
    fun `启用备份提醒应该更新状态并调度提醒任务`() = runTest {
        // 模拟提醒调度器
        val reminderScheduler = mockk<BackupReminderWorker.Scheduler>()
        every { reminderScheduler.scheduleBackupReminder() } just Runs
        mockkConstructor(BackupReminderWorker.Scheduler::class)
        every { anyConstructed<BackupReminderWorker.Scheduler>().scheduleBackupReminder() } just Runs

        // 启用备份提醒
        viewModel.setBackupReminder(true)
        testScheduler.advanceUntilIdle()

        // 验证状态更新
        val state = viewModel.backupRestoreState.value
        assertTrue(state.isBackupReminderEnabled)

        // 验证调度器被调用
        verify { anyConstructed<BackupReminderWorker.Scheduler>().scheduleBackupReminder() }
        verify { sharedPreferencesEditor.putBoolean("backup_reminder_enabled", true) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `更新备份提醒天数应该更新状态`() = runTest {
        // 模拟启用备份提醒
        every { sharedPreferences.getBoolean("backup_reminder_enabled", any()) } returns true
        mockkConstructor(BackupReminderWorker.Scheduler::class)
        every { anyConstructed<BackupReminderWorker.Scheduler>().scheduleBackupReminder() } just Runs

        // 重新创建ViewModel以使用新的模拟设置
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )

        // 更新备份提醒天数
        viewModel.updateBackupReminderDays(14)
        testScheduler.advanceUntilIdle()

        // 验证状态更新
        val state = viewModel.backupRestoreState.value
        assertEquals(14, state.backupReminderDays)

        // 验证设置被保存
        verify { sharedPreferencesEditor.putInt("backup_reminder_days", 14) }
        verify { sharedPreferencesEditor.apply() }

        // 验证调度器被调用（因为提醒已启用）
        verify { anyConstructed<BackupReminderWorker.Scheduler>().scheduleBackupReminder() }
    }

    @Test
    fun `检查备份提醒应该在超过指定天数时发送通知`() = runTest {
        // 模拟上次备份时间为14天前
        val currentTime = System.currentTimeMillis()
        val lastBackupTime = currentTime - (14 * 24 * 60 * 60 * 1000)
        every { sharedPreferences.getLong("last_backup_time", any()) } returns lastBackupTime

        // 模拟通知助手
        every { notificationHelper.showBackupReminderNotification(any(), any(), any()) } just Runs

        // 重新创建ViewModel以使用新的模拟设置
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )

        // 检查备份提醒
        viewModel.checkBackupReminder(7)
        testScheduler.advanceUntilIdle()

        // 验证通知被发送
        verify { notificationHelper.showBackupReminderNotification(any(), any(), any()) }
    }

    @Test
    fun `检查备份提醒应该在未超过指定天数时不发送通知`() = runTest {
        // 模拟上次备份时间为3天前
        val currentTime = System.currentTimeMillis()
        val lastBackupTime = currentTime - (3 * 24 * 60 * 60 * 1000)
        every { sharedPreferences.getLong("last_backup_time", any()) } returns lastBackupTime

        // 模拟通知助手
        every { notificationHelper.showBackupReminderNotification(any(), any(), any()) } just Runs

        // 重新创建ViewModel以使用新的模拟设置
        viewModel = BackupRestoreViewModel(
            context,
            dataExportImportRepository,
            autoBackupScheduler,
            databaseRecoveryManager,
            notificationHelper
        )

        // 检查备份提醒
        viewModel.checkBackupReminder(7)
        testScheduler.advanceUntilIdle()

        // 验证通知未被发送
        verify(exactly = 0) { notificationHelper.showBackupReminderNotification(any(), any(), any()) }
    }
}
