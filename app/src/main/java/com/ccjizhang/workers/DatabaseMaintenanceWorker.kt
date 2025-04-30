package com.ccjizhang.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ccjizhang.data.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 数据库维护工作器
 * 负责执行定期数据库维护任务，如VACUUM、检查点等
 */
@HiltWorker
class DatabaseMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appDatabase: AppDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DatabaseMaintenance"
        private const val WORK_NAME = "database_maintenance_work"
        private const val REPORTS_FOLDER = "db_maintenance_reports"
        
        /**
         * 调度器类，用于安排定期数据库维护任务
         */
        class Scheduler(private val context: Context) {
            /**
             * 安排定期数据库维护任务
             * @param intervalDays 执行间隔天数
             */
            fun schedulePeriodicMaintenance(intervalDays: Int = 7) {
                val maintenanceRequest = PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(
                    intervalDays.toLong(), TimeUnit.DAYS
                )
                .addTag(WORK_NAME)
                .build()
                
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    maintenanceRequest
                )
                
                Log.i(TAG, "已安排定期数据库维护任务，间隔 $intervalDays 天")
            }
            
            /**
             * 取消定期数据库维护任务
             */
            fun cancelPeriodicMaintenance() {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.i(TAG, "已取消定期数据库维护任务")
            }
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始执行数据库维护任务")
            
            // 执行VACUUM操作
            val vacuumResult = performVacuum()
            
            // 执行检查点操作
            val checkpointResult = performCheckpoint()
            
            // 执行数据库文件分析
            val analysisResult = analyzeDatabase()
            
            // 生成维护报告
            generateMaintenanceReport(vacuumResult, checkpointResult, analysisResult)
            
            Log.i(TAG, "数据库维护任务完成")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "数据库维护任务失败", e)
            Result.failure()
        }
    }
    
    /**
     * 执行VACUUM操作
     * 回收未使用的空间，优化数据库文件大小
     */
    private suspend fun performVacuum(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始执行VACUUM操作")
            val startTime = System.currentTimeMillis()
            
            // 执行VACUUM操作
            appDatabase.openHelper.writableDatabase.execSQL("VACUUM")
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.i(TAG, "VACUUM操作完成，耗时 $duration 毫秒")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "VACUUM操作失败", e)
            return@withContext false
        }
    }
    
    /**
     * 执行检查点操作
     * 将WAL文件中的更改写入主数据库文件
     */
    private suspend fun performCheckpoint(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始执行检查点操作")
            val startTime = System.currentTimeMillis()
            
            // 执行检查点操作
            appDatabase.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            Log.i(TAG, "检查点操作完成，耗时 $duration 毫秒")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "检查点操作失败", e)
            return@withContext false
        }
    }
    
    /**
     * 分析数据库文件
     * 收集数据库文件大小、表数量等信息
     */
    private suspend fun analyzeDatabase(): DatabaseAnalysisResult = withContext(Dispatchers.IO) {
        val result = DatabaseAnalysisResult()
        
        try {
            Log.i(TAG, "开始分析数据库文件")
            
            // 获取数据库文件
            val dbFile = applicationContext.getDatabasePath("ccjizhang_database_plain")
            
            // 收集数据库文件信息
            if (dbFile.exists()) {
                result.dbFileExists = true
                result.dbFileSize = dbFile.length()
                result.dbFilePath = dbFile.absolutePath
            }
            
            // 检查WAL文件
            val walFile = File("${dbFile.path}-wal")
            if (walFile.exists()) {
                result.walFileExists = true
                result.walFileSize = walFile.length()
            }
            
            // 检查SHM文件
            val shmFile = File("${dbFile.path}-shm")
            if (shmFile.exists()) {
                result.shmFileExists = true
                result.shmFileSize = shmFile.length()
            }
            
            // 获取表数量
            val cursor = appDatabase.openHelper.readableDatabase.query(
                "SELECT count(*) FROM sqlite_master WHERE type='table'"
            )
            cursor.use {
                if (it.moveToFirst()) {
                    result.tableCount = it.getInt(0)
                }
            }
            
            // 获取索引数量
            val indexCursor = appDatabase.openHelper.readableDatabase.query(
                "SELECT count(*) FROM sqlite_master WHERE type='index'"
            )
            indexCursor.use {
                if (it.moveToFirst()) {
                    result.indexCount = it.getInt(0)
                }
            }
            
            // 获取数据库页大小
            val pageSizeCursor = appDatabase.openHelper.readableDatabase.query("PRAGMA page_size")
            pageSizeCursor.use {
                if (it.moveToFirst()) {
                    result.pageSize = it.getInt(0)
                }
            }
            
            // 获取数据库页数
            val pageCountCursor = appDatabase.openHelper.readableDatabase.query("PRAGMA page_count")
            pageCountCursor.use {
                if (it.moveToFirst()) {
                    result.pageCount = it.getInt(0)
                }
            }
            
            Log.i(TAG, "数据库分析完成")
        } catch (e: Exception) {
            Log.e(TAG, "数据库分析失败", e)
            result.error = e.message ?: "未知错误"
        }
        
        return@withContext result
    }
    
    /**
     * 生成维护报告
     */
    private suspend fun generateMaintenanceReport(
        vacuumResult: Boolean,
        checkpointResult: Boolean,
        analysisResult: DatabaseAnalysisResult
    ): File? = withContext(Dispatchers.IO) {
        try {
            // 创建报告目录
            val reportsDir = File(applicationContext.filesDir, REPORTS_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            // 创建报告文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val reportFile = File(
                reportsDir,
                "db_maintenance_${dateFormat.format(Date())}.txt"
            )
            
            // 写入报告内容
            reportFile.bufferedWriter().use { writer ->
                writer.write("数据库维护报告\n")
                writer.write("生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
                
                writer.write("维护操作结果:\n")
                writer.write("- VACUUM: ${if (vacuumResult) "成功" else "失败"}\n")
                writer.write("- 检查点: ${if (checkpointResult) "成功" else "失败"}\n\n")
                
                writer.write("数据库分析结果:\n")
                writer.write("- 数据库文件: ${if (analysisResult.dbFileExists) "存在" else "不存在"}\n")
                writer.write("- 数据库文件大小: ${formatFileSize(analysisResult.dbFileSize)}\n")
                writer.write("- 数据库文件路径: ${analysisResult.dbFilePath}\n")
                writer.write("- WAL文件: ${if (analysisResult.walFileExists) "存在 (${formatFileSize(analysisResult.walFileSize)})" else "不存在"}\n")
                writer.write("- SHM文件: ${if (analysisResult.shmFileExists) "存在 (${formatFileSize(analysisResult.shmFileSize)})" else "不存在"}\n")
                writer.write("- 表数量: ${analysisResult.tableCount}\n")
                writer.write("- 索引数量: ${analysisResult.indexCount}\n")
                writer.write("- 页大小: ${formatFileSize(analysisResult.pageSize.toLong())}\n")
                writer.write("- 页数: ${analysisResult.pageCount}\n")
                
                if (analysisResult.error != null) {
                    writer.write("\n错误信息: ${analysisResult.error}\n")
                }
            }
            
            Log.i(TAG, "维护报告已生成: ${reportFile.absolutePath}")
            return@withContext reportFile
        } catch (e: Exception) {
            Log.e(TAG, "生成维护报告失败", e)
            return@withContext null
        }
    }
    
    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
    
    /**
     * 数据库分析结果数据类
     */
    data class DatabaseAnalysisResult(
        var dbFileExists: Boolean = false,
        var dbFileSize: Long = 0,
        var dbFilePath: String = "",
        var walFileExists: Boolean = false,
        var walFileSize: Long = 0,
        var shmFileExists: Boolean = false,
        var shmFileSize: Long = 0,
        var tableCount: Int = 0,
        var indexCount: Int = 0,
        var pageSize: Int = 0,
        var pageCount: Int = 0,
        var error: String? = null
    )
}
