package com.ccjizhang.data.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import com.ccjizhang.data.repository.FinancialReportRepository
import com.ccjizhang.data.repository.InvestmentRepository
import com.ccjizhang.data.repository.RecurringTransactionRepository
import com.ccjizhang.data.repository.SavingGoalRepository
import com.ccjizhang.data.worker.InvestmentAutoUpdateWorker
import com.ccjizhang.data.worker.MonthlyFinancialReportWorker
import com.ccjizhang.data.worker.RecurringTransactionWorker
import com.ccjizhang.data.worker.SavingGoalAutoSaveWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 工作管理器初始化器
 * 负责注册和配置所有需要在后台定期运行的任务
 */
@Singleton
class WorkManagerInitializer @Inject constructor(
    private val context: Context,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val savingGoalRepository: SavingGoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val financialReportRepository: FinancialReportRepository
) {
    
    /**
     * 初始化所有工作任务
     */
    fun initializeWorkTasks() {
        // 注册定期交易检查和创建任务
        registerRecurringTransactionWorker()
        
        // 注册储蓄目标自动存款任务
        registerSavingGoalAutoSaveWorker()
        
        // 注册投资产品自动更新任务
        registerInvestmentAutoUpdateWorker()
        
        // 注册月度财务报告生成任务
        registerMonthlyFinancialReportWorker()
    }
    
    /**
     * 注册定期交易工作器
     * 每天检查需要创建的定期交易
     */
    private fun registerRecurringTransactionWorker() {
        val recurringTransactionWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
            1, TimeUnit.DAYS // 每天执行一次
        )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.MINUTES
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RecurringTransactionWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            recurringTransactionWorkRequest
        )
        
        Timber.d("已注册定期交易检查任务")
    }
    
    /**
     * 注册储蓄目标自动存款工作器
     * 每天检查需要自动存款的储蓄目标
     */
    private fun registerSavingGoalAutoSaveWorker() {
        val savingGoalWorkRequest = PeriodicWorkRequestBuilder<SavingGoalAutoSaveWorker>(
            1, TimeUnit.DAYS // 每天执行一次
        )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                30, TimeUnit.MINUTES
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SavingGoalAutoSaveWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            savingGoalWorkRequest
        )
        
        Timber.d("已注册储蓄目标自动存款任务")
    }
    
    /**
     * 注册投资产品自动更新工作器
     * 每天更新需要定期更新的投资产品价值
     */
    private fun registerInvestmentAutoUpdateWorker() {
        // 设置工作约束，需要网络连接
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val investmentUpdateWorkRequest = PeriodicWorkRequestBuilder<InvestmentAutoUpdateWorker>(
            1, TimeUnit.DAYS // 每天执行一次
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.HOURS
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            InvestmentAutoUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            investmentUpdateWorkRequest
        )
        
        Timber.d("已注册投资产品自动更新任务")
    }
    
    /**
     * 注册月度财务报告生成工作器
     * 每月生成一次月度财务报告
     */
    private fun registerMonthlyFinancialReportWorker() {
        val monthlyReportWorkRequest = PeriodicWorkRequestBuilder<MonthlyFinancialReportWorker>(
            30, TimeUnit.DAYS // 大约每月执行一次
        )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                2, TimeUnit.HOURS
            )
            .build()
            
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MonthlyFinancialReportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            monthlyReportWorkRequest
        )
        
        Timber.d("已注册月度财务报告生成任务")
    }
}