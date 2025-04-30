package com.ccjizhang.data.db

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.utils.DatabaseExceptionHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库连接管理器
 * 负责管理数据库连接的生命周期、资源使用和性能优化
 */
@Singleton
class DatabaseConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val databaseExceptionHandler: DatabaseExceptionHandler
) {
    companion object {
        private const val TAG = "DatabaseConnectionMgr"
        private const val IDLE_CONNECTION_TIMEOUT_MS = 30_000L // 空闲连接超时时间（30秒）
        private const val CONNECTION_CHECK_INTERVAL_MS = 10_000L // 连接检查间隔（10秒）
        private const val MAX_CONNECTIONS = 5 // 最大连接数
    }

    // 活跃连接计数
    private val activeConnections = AtomicInteger(0)

    // 连接使用情况跟踪
    private val connectionUsage = ConcurrentHashMap<String, Long>()

    // 连接监控任务
    private var monitorJob: Job? = null

    // 数据库是否已初始化
    private var isInitialized = false

    // 数据库配置
    private val databaseConfig = ConcurrentHashMap<String, Any>()

    /**
     * 初始化数据库连接管理器
     */
    fun initialize() {
        if (isInitialized) return

        Log.i(TAG, "初始化数据库连接管理器")

        // 在单独的协程中执行数据库配置，避免阻塞主线程
        CoroutineScope(Dispatchers.IO).launch {
            // 设置数据库配置
            configureDatabaseSettings()
        }

        // 预热数据库连接
        warmUpDatabaseConnection()

        // 启动连接监控
        startConnectionMonitoring()

        isInitialized = true

        Log.i(TAG, "数据库连接管理器初始化完成")
    }

    /**
     * 配置数据库设置
     */
    private fun configureDatabaseSettings() {
        try {
            val db = appDatabase.openHelper.writableDatabase

            // 使用查询方式执行 PRAGMA 语句，而不是直接使用 execSQL

            // 设置页缓存大小（默认为2000，增加可提高性能但会增加内存使用）
            db.query("PRAGMA cache_size = 4000", emptyArray()).close()
            databaseConfig["cache_size"] = 4000

            // 设置临时存储模式为内存（提高性能）
            db.query("PRAGMA temp_store = MEMORY", emptyArray()).close()
            databaseConfig["temp_store"] = "MEMORY"

            // 设置同步模式（FULL提供更高的数据安全性）
            db.query("PRAGMA synchronous = FULL", emptyArray()).close()
            databaseConfig["synchronous"] = "FULL"

            // 设置自动提交为开启状态
            db.query("PRAGMA auto_commit = ON", emptyArray()).close()
            databaseConfig["auto_commit"] = "ON"

            // 不设置日志模式，而是获取当前的日志模式
            // 避免与 Room 的设置冲突
            val journalMode = db.query("PRAGMA journal_mode", emptyArray()).use {
                if (it.moveToFirst()) it.getString(0) else "UNKNOWN"
            }
            databaseConfig["journal_mode"] = journalMode

            // 获取当前的同步模式
            val synchronousMode = db.query("PRAGMA synchronous", emptyArray()).use {
                if (it.moveToFirst()) it.getInt(0) else -1
            }
            databaseConfig["synchronous_value"] = synchronousMode

            // 获取当前的页大小
            val pageSize = db.query("PRAGMA page_size", emptyArray()).use {
                if (it.moveToFirst()) it.getInt(0) else -1
            }
            databaseConfig["page_size"] = pageSize

            // 获取当前的缓存大小
            val cacheSize = db.query("PRAGMA cache_size", emptyArray()).use {
                if (it.moveToFirst()) it.getInt(0) else -1
            }
            databaseConfig["cache_size_value"] = cacheSize

            // 获取当前的锁定模式
            val lockingMode = db.query("PRAGMA locking_mode", emptyArray()).use {
                if (it.moveToFirst()) it.getString(0) else "UNKNOWN"
            }
            databaseConfig["locking_mode"] = lockingMode

            Log.i(TAG, "数据库配置完成: $databaseConfig")
        } catch (e: Exception) {
            Log.e(TAG, "配置数据库设置失败", e)
        }
    }

    /**
     * 预热数据库连接
     * 提前初始化数据库连接，减少首次查询延迟
     */
    private fun warmUpDatabaseConnection() {
        try {
            // 在单独的协程中预热数据库连接，避免阻塞主线程
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 获取数据库连接
                    val db = appDatabase.openHelper.readableDatabase

                    // 执行简单查询预热连接
                    db.query("SELECT 1", emptyArray()).close()

                    Log.i(TAG, "数据库连接预热完成")
                } catch (e: Exception) {
                    Log.e(TAG, "数据库连接预热失败", e)
                }
            }

            Log.i(TAG, "数据库连接预热已安排")
        } catch (e: Exception) {
            Log.e(TAG, "安排数据库连接预热失败", e)
        }
    }

    /**
     * 启动连接监控
     * 定期检查空闲连接并关闭长时间未使用的连接
     */
    private fun startConnectionMonitoring() {
        monitorJob?.cancel()

        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // 检查空闲连接
                    checkIdleConnections()

                    // 记录当前连接状态
                    logConnectionStatus()

                    // 等待下一次检查
                    delay(CONNECTION_CHECK_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "连接监控异常", e)
                }
            }
        }

        Log.i(TAG, "数据库连接监控已启动")
    }

    /**
     * 检查空闲连接
     * 关闭长时间未使用的连接以释放资源
     */
    private suspend fun checkIdleConnections() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()

        // 检查每个连接的最后使用时间
        connectionUsage.entries.removeIf { (connectionId, lastUsedTime) ->
            val idleTime = currentTime - lastUsedTime

            // 如果连接空闲时间超过阈值，尝试关闭它
            if (idleTime > IDLE_CONNECTION_TIMEOUT_MS) {
                Log.d(TAG, "关闭空闲连接: $connectionId, 空闲时间: ${idleTime}ms")

                // 从跟踪列表中移除
                return@removeIf true
            }

            false
        }
    }

    /**
     * 记录当前连接状态
     */
    private fun logConnectionStatus() {
        val activeCount = activeConnections.get()
        Log.d(TAG, "当前活跃连接数: $activeCount, 跟踪连接数: ${connectionUsage.size}")
    }

    /**
     * 获取数据库连接
     * 跟踪连接使用情况并限制最大连接数
     */
    fun getConnection(): SupportSQLiteDatabase {
        // 检查是否超过最大连接数
        val currentConnections = activeConnections.incrementAndGet()
        if (currentConnections > MAX_CONNECTIONS) {
            Log.w(TAG, "警告: 当前连接数($currentConnections)超过最大限制($MAX_CONNECTIONS)")
        }

        // 获取数据库连接
        val connection = appDatabase.openHelper.writableDatabase

        // 生成连接ID并记录使用时间
        val connectionId = "conn-${System.identityHashCode(connection)}"
        connectionUsage[connectionId] = System.currentTimeMillis()

        Log.d(TAG, "获取数据库连接: $connectionId, 当前活跃连接数: $currentConnections")

        return connection
    }

    /**
     * 释放数据库连接
     * 更新连接使用时间并减少活跃连接计数
     */
    fun releaseConnection(connection: SupportSQLiteDatabase) {
        // 更新连接最后使用时间
        val connectionId = "conn-${System.identityHashCode(connection)}"
        connectionUsage[connectionId] = System.currentTimeMillis()

        // 减少活跃连接计数
        val currentConnections = activeConnections.decrementAndGet()

        Log.d(TAG, "释放数据库连接: $connectionId, 当前活跃连接数: $currentConnections")
    }

    /**
     * 执行数据库操作
     * 自动管理连接的获取和释放
     */
    suspend fun <T> withConnection(block: (SupportSQLiteDatabase) -> T): T = withContext(Dispatchers.IO) {
        val connection = getConnection()

        try {
            // 执行数据库操作
            block(connection)
        } finally {
            // 确保连接被释放
            releaseConnection(connection)
        }
    }

    /**
     * 执行数据库事务
     * 自动管理事务的开始和提交/回滚
     */
    suspend fun <T> withTransaction(block: (SupportSQLiteDatabase) -> T): T = withContext(Dispatchers.IO) {
        val connection = getConnection()

        try {
            // 开始事务
            connection.beginTransaction()

            try {
                // 执行事务操作
                val result = block(connection)

                // 标记事务成功
                connection.setTransactionSuccessful()

                result
            } finally {
                // 结束事务（提交或回滚）
                connection.endTransaction()
            }
        } finally {
            // 确保连接被释放
            releaseConnection(connection)
        }
    }

    /**
     * 获取数据库配置信息
     */
    fun getDatabaseConfig(): Map<String, Any> {
        return databaseConfig.toMap()
    }

    /**
     * 获取当前连接统计信息
     */
    fun getConnectionStats(): ConnectionStats {
        return ConnectionStats(
            activeConnections = activeConnections.get(),
            trackedConnections = connectionUsage.size,
            maxConnections = MAX_CONNECTIONS,
            databaseConfig = databaseConfig.toMap()
        )
    }

    /**
     * 关闭数据库连接管理器
     */
    fun shutdown() {
        Log.i(TAG, "关闭数据库连接管理器")

        // 取消监控任务
        monitorJob?.cancel()
        monitorJob = null

        // 清除连接跟踪
        connectionUsage.clear()
        activeConnections.set(0)

        isInitialized = false
    }

    /**
     * 连接统计信息数据类
     */
    data class ConnectionStats(
        val activeConnections: Int,
        val trackedConnections: Int,
        val maxConnections: Int,
        val databaseConfig: Map<String, Any>
    )
}
