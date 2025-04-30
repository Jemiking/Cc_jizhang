package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.TransactionRepository
import com.ccjizhang.utils.GrayscaleReleaseConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据一致性检查工具类
 * 提供数据验证与修复功能
 */
class DataIntegrityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val appDatabase: AppDatabase,
    private val grayscaleReleaseConfig: GrayscaleReleaseConfig
) {
    companion object {
        private const val TAG = "DataIntegrityChecker"
    }

    /**
     * 检查并修复所有数据一致性问题
     * @return 如果进行了任何修复，返回true
     */
    suspend fun checkAndFixAllIssues(): Boolean {
        var hasFixed = false

        // 检查数据库完整性
        val integrityOk = checkDatabaseIntegrity()
        if (!integrityOk) {
            Log.w(TAG, "数据库完整性检查失败，可能需要修复")
            // 这里不进行修复，因为完整性问题通常需要从备份恢复
        }

        // 检查账户余额
        val balanceFixed = transactionRepository.verifyAndFixAccountBalances()
        if (balanceFixed) {
            hasFixed = true
            Log.i(TAG, "账户余额不一致问题已修复")
        }

        // 检查孤立的交易记录（引用了不存在的分类或账户）
        val orphanedFixed = checkAndFixOrphanedTransactions()
        if (orphanedFixed) {
            hasFixed = true
            Log.i(TAG, "孤立的交易记录问题已修复")
        }

        // 检查WAL模式是否正确启用
        val walModeOk = checkWalMode()
        if (!walModeOk) {
            Log.w(TAG, "WAL模式未正确启用，尝试启用")
            enableWalMode()
            hasFixed = true
        }

        return hasFixed
    }

    /**
     * 检查数据库完整性
     * @return 如果数据库完整性正常，返回true
     */
    suspend fun checkDatabaseIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cursor = appDatabase.query("PRAGMA integrity_check", null)
                cursor.use {
                    if (cursor.moveToFirst()) {
                        val result = cursor.getString(0)
                        val isOk = result == "ok"
                        Log.i(TAG, "数据库完整性检查结果: $result")
                        return@withContext isOk
                    }
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "数据库完整性检查失败", e)
                false
            }
        }
    }

    /**
     * 检查并修复孤立的交易记录
     * @return 如果进行了修复，返回true
     */
    private suspend fun checkAndFixOrphanedTransactions(): Boolean = withContext(Dispatchers.IO) {
        try {
            var hasFixed = false

            // 检查引用了不存在分类的交易
            val orphanedByCategoryCount = transactionRepository.fixTransactionsWithInvalidCategory()
            if (orphanedByCategoryCount > 0) {
                Log.i(TAG, "修复了 $orphanedByCategoryCount 条引用了不存在分类的交易")
                hasFixed = true
            }

            // 检查引用了不存在账户的交易
            val orphanedByAccountCount = transactionRepository.fixTransactionsWithInvalidAccount()
            if (orphanedByAccountCount > 0) {
                Log.i(TAG, "修复了 $orphanedByAccountCount 条引用了不存在账户的交易")
                hasFixed = true
            }

            hasFixed
        } catch (e: Exception) {
            Log.e(TAG, "修复孤立交易失败", e)
            false
        }
    }

    /**
     * 检查WAL模式是否启用
     * @return 如果WAL模式已启用或不需要启用，返回true
     */
    private suspend fun checkWalMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查是否启用了WAL模式的灰度发布
            if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE)) {
                Log.i(TAG, "WAL模式未在灰度发布中启用，跳过检查")
                return@withContext true // 不需要启用WAL模式，直接返回true
            }

            val cursor = appDatabase.query("PRAGMA journal_mode", null)
            cursor.use {
                if (cursor.moveToFirst()) {
                    val journalMode = cursor.getString(0)
                    Log.i(TAG, "当前日志模式: $journalMode")
                    return@withContext journalMode.equals("wal", ignoreCase = true)
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "检查WAL模式失败", e)
            false
        }
    }

    /**
     * 启用WAL模式
     */
    private suspend fun enableWalMode() = withContext(Dispatchers.IO) {
        try {
            // 检查是否启用了WAL模式的灰度发布
            if (!grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE)) {
                Log.i(TAG, "WAL模式未在灰度发布中启用，跳过启用")
                return@withContext
            }

            appDatabase.query("PRAGMA journal_mode = WAL", null)
            Log.i(TAG, "启用WAL模式成功")

            // 设置同步模式为NORMAL
            appDatabase.query("PRAGMA synchronous = NORMAL", null)
            Log.i(TAG, "设置同步模式为NORMAL")

            // 设置WAL文件大小限制
            appDatabase.query("PRAGMA journal_size_limit = 10485760", null) // 10MB
            Log.i(TAG, "设置WAL文件大小限制为10MB")
        } catch (e: Exception) {
            Log.e(TAG, "启用WAL模式失败", e)
        }
    }

    /**
     * 设置定期数据一致性检查任务
     * 默认每周运行一次
     */
    fun schedulePeriodicIntegrityCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val dataIntegrityCheckRequest = PeriodicWorkRequestBuilder<DataIntegrityWorker>(
            7, TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DATA_INTEGRITY_CHECK",
            ExistingPeriodicWorkPolicy.KEEP,
            dataIntegrityCheckRequest
        )
    }

    /**
     * 数据一致性检查Worker
     * 在后台定期执行数据一致性检查
     */
    @HiltWorker
    class DataIntegrityWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val dataIntegrityChecker: DataIntegrityChecker
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                // 执行一致性检查
                dataIntegrityChecker.checkAndFixAllIssues()
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}