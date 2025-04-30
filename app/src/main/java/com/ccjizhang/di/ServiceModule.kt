package com.ccjizhang.di

import android.content.Context
import com.ccjizhang.data.repository.FinancialReportRepository
import com.ccjizhang.data.repository.InvestmentRepository
import com.ccjizhang.data.repository.RecurringTransactionRepository
import com.ccjizhang.data.repository.SavingGoalRepository
import com.ccjizhang.data.service.WorkManagerInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 服务相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    /**
     * 提供WorkManagerInitializer实例
     * 
     * @param context 应用上下文
     * @param recurringTransactionRepository 定期交易存储库
     * @param savingGoalRepository 存储目标存储库
     * @param investmentRepository 投资存储库
     * @param financialReportRepository 财务报告存储库
     * @return WorkManagerInitializer实例
     */
    @Provides
    @Singleton
    fun provideWorkManagerInitializer(
        @ApplicationContext context: Context,
        recurringTransactionRepository: RecurringTransactionRepository,
        savingGoalRepository: SavingGoalRepository,
        investmentRepository: InvestmentRepository,
        financialReportRepository: FinancialReportRepository
    ): WorkManagerInitializer {
        return WorkManagerInitializer(
            context,
            recurringTransactionRepository,
            savingGoalRepository,
            investmentRepository,
            financialReportRepository
        )
    }
} 