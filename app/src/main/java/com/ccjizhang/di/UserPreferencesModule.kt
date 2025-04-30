package com.ccjizhang.di

import com.ccjizhang.data.repository.UserPreferencesRepository
import com.ccjizhang.data.repository.UserPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 用户偏好设置的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserPreferencesModule {
    
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        repositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
} 