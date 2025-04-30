package com.ccjizhang.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ccjizhang.data.archive.DatabaseArchiveManager
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.DatabaseConnectionManager
import com.ccjizhang.data.db.DatabasePerformanceMonitor
import com.ccjizhang.data.db.TransactionManager
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.db.dao.TransactionTagDao
import com.ccjizhang.utils.DatabaseExceptionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * 数据库模块
 * 提供数据库相关的依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // 不再提供 AppDatabase实例，使用 AppModule 中的实例

    /**
     * 提供数据库连接管理器
     */
    @Provides
    @Singleton
    fun provideDatabaseConnectionManager(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): DatabaseConnectionManager {
        // 创建连接管理器实例，但不立即初始化
        val connectionManager = DatabaseConnectionManager(context, appDatabase, databaseExceptionHandler)

        // 在单独的协程中初始化连接管理器，避免阻塞主线程
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 等待一小段时间，避免与其他初始化操作冲突
                delay(500)
                connectionManager.initialize()
            } catch (e: Exception) {
                android.util.Log.e("DatabaseModule", "初始化数据库连接管理器失败", e)
            }
        }

        return connectionManager
    }

    /**
     * 提供数据库性能监控器
     */
    @Provides
    @Singleton
    fun provideDatabasePerformanceMonitor(
        @ApplicationContext context: Context,
        connectionManager: DatabaseConnectionManager,
        appDatabase: AppDatabase
    ): DatabasePerformanceMonitor {
        return DatabasePerformanceMonitor(context, connectionManager, appDatabase)
    }

    /**
     * 提供数据归档管理器
     */
    @Provides
    @Singleton
    fun provideDatabaseArchiveManager(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        transactionManager: TransactionManager,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): DatabaseArchiveManager {
        return DatabaseArchiveManager(context, appDatabase, transactionManager, databaseExceptionHandler)
    }

    /**
     * 提供数据库连接
     * 注意：这个连接应该在使用后释放
     */
    @Provides
    fun provideDatabaseConnection(
        connectionManager: DatabaseConnectionManager
    ): SupportSQLiteDatabase {
        return connectionManager.getConnection()
    }

    /**
     * 提供事务管理器
     */
    @Provides
    @Singleton
    fun provideTransactionManager(
        connectionManager: DatabaseConnectionManager,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): TransactionManager {
        return TransactionManager(connectionManager, databaseExceptionHandler)
    }

    // 不再提供 DAO 实例，使用 AppModule 中的实例

    // 不再提供 DatabaseExceptionHandler，使用 AppModule 中的实例
}
