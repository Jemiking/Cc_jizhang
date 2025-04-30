package com.ccjizhang.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ccjizhang.data.repository.FinancialReportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

/**
 * 月度财务报告生成工作器
 * 在每月初自动生成上个月的财务报告
 */
@HiltWorker
class MonthlyFinancialReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val financialReportRepository: FinancialReportRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "monthly_financial_report_worker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Timber.d("开始生成月度财务报告")
            
            // 获取上个月的年份和月份
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1) // 上个月
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH 从 0 开始，需要 +1
            
            // 生成月度报告
            val reportId = financialReportRepository.generateMonthlyReport(year, month)
            
            Timber.d("月度财务报告生成完成，报告ID: $reportId")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "月度财务报告生成失败: ${e.message}")
            Result.failure()
        }
    }
} 