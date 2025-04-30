package com.ccjizhang.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ccjizhang.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 备份提醒工作器
 * 检查上次备份时间，如果超过指定天数则发送通知提醒用户备份
 */
@HiltWorker
class BackupReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BackupReminderWorker"
        private const val PREFS_NAME = "backup_restore_prefs"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val DEFAULT_REMINDER_DAYS = 7 // 默认7天未备份提醒
        private const val WORK_NAME = "backup_reminder_work"
    }

    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始检查备份提醒")
            
            // 获取上次备份时间
            val lastBackupTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
            
            // 如果从未备份过，或者上次备份时间超过指定天数，发送通知
            if (lastBackupTime == 0L || isBackupOverdue(lastBackupTime)) {
                showBackupReminderNotification()
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "备份提醒检查失败")
            Result.failure()
        }
    }

    /**
     * 检查上次备份是否已过期（超过指定天数）
     */
    private fun isBackupOverdue(lastBackupTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val daysSinceLastBackup = (currentTime - lastBackupTime) / (1000 * 60 * 60 * 24)
        return daysSinceLastBackup >= DEFAULT_REMINDER_DAYS
    }

    /**
     * 显示备份提醒通知
     */
    private fun showBackupReminderNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = NotificationHelper.ACTION_OPEN_SETTINGS
            putExtra("OPEN_BACKUP_SCREEN", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        notificationHelper.showBackupReminderNotification(
            "您已超过 $DEFAULT_REMINDER_DAYS 天未备份数据",
            "为避免数据丢失，建议立即备份您的数据",
            intent
        )
    }

    /**
     * 备份提醒调度器
     */
    class Scheduler(private val context: Context) {
        /**
         * 安排备份提醒检查任务
         * 每天检查一次是否需要提醒用户备份
         */
        fun scheduleBackupReminder() {
            val reminderRequest = PeriodicWorkRequestBuilder<BackupReminderWorker>(
                1, TimeUnit.DAYS
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
            
            Timber.d("已安排备份提醒检查任务")
        }
        
        /**
         * 取消备份提醒检查任务
         */
        fun cancelBackupReminder() {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("已取消备份提醒检查任务")
        }
    }
}
