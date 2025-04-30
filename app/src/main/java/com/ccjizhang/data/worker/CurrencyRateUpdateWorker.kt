package com.ccjizhang.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.service.CurrencyService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 汇率更新工作器
 * 用于定期自动更新汇率数据
 */
@HiltWorker
class CurrencyRateUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val currencyService: CurrencyService
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "CurrencyRateUpdateWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始更新汇率数据")
            
            // 检查是否需要更新
            if (!currencyService.shouldUpdateRates()) {
                Log.d(TAG, "上次更新时间未超过24小时，跳过本次更新")
                return@withContext Result.success()
            }
            
            // 执行汇率更新
            currencyService.updateExchangeRates()
            
            // 根据更新结果返回工作结果
            val updateResult = currencyService.updateResult.value
            return@withContext if (updateResult is CurrencyService.UpdateResult.Success) {
                Log.d(TAG, "汇率更新成功")
                Result.success()
            } else {
                Log.e(TAG, "汇率更新失败: ${updateResult?.let { 
                    (it as? CurrencyService.UpdateResult.Error)?.message 
                } ?: "未知错误"}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "汇率更新出错", e)
            Result.failure()
        }
    }
} 