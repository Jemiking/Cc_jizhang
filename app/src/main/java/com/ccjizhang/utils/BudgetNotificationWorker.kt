package com.ccjizhang.utils

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.BudgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 预算通知工作器
 * 用于定期检查预算使用情况并发送通知
 */
@HiltWorker
class BudgetNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 检查需要通知的预算
            val budgetsToNotify = budgetRepository.checkBudgetsForNotification()
            
            if (budgetsToNotify.isNotEmpty()) {
                // 发送通知
                for (budget in budgetsToNotify) {
                    val (used, total) = budgetRepository.getBudgetUsage(budget.id)
                    val percentage = (used / total * 100).toInt()
                    
                    // 根据使用情况决定显示什么通知
                    if (used > total) {
                        // 超支通知
                        notificationHelper.showBudgetExceededNotification(
                            budget.name,
                            used - total
                        )
                    } else {
                        // 接近预算上限通知
                        notificationHelper.showBudgetWarningNotification(
                            budget.name,
                            percentage,
                            total - used
                        )
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
} 