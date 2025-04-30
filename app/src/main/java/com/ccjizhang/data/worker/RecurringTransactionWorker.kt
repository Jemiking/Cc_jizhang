package com.ccjizhang.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.RecurringTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 定期交易工作器
 * 自动创建设置为定期执行的交易记录
 */
@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringTransactionRepository: RecurringTransactionRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始执行定期交易自动创建任务")
            
            // 处理今天需要执行的定期交易
            recurringTransactionRepository.processDueTransactions()
            
            Timber.d("定期交易自动创建任务完成")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "定期交易自动创建任务失败: ${e.message}")
            Result.failure()
        }
    }
} 