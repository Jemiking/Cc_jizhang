package com.ccjizhang.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.InvestmentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 投资产品自动更新工作器
 * 自动更新需要定期更新价值的投资产品
 */
@HiltWorker
class InvestmentAutoUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val investmentRepository: InvestmentRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "investment_auto_update_worker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始执行投资产品价值自动更新任务")
            
            // 处理需要自动更新价值的投资产品
            investmentRepository.processAutoValueUpdates()
            
            Timber.d("投资产品价值自动更新任务完成")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "投资产品价值自动更新任务失败: ${e.message}")
            Result.failure()
        }
    }
} 