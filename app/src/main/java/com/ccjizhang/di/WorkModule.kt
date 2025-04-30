package com.ccjizhang.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkManager相关的依赖注入模块
 * 注意：此模块不再提供WorkManager配置，配置已移至WorkManagerModule
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkModule {
    // WorkManager配置已移至WorkManagerModule
} 