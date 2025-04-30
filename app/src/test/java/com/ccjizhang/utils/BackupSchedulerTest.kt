package com.ccjizhang.utils

import android.content.Context
import androidx.work.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BackupSchedulerTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var workManager: WorkManager

    @MockK
    lateinit var workRequest: PeriodicWorkRequest

    @MockK
    lateinit var workRequestBuilder: PeriodicWorkRequest.Builder

    @MockK
    lateinit var operation: Operation

    private lateinit var autoBackupScheduler: AutoBackupWorker.Scheduler
    private lateinit var backupReminderScheduler: BackupReminderWorker.Scheduler

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // 模拟WorkManager
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns operation
        every { workManager.cancelUniqueWork(any()) } returns operation
        every { operation.state } returns mockk()

        // 模拟WorkRequest.Builder
        mockkConstructor(PeriodicWorkRequest.Builder::class)
        every { anyConstructed<PeriodicWorkRequest.Builder>().setConstraints(any()) } returns workRequestBuilder
        every { anyConstructed<PeriodicWorkRequest.Builder>().setInputData(any()) } returns workRequestBuilder
        every { anyConstructed<PeriodicWorkRequest.Builder>().build() } returns workRequest

        // 创建测试对象
        autoBackupScheduler = AutoBackupWorker.Scheduler(context)
        backupReminderScheduler = BackupReminderWorker.Scheduler(context)
    }

    @Test
    fun `scheduleAutoBackup应该创建正确的WorkRequest并调用WorkManager`() {
        // 调用调度方法
        autoBackupScheduler.scheduleAutoBackup(7)

        // 验证WorkRequest.Builder被正确创建
        verify {
            PeriodicWorkRequest.Builder(
                AutoBackupWorker::class.java,
                7,
                TimeUnit.DAYS
            )
        }

        // 验证WorkManager被调用
        verify {
            workManager.enqueueUniquePeriodicWork(
                "auto_backup_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    @Test
    fun `cancelAutoBackup应该调用WorkManager取消工作`() {
        // 调用取消方法
        autoBackupScheduler.cancelAutoBackup()

        // 验证WorkManager被调用
        verify {
            workManager.cancelUniqueWork("auto_backup_work")
        }
    }

    @Test
    fun `scheduleBackupReminder应该创建正确的WorkRequest并调用WorkManager`() {
        // 调用调度方法
        backupReminderScheduler.scheduleBackupReminder()

        // 验证WorkRequest.Builder被正确创建
        verify {
            PeriodicWorkRequest.Builder(
                BackupReminderWorker::class.java,
                1,
                TimeUnit.DAYS
            )
        }

        // 验证WorkManager被调用
        verify {
            workManager.enqueueUniquePeriodicWork(
                "backup_reminder_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    @Test
    fun `cancelBackupReminder应该调用WorkManager取消工作`() {
        // 调用取消方法
        backupReminderScheduler.cancelBackupReminder()

        // 验证WorkManager被调用
        verify {
            workManager.cancelUniqueWork("backup_reminder_work")
        }
    }

    @Test
    fun `AutoBackupWorker应该在doWork中创建备份并返回成功`() = runTest {
        // 模拟DatabaseRecoveryManager
        val databaseRecoveryManager = mockk<DatabaseRecoveryManager>()
        every { databaseRecoveryManager.createScheduledBackup() } returns mockk()

        // 模拟WorkerParameters
        val workerParams = mockk<WorkerParameters>()
        every { workerParams.taskExecutor } returns mockk()

        // 创建Worker
        val worker = spyk(AutoBackupWorker(context, workerParams))
        every { worker.getDatabaseRecoveryManager() } returns databaseRecoveryManager

        // 执行工作
        val result = worker.doWork()

        // 验证备份被创建
        verify { databaseRecoveryManager.createScheduledBackup() }

        // 验证结果
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `BackupReminderWorker应该在doWork中检查备份提醒并返回成功`() = runTest {
        // 模拟BackupRestoreViewModel
        val viewModel = mockk<com.ccjizhang.ui.viewmodels.BackupRestoreViewModel>()
        every { viewModel.checkBackupReminder(any()) } just Runs

        // 模拟WorkerParameters
        val workerParams = mockk<WorkerParameters>()
        every { workerParams.taskExecutor } returns mockk()

        // 创建Worker
        val worker = spyk(BackupReminderWorker(context, workerParams))
        every { worker.getBackupRestoreViewModel() } returns viewModel

        // 执行工作
        val result = worker.doWork()

        // 验证备份提醒被检查
        verify { viewModel.checkBackupReminder(any()) }

        // 验证结果
        assertEquals(ListenableWorker.Result.success(), result)
    }
}
