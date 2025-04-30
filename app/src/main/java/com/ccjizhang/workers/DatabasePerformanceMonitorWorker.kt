package com.ccjizhang.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.ccjizhang.utils.DatabasePerformanceMonitor
import com.ccjizhang.utils.GrayscaleReleaseConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 数据库性能监控工作任务
 * 定期收集数据库性能指标并生成报告
 */
@HiltWorker
class DatabasePerformanceMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val databasePerformanceMonitor: DatabasePerformanceMonitor,
    private val grayscaleReleaseConfig: GrayscaleReleaseConfig
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DatabasePerformanceMonitorWorker"
        private const val WORK_NAME = "database_performance_monitor_work"
        
        /**
         * 调度器类，用于安排定期性能监控任务
         */
        class Scheduler(private val context: Context) {
            /**
             * 安排定期性能监控任务
             * @param intervalHours 间隔小时数，默认24小时
             */
            fun schedulePeriodicMonitoring(intervalHours: Int = 24) {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true) // 电池电量不低时执行
                    .setRequiresDeviceIdle(true) // 设备空闲时执行
                    .build()
                
                val monitoringWorkRequest = PeriodicWorkRequestBuilder<DatabasePerformanceMonitorWorker>(
                    intervalHours.toLong(), TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()
                
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // 如果已存在，保留现有任务
                    monitoringWorkRequest
                )
                
                Log.i(TAG, "已安排定期数据库性能监控任务，间隔: $intervalHours 小时")
            }
            
            /**
             * 取消定期性能监控任务
             */
            fun cancelPeriodicMonitoring() {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.i(TAG, "已取消定期数据库性能监控任务")
            }
            
            /**
             * 立即执行一次性能监控任务
             */
            fun runOneTimeMonitoring() {
                val monitoringWorkRequest = OneTimeWorkRequestBuilder<DatabasePerformanceMonitorWorker>()
                    .build()
                
                WorkManager.getInstance(context).enqueue(monitoringWorkRequest)
                Log.i(TAG, "已安排一次性数据库性能监控任务")
            }
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始执行数据库性能监控任务")
            
            // 检查是否启用了数据库监控功能
            if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING)) {
                Log.i(TAG, "数据库监控功能未启用，跳过性能监控")
                return@withContext Result.success()
            }
            
            // 生成性能报告
            val reportFile = databasePerformanceMonitor.generatePerformanceReport()
            
            if (reportFile != null && reportFile.exists()) {
                Log.i(TAG, "数据库性能报告生成成功: ${reportFile.path}")
                
                // 可以在这里添加代码，将报告发送到服务器或通知用户
                
                return@withContext Result.success()
            } else {
                Log.e(TAG, "数据库性能报告生成失败")
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据库性能监控任务执行失败", e)
            return@withContext Result.failure()
        }
    }
}
