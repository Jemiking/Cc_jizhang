package com.ccjizhang.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.SavingGoalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 储蓄目标自动存款工作器
 * 自动对设置了自动存款的储蓄目标执行定期存款
 */
@HiltWorker
class SavingGoalAutoSaveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val savingGoalRepository: SavingGoalRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "saving_goal_auto_save_worker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始执行储蓄目标自动存款任务")
            
            // 处理需要自动存款的目标
            savingGoalRepository.processAutoSaveGoals()
            
            Timber.d("储蓄目标自动存款任务完成")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "储蓄目标自动存款任务失败")
            Result.failure()
        }
    }
} 