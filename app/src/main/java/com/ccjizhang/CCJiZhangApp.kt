package com.ccjizhang

import android.app.AlertDialog
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import timber.log.Timber
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

import com.ccjizhang.data.archive.DatabaseArchiveManager
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.DatabaseConnectionManager
import com.ccjizhang.data.db.DatabasePerformanceMonitor
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.service.WorkManagerInitializer
import com.ccjizhang.utils.AutoBackupWorker
import com.ccjizhang.utils.BackupReminderWorker
import com.ccjizhang.utils.BudgetNotificationWorker
import com.ccjizhang.utils.DatabaseMigrationHelper
import com.ccjizhang.utils.DatabaseMonitor
import com.ccjizhang.utils.DatabaseRecoveryManager
import com.ccjizhang.utils.DatabaseRepairTool
import com.ccjizhang.utils.DataIntegrityChecker
import com.ccjizhang.utils.GrayscaleReleaseConfig
import com.ccjizhang.utils.WebDavSyncManager
import com.ccjizhang.data.worker.CurrencyRateUpdateWorker
import com.ccjizhang.workers.DatabasePerformanceMonitorWorker
import com.ccjizhang.workers.DatabaseMaintenanceWorker
import com.ccjizhang.utils.DatabaseStructureAnalyzer
import com.ccjizhang.utils.UserFeedbackManager
import com.ccjizhang.utils.AdvancedDatabaseMonitor
import com.ccjizhang.utils.QueryLogManager
import com.ccjizhang.utils.DatabaseAnalyzer
import com.ccjizhang.ui.utils.IconUtils
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import com.ccjizhang.data.model.CategoryType
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.util.Log as AndroidLog
import com.ccjizhang.utils.FileLoggingTree

/**
 * 应用主类
 * 使用Hilt进行依赖注入
 */
@HiltAndroidApp
class CCJiZhangApp : Application(), Configuration.Provider {

    /**
     * 自定义日志树，用于发布版本
     * 只记录WARN和ERROR级别的日志，并可以发送到崩溃报告服务
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < AndroidLog.WARN) return

            // 记录到系统日志
            AndroidLog.println(priority, tag, message)

            // 如果是错误日志，记录异常信息
            if (priority >= AndroidLog.ERROR && t != null) {
                // 这里可以集成崩溃报告服务，如Firebase Crashlytics
                // FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var dataIntegrityChecker: DataIntegrityChecker

    @Inject
    lateinit var autoBackupScheduler: AutoBackupWorker.Scheduler

    @Inject
    lateinit var databaseMigrationHelper: DatabaseMigrationHelper

    @Inject
    lateinit var webDavSyncManager: WebDavSyncManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManagerInitializer: WorkManagerInitializer

    @Inject
    lateinit var databaseRepairTool: DatabaseRepairTool

    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var databaseRecoveryManager: DatabaseRecoveryManager

    @Inject
    lateinit var grayscaleReleaseConfig: GrayscaleReleaseConfig

    @Inject
    lateinit var databaseMonitor: DatabaseMonitor

    @Inject
    lateinit var databaseConnectionManager: DatabaseConnectionManager

    @Inject
    lateinit var databasePerformanceMonitor: DatabasePerformanceMonitor

    @Inject
    lateinit var databaseArchiveManager: DatabaseArchiveManager

    @Inject
    lateinit var advancedDatabaseMonitor: AdvancedDatabaseMonitor

    @Inject
    lateinit var queryLogManager: QueryLogManager

    @Inject
    lateinit var databaseAnalyzer: DatabaseAnalyzer

    @Inject
    lateinit var fileLoggingTree: FileLoggingTree

    @Inject
    lateinit var themeManager: com.ccjizhang.ui.theme.ThemeManager

    @Inject
    override lateinit var workManagerConfiguration: Configuration

    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 初始化Timber日志库
        try {
            // 调试版本使用详细日志
            Timber.plant(Timber.DebugTree())
            // 同时使用文件日志树，方便诊断问题
            Timber.plant(fileLoggingTree)

            // 在发布版本中添加崩溃报告树
            // 注意：我们在这里总是添加崩溃报告树，因为我们无法确定当前是否是调试版本
            Timber.plant(CrashReportingTree())
        } catch (e: Exception) {
            // 如果初始化Timber失败，使用原生日志
            AndroidLog.e("CCJiZhangApp", "初始化Timber失败", e)
        }

        Timber.i("应用启动中...")

        try {
            // 仅执行必要的初始化操作，其他操作延后执行

            // 初始化灰度发布配置
            initGrayscaleReleaseConfig()

            // 初始化WorkManager
            WorkManager.initialize(this, workManagerConfiguration)

            // 初始化图标缓存
            initializeIconCache()

            // 增强的数据库检查和修复机制
            try {
                Timber.i("在应用启动时执行增强的数据库检查")
                // 阻塞式执行数据库检查
                runBlocking {
                    try {
                        // 1. 检查数据库文件是否存在
                        val dbFile = getDatabasePath("ccjizhang_database_plain")
                        if (!dbFile.exists()) {
                            Timber.i("数据库文件不存在，将在后续初始化中创建")
                        } else {
                            Timber.i("数据库文件已存在，大小: ${dbFile.length()} 字节")

                            // 2. 检查数据库文件是否可读
                            if (!dbFile.canRead()) {
                                Timber.e("数据库文件不可读，尝试修复")
                                // 尝试修复文件权限
                                val fixPermission = dbFile.setReadable(true)
                                Timber.i("修复文件读取权限结果: $fixPermission")
                            }

                            // 3. 检查数据库文件大小
                            if (dbFile.length() == 0L) {
                                Timber.e("数据库文件大小为0，可能已损坏")
                                // 删除空数据库文件
                                val deleted = dbFile.delete()
                                Timber.i("删除空数据库文件结果: $deleted")
                            }

                            // 4. 尝试打开数据库进行基本测试
                            try {
                                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                                    dbFile.path,
                                    null,
                                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                                )
                                // 执行简单查询
                                val cursor = db.rawQuery("SELECT 1", null)
                                val result = cursor.moveToFirst()
                                cursor.close()
                                db.close()

                                if (!result) {
                                    Timber.e("数据库可以打开但无法执行查询，可能已损坏")
                                    // 检测到数据库损坏，标记为需要修复
                                    databaseRepairTool.setDatabaseStatus("CORRUPTED")
                                } else {
                                    Timber.i("数据库文件可以正常打开和查询")
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "数据库文件无法打开或查询，可能已损坏")
                                // 检测到数据库损坏，标记为需要修复
                                databaseRepairTool.setDatabaseStatus("CORRUPTED")
                            }

                            // 5. 检查数据库状态
                            val isDatabaseCorrupted = databaseRepairTool.isDatabaseCorrupted()
                            if (isDatabaseCorrupted) {
                                Timber.e("数据库已损坏，需要修复")
                                // 尝试修复数据库
                                val repairResult = databaseRepairTool.forceRepairDatabase()
                                Timber.i("数据库修复结果: $repairResult")

                                val shouldReset = !repairResult
                                if (shouldReset) {
                                    // 如果修复失败，尝试完全重置
                                    Timber.e("数据库修复失败，尝试完全重置")
                                    val resetResult = databaseRepairTool.completelyResetDatabase()
                                    Timber.i("数据库完全重置结果: $resetResult")

                                    if (resetResult) {
                                        // 重置成功，初始化默认数据
                                        initializeDefaultData()
                                    } else {
                                        // 重置失败，什么也不做
                                        Timber.e("数据库完全重置失败")
                                    }
                                } else {
                                    // 修复成功
                                    Timber.i("数据库修复成功")
                                }
                            } else {
                                Timber.i("数据库状态正常")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "检查数据库文件失败")
                    }
                }

                Timber.i("增强的数据库检查完成，继续应用初始化")
            } catch (e: Exception) {
                Timber.e(e, "数据库检查失败")
            }

            // 在后台线程中执行其他初始化操作
            applicationScope.launch {
                try {
                    // 第一阶段：数据库基本配置
                    Timber.i("第一阶段初始化开始")

                    // 检查数据库版本
                    checkDatabaseVersion()

                    // 配置数据库同步模式
                    configureDatabaseSyncMode()

                    // 设置数据库检查点策略
                    setupDatabaseCheckpoint()

                    // 等待一段时间，确保数据库初始化完成
                    delay(1000)

                    // 第二阶段：数据初始化
                    Timber.i("第二阶段初始化开始")

                    // 初始化默认数据
                    initDefaultData()

                    // 等待一段时间，确保数据初始化完成
                    delay(500)

                    // 第三阶段：其他功能初始化
                    Timber.i("第三阶段初始化开始")

                    // 设置预算通知定时任务
                    setupBudgetNotificationWork()

                    // 设置数据一致性检查任务
                    setupDataIntegrityCheck()

                    // 设置自动备份任务
                    setupAutoBackup()

                    // 设置WebDAV自动同步任务
                    setupWebDavSync()

                    // 设置定期汇率更新任务
                    setupCurrencyRateUpdateWork()

                    // 等待一段时间，确保任务设置完成
                    delay(500)

                    // 第四阶段：高级功能初始化
                    Timber.i("第四阶段初始化开始")

                    // 设置高级功能相关的工作任务
                    setupAdvancedFeaturesTasks()

                    // 设置数据库性能监控
                    setupDatabasePerformanceMonitoring()

                    // 初始化数据归档管理器
                    setupDatabaseArchiving()

                    // 生成数据库结构分析报告
                    generateDatabaseStructureReport()

                    // 启动数据库监控
                    startDatabaseMonitoring()

                    Timber.i("应用初始化完成")
                } catch (e: Exception) {
                    Timber.e(e, "应用初始化失败")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "应用初始化失败")
        }
    }

    /**
     * 检查数据库完整性并修复
     */
    private fun checkDatabaseIntegrity() {
        // 立即尝试修复数据库，不要等待协程
        try {
            Timber.i("开始数据库完全重置")
            // 在主线程中直接调用完全重置方法
            applicationScope.launch {
                try {
                    val resetResult = databaseRepairTool.completelyResetDatabase()
                    if (resetResult) {
                        Timber.i("数据库重置成功，开始初始化数据")
                        initializeDefaultData()
                        showToast("数据库已重置，应用可以正常使用")
                    } else {
                        Timber.e("数据库重置失败")
                        showToast("数据库重置失败，请尝试清除应用数据后重新安装")
                    }

                    // 创建数据库备份
                    try {
                        if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_AUTO_BACKUP)) {
                            databaseRecoveryManager.createScheduledBackup()
                            Timber.i("创建数据库备份成功")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "创建数据库备份失败")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "数据库重置过程中发生错误")
                    showToast("数据库重置失败，请尝试清除应用数据后重新安装")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "数据库完整性检查失败")
            // 如果完整性检查失败，提示用户清除应用数据
            showToast("数据库可能已损坏，请在系统设置中清除应用数据")
        }
    }

    /**
     * 测试数据库查询
     * @return 如果查询成功返回true，否则返回false
     */
    private suspend fun testDatabaseQuery(): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.i("开始测试数据库查询")

            // 首先检查数据库文件是否存在
            val dbFile = getDatabasePath("ccjizhang_database_plain")
            if (!dbFile.exists()) {
                Timber.w("数据库文件不存在")
                return@withContext false
            }

            // 检查文件大小
            if (dbFile.length() == 0L) {
                Timber.w("数据库文件大小为0")
                return@withContext false
            }

            // 尝试直接打开数据库文件
            try {
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.path,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                // 执行简单查询
                val cursor = db.rawQuery("SELECT 1", null)
                val result = cursor.moveToFirst()
                cursor.close()
                db.close()

                if (!result) {
                    Timber.w("数据库可以打开但无法执行查询")
                    return@withContext false
                }

                Timber.i("直接打开数据库文件测试成功")
            } catch (e: Exception) {
                Timber.e(e, "直接打开数据库文件失败")
                // 继续尝试使用Room打开
            }

            // 尝试使用Room执行查询
            try {
                val result = appDatabase.query("SELECT 1", null)
                result.close()
                Timber.i("数据库测试查询成功")
                return@withContext true
            } catch (e: Exception) {
                Timber.e(e, "数据库测试查询失败")
                return@withContext false
            }
        } catch (e: Exception) {
            Timber.e(e, "数据库测试查询过程中发生异常")
            return@withContext false
        }
    }

    /**
     * 显示数据库重置对话框
     * 当检测到数据库问题时，提示用户选择重置数据库
     */
    private fun showDatabaseResetDialog() {
        // 在主线程中显示对话框
        Handler(Looper.getMainLooper()).post {
            try {
                // 直接强制重置数据库，不显示对话框
                Timber.i("检测到数据库问题，自动开始重置数据库")

                // 在后台线程中执行数据库重置
                applicationScope.launch {
                    try {
                        Timber.i("开始完全重置数据库")
                        val resetResult = databaseRepairTool.completelyResetDatabase()
                        if (resetResult) {
                            // 重置成功，初始化默认数据
                            Timber.i("数据库重置成功，开始初始化默认数据")
                            initializeDefaultData()
                            // 通知用户
                            showToast("数据库已重置，应用可以正常使用")
                        } else {
                            // 重置失败，提示用户清除应用数据
                            Timber.e("数据库重置失败")
                            showToast("数据库重置失败，请尝试在系统设置中清除应用数据")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "数据库重置过程中发生错误")
                        showToast("数据库重置失败，请尝试在系统设置中清除应用数据")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "显示数据库重置对话框失败")
                // 如果显示对话框失败，直接提示用户清除应用数据
                showToast("数据库可能已损坏，请在系统设置中清除应用数据")
            }
        }
    }

    /**
     * 显示Toast消息
     */
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 检查数据库版本
     * 确保数据库结构与应用版本匹配
     */
    private suspend fun checkDatabaseVersion() {
        try {
            Timber.i("开始检查数据库版本")

            // 获取当前数据库版本
            val cursor = appDatabase.query("PRAGMA user_version", null)
            var currentDbVersion = 0
            if (cursor.moveToFirst()) {
                currentDbVersion = cursor.getInt(0)
            }
            cursor.close()

            // 获取应用期望的数据库版本
            val appDbVersion = 6 // 使用当前数据库版本号

            Timber.i("当前数据库版本: $currentDbVersion, 应用期望的数据库版本: $appDbVersion")

            if (currentDbVersion < appDbVersion) {
                Timber.w("数据库版本不匹配，需要升级")

                // 尝试升级数据库版本
                try {
                    // 执行完整性检查
                    val integrityCheckCursor = appDatabase.query("PRAGMA integrity_check", null)
                    var integrityResult = "unknown"
                    if (integrityCheckCursor.moveToFirst()) {
                        integrityResult = integrityCheckCursor.getString(0)
                    }
                    integrityCheckCursor.close()

                    Timber.i("数据库完整性检查结果: $integrityResult")

                    // 如果完整性检查失败，尝试修复数据库
                    if (integrityResult != "ok") {
                        Timber.w("数据库完整性检查失败，尝试修复")
                        val repairResult = databaseRepairTool.forceRepairDatabase()
                        Timber.i("数据库修复结果: $repairResult")

                        if (!repairResult) {
                            // 如果修复失败，尝试完全重置
                            Timber.e("数据库修复失败，尝试完全重置")
                            val resetResult = databaseRepairTool.completelyResetDatabase()
                            Timber.i("数据库完全重置结果: $resetResult")
                            return
                        }
                    }

                    // 设置新的数据库版本
                    appDatabase.query("PRAGMA user_version = $appDbVersion", null)
                    Timber.i("数据库版本已更新为: $appDbVersion")

                    // 执行数据库优化
                    try {
                        appDatabase.query("PRAGMA optimize", null)
                        Timber.i("数据库优化成功")
                    } catch (e: Exception) {
                        Timber.e(e, "数据库优化失败")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "更新数据库版本失败")

                    // 如果更新失败，检查数据库是否损坏
                    if (databaseRepairTool.isDatabaseCorrupted()) {
                        Timber.w("数据库已损坏，尝试修复")
                        val repairResult = databaseRepairTool.forceRepairDatabase()
                        Timber.i("数据库修复结果: $repairResult")
                    }
                }
            } else {
                Timber.i("数据库版本匹配，无需升级")

                // 即使版本匹配，也执行完整性检查
                try {
                    val integrityCheckCursor = appDatabase.query("PRAGMA integrity_check", null)
                    var integrityResult = "unknown"
                    if (integrityCheckCursor.moveToFirst()) {
                        integrityResult = integrityCheckCursor.getString(0)
                    }
                    integrityCheckCursor.close()

                    Timber.i("数据库完整性检查结果: $integrityResult")

                    // 如果完整性检查失败，尝试修复数据库
                    if (integrityResult != "ok") {
                        Timber.w("数据库完整性检查失败，尝试修复")
                        val repairResult = databaseRepairTool.forceRepairDatabase()
                        Timber.i("数据库修复结果: $repairResult")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "执行完整性检查失败")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "检查数据库版本失败")
            // 如果无法检查版本，可能是数据库损坏
            if (databaseRepairTool.isDatabaseCorrupted()) {
                Timber.w("数据库已损坏，尝试修复")
                // 尝试修复数据库
                val repairResult = databaseRepairTool.forceRepairDatabase()
                Timber.i("数据库修复结果: $repairResult")

                if (!repairResult) {
                    // 如果修复失败，尝试完全重置
                    Timber.e("数据库修复失败，尝试完全重置")
                    val resetResult = databaseRepairTool.completelyResetDatabase()
                    Timber.i("数据库完全重置结果: $resetResult")
                } else {
                    // 修复成功
                    Timber.i("数据库修复成功")
                }
            }
        }
    }

    /**
     * 初始化默认数据
     */
    private suspend fun initializeDefaultData() {
        try {
            Timber.i("初始化默认分类")
            categoryRepository.initDefaultCategories()

            Timber.i("初始化默认账户")
            accountRepository.initDefaultAccounts()

            Timber.i("执行数据一致性检查")
            dataIntegrityChecker.checkAndFixAllIssues()
        } catch (e: Exception) {
            Timber.e(e, "初始化默认数据失败")
        }
    }

    /**
     * 检查并初始化默认数据（如果需要）
     */
    private suspend fun checkAndInitializeDefaultData() {
        try {
            // 检查是否已经初始化了默认数据
            val categoryCount = categoryRepository.getCategoryCount(CategoryType.EXPENSE).first()
            val accountCount = accountRepository.getAllAccounts().first().size

            Timber.i("当前分类数量: $categoryCount, 账户数量: $accountCount")

            if (categoryCount == 0 || accountCount == 0) {
                Timber.i("需要初始化默认数据")
                initializeDefaultData()
            } else {
                Timber.i("默认数据已存在，跳过初始化")
            }
        } catch (e: Exception) {
            Timber.e(e, "检查默认数据失败")
        }
    }

    /**
     * 检查数据库加密状态并处理迁移
     * 注意：现在我们使用非加密数据库，所以这个方法仅仅记录日志
     */
    private fun checkDatabaseEncryption() {
        applicationScope.launch {
            Timber.i("使用非加密数据库，跳过加密检查和迁移")
            // 不再执行加密相关操作
        }
    }

    /**
     * 初始化默认数据
     * 现在调用 checkAndInitializeDefaultData 方法来检查并初始化默认数据
     */
    private fun initDefaultData() {
        // 在IO线程中执行
        applicationScope.launch {
            try {
                Timber.i("开始初始化默认数据")
                checkAndInitializeDefaultData()
            } catch (e: Exception) {
                Timber.e(e, "初始化默认数据失败")
            }
        }
    }

    /**
     * 设置预算通知定时任务
     */
    private fun setupBudgetNotificationWork() {
        // 创建定期工作请求
        val budgetCheckRequest = PeriodicWorkRequestBuilder<BudgetNotificationWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        // 安排工作，如果已存在则保留
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BUDGET_NOTIFICATION_WORK",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetCheckRequest
        )
    }

    /**
     * 设置数据一致性检查定时任务
     */
    private fun setupDataIntegrityCheck() {
        // 安排定期数据一致性检查
        dataIntegrityChecker.schedulePeriodicIntegrityCheck()
    }

    /**
     * 设置自动备份任务
     */
    private fun setupAutoBackup() {
        // 设置每3天自动备份一次
        autoBackupScheduler.scheduleAutoBackup(intervalDays = 3)

        // 设置备份提醒任务
        val backupReminderScheduler = BackupReminderWorker.Scheduler(this)
        backupReminderScheduler.scheduleBackupReminder()
    }

    /**
     * 设置WebDAV自动同步任务
     */
    private fun setupWebDavSync() {
        applicationScope.launch {
            // 恢复WebDAV自动同步设置
            val config = webDavSyncManager.getWebDavConfig()
            if (config != null && config.autoSync) {
                // 如果配置了自动同步，重新设置任务
                webDavSyncManager.saveWebDavConfig(config)
            }
        }
    }

    /**
     * 设置定期汇率更新任务
     */
    private fun setupCurrencyRateUpdateWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
            .build()

        val currencyUpdateRequest = PeriodicWorkRequestBuilder<CurrencyRateUpdateWorker>(
            24, TimeUnit.HOURS // 每24小时执行一次
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "currency_rate_update",
            ExistingPeriodicWorkPolicy.KEEP, // 如果已存在任务，保留现有任务
            currencyUpdateRequest
        )
    }

    /**
     * 设置高级功能相关的工作任务
     */
    private fun setupAdvancedFeaturesTasks() {
        applicationScope.launch {
            // 初始化高级功能相关的工作任务
            workManagerInitializer.initializeWorkTasks()
        }
    }

    /**
     * 设置数据库性能监控
     */
    private fun setupDatabasePerformanceMonitoring() {
        // 检查是否启用了数据库监控功能
        if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING)) {
            try {
                // 在后台线程中初始化数据库连接管理器
                applicationScope.launch {
                    try {
                        // 等待一段时间，确保其他初始化操作完成
                        delay(2000)

                        // 初始化数据库连接管理器
                        databaseConnectionManager.initialize()
                        Log.i("CCJiZhangApp", "数据库连接管理器初始化成功")

                        // 启动数据库性能监控
                        databasePerformanceMonitor.startMonitoring()
                        Log.i("CCJiZhangApp", "数据库性能监控启动成功")

                        // 生成性能报告
                        val reportFile = databasePerformanceMonitor.generatePerformanceReport()
                        if (reportFile != null) {
                            Log.i("CCJiZhangApp", "数据库性能报告生成成功: ${reportFile.path}")
                        }
                    } catch (e: Exception) {
                        Log.e("CCJiZhangApp", "数据库连接管理器初始化失败", e)
                    }
                }

                // 安排定期数据库性能监控任务
                val monitorScheduler = DatabasePerformanceMonitorWorker.Companion.Scheduler(this)
                monitorScheduler.schedulePeriodicMonitoring(24) // 每24小时执行一次

                // 安排定期数据库维护任务（VACUUM和检查点）
                val maintenanceScheduler = DatabaseMaintenanceWorker.Companion.Scheduler(this)
                maintenanceScheduler.schedulePeriodicMaintenance(7) // 每7天执行一次

                Log.i("CCJiZhangApp", "数据库性能监控任务已设置")
            } catch (e: Exception) {
                Log.e("CCJiZhangApp", "设置数据库性能监控失败", e)
            }
        } else {
            Log.i("CCJiZhangApp", "数据库监控功能未启用，跳过性能监控设置")
        }
    }

    /**
     * 生成数据库结构分析报告
     */
    private fun generateDatabaseStructureReport() {
        // 在后台线程中执行，避免阻塞主线程
        applicationScope.launch {
            try {
                // 检查是否启用了数据库监控功能
                if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING)) {
                    Log.i("CCJiZhangApp", "开始生成数据库结构分析报告")

                    // 创建DatabaseStructureAnalyzer实例
                    val databaseStructureAnalyzer = DatabaseStructureAnalyzer(applicationContext, appDatabase)

                    // 生成报告
                    val reportFile = databaseStructureAnalyzer.generateStructureReport()

                    if (reportFile != null) {
                        Timber.i("数据库结构分析报告生成成功: ${reportFile.path}")
                    } else {
                        Timber.e("数据库结构分析报告生成失败")
                    }
                } else {
                    Timber.i("数据库监控功能未启用，跳过结构分析")
                }
            } catch (e: Exception) {
                Timber.e(e, "生成数据库结构分析报告失败")
            }
        }
    }

    /**
     * 配置数据库同步模式
     * 使用更安全的配置确保数据持久化
     */
    private fun configureDatabaseSyncMode() {
        Timber.i("开始配置数据库模式")
        applicationScope.launch(Dispatchers.IO) {
            try {
                // 使用 WAL 模式来提高性能和可靠性
                appDatabase.query("PRAGMA journal_mode=WAL", emptyArray())
                Timber.i("数据库日志模式设置为 WAL")

                // 设置同步模弎为 FULL，提高数据安全性
                appDatabase.query("PRAGMA synchronous=FULL", emptyArray())
                Timber.i("数据库同步模弎设置为 FULL")

                // 设置锁定模式为 NORMAL
                appDatabase.query("PRAGMA locking_mode=NORMAL", emptyArray())
                Timber.i("数据库锁定模弎设置为 NORMAL")

                // 设置自动提交为开启状态
                appDatabase.query("PRAGMA auto_commit=ON", emptyArray())
                Timber.i("数据库自动提交设置为 ON")

                // 设置页缓存大小，提高性能
                appDatabase.query("PRAGMA cache_size=2000", emptyArray())
                Timber.i("数据库页缓存大小设置为 2000")

                // 验证当前日志模式
                val cursor = appDatabase.query("PRAGMA journal_mode", emptyArray())
                cursor.use {
                    if (cursor.moveToFirst()) {
                        val journalMode = cursor.getString(0)
                        Timber.i("当前日志模式: $journalMode")
                    }
                }

                // 执行 VACUUM 来清理数据库
                try {
                    appDatabase.query("VACUUM", emptyArray())
                    Timber.i("执行 VACUUM 成功")
                } catch (e: Exception) {
                    Timber.e(e, "执行 VACUUM 失败")
                }

                // 执行 PRAGMA integrity_check
                try {
                    val integrityCheckCursor = appDatabase.query("PRAGMA integrity_check", emptyArray())
                    if (integrityCheckCursor.moveToFirst()) {
                        val result = integrityCheckCursor.getString(0)
                        Timber.i("完整性检查结果: $result")
                    }
                    integrityCheckCursor.close()
                } catch (e: Exception) {
                    Timber.e(e, "执行完整性检查失败")
                }

            } catch (e: Exception) {
                Timber.e(e, "设置数据库同步模弌失败")
            }
        }
    }

    /**
     * 设置数据库维护策略
     * 定期执行数据库维护操作，减少数据库损坏风险
     */
    private fun setupDatabaseCheckpoint() {
        Timber.i("设置数据库维护策略")
        applicationScope.launch {
            // 定期执行数据库维护操作
            while (isActive) {
                delay(30 * 60 * 1000) // 每30分钟执行一次
                try {
                    // 执行 PRAGMA optimize
                    try {
                        appDatabase.query("PRAGMA optimize", emptyArray())
                        Timber.i("执行 PRAGMA optimize 成功")
                    } catch (e: Exception) {
                        Timber.e(e, "执行 PRAGMA optimize 失败")
                    }

                    // 执行 PRAGMA integrity_check
                    try {
                        val integrityCheckCursor = appDatabase.query("PRAGMA integrity_check", emptyArray())
                        if (integrityCheckCursor.moveToFirst()) {
                            val result = integrityCheckCursor.getString(0)
                            Timber.i("定期完整性检查结果: $result")
                        }
                        integrityCheckCursor.close()
                    } catch (e: Exception) {
                        Timber.e(e, "执行定期完整性检查失败")
                    }

                    // 检查是否启用了自动备份
                    if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_AUTO_BACKUP)) {
                        // 创建定期备份
                        databaseRecoveryManager.createScheduledBackup()
                        Timber.i("创建定期数据库备份成功")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "执行数据库维护或备份失败")
                }
            }
        }
    }

    /**
     * 初始化图标缓存
     */
    private fun initializeIconCache() {
        // 在后台线程中初始化图标缓存
        Thread {
            try {
                IconUtils.initializeCommonIcons()
                Timber.d("图标缓存初始化完成")
            } catch (e: Exception) {
                Timber.e(e, "图标缓存初始化失败")
            }
        }.start()
    }

    /**
     * 初始化灰度发布配置
     */
    private fun initGrayscaleReleaseConfig() {
        try {
            // 初始化默认配置
            grayscaleReleaseConfig.initDefaultConfig()

            // 在开发环境中启用所有功能
            // 始终启用所有功能，因为这是测试版本
            if (true) {
                grayscaleReleaseConfig.setUserGroup(GrayscaleReleaseConfig.GROUP_INTERNAL)

                // 基本功能配置
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE, true) // 启用WAL模式提高数据安全性
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_AUTO_BACKUP, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_CONNECTION_MANAGEMENT, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DATA_ARCHIVING, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_STRUCTURE_ANALYSIS, true)

                // 高级监控功能配置
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_ADVANCED_DB_MONITORING, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_QUERY_LOGGING, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_PERFORMANCE_ANALYSIS, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_SLOW_QUERY_DETECTION, true)
                grayscaleReleaseConfig.setFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DATABASE_ANALYZER, true)
            }

            Log.i("CCJiZhangApp", "灰度发布配置初始化成功")
        } catch (e: Exception) {
            Log.e("CCJiZhangApp", "灰度发布配置初始化失败", e)
        }
    }

    /**
     * 启动数据库监控
     */
    private fun startDatabaseMonitoring() {
        try {
            // 检查是否支持查询拦截器
            val hasQueryCallback = try {
                Class.forName("androidx.room.RoomDatabase\$QueryCallback")
                true
            } catch (e: ClassNotFoundException) {
                Log.w("CCJiZhangApp", "当前 Room 版本不支持查询拦截器，部分监控功能将受限")
                false
            }

            // 检查是否启用了数据库监控
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DB_MONITORING)) {
                databaseMonitor.startMonitoring()
                Log.i("CCJiZhangApp", "数据库监控已启动")
            } else {
                Log.i("CCJiZhangApp", "数据库监控未启用，跳过监控")
            }

            // 检查是否启用了高级数据库监控
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_ADVANCED_DB_MONITORING)) {
                try {
                    advancedDatabaseMonitor.startMonitoring()
                    Log.i("CCJiZhangApp", "高级数据库监控已启动")
                } catch (e: Exception) {
                    Log.e("CCJiZhangApp", "启动高级数据库监控失败", e)
                    // 即使高级监控启动失败，也不应该影响应用的正常运行
                }
            } else {
                Log.i("CCJiZhangApp", "高级数据库监控未启用，跳过监控")
            }

            // 检查是否启用了查询日志
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_QUERY_LOGGING)) {
                try {
                    queryLogManager.enableQueryLogging(appDatabase)
                    Log.i("CCJiZhangApp", "查询日志已启用")
                } catch (e: Exception) {
                    Log.e("CCJiZhangApp", "启用查询日志失败", e)
                    // 即使查询日志启用失败，也不应该影响应用的正常运行
                }
            } else {
                Log.i("CCJiZhangApp", "查询日志未启用，跳过日志记录")
            }

            // 检查是否启用了数据库分析
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DATABASE_ANALYZER)) {
                // 在后台线程中执行数据库分析
                applicationScope.launch {
                    try {
                        // 等待一段时间，确保数据库初始化完成
                        delay(5000)

                        // 分析数据库
                        val analysisResult = databaseAnalyzer.analyzeDatabase()
                        Log.i("CCJiZhangApp", "数据库分析完成，表数量: ${analysisResult.tables.size}")
                    } catch (e: Exception) {
                        Log.e("CCJiZhangApp", "数据库分析失败", e)
                    }
                }
                Log.i("CCJiZhangApp", "数据库分析器已启动")
            } else {
                Log.i("CCJiZhangApp", "数据库分析器未启用，跳过分析")
            }
        } catch (e: Exception) {
            Log.e("CCJiZhangApp", "启动数据库监控失败", e)
            // 即使监控启动失败，也不应该影响应用的正常运行
        }
    }

    /**
     * 在应用结束时正确关闭数据库连接并刷新日志
     */
    override fun onTerminate() {
        super.onTerminate()

        // 刷新日志缓冲区，确保所有日志都写入文件
        if (::fileLoggingTree.isInitialized) {
            fileLoggingTree.flushLogs()

            // 清理旧的日志文件，只保留最近10个
            fileLoggingTree.cleanupOldLogs(10)

            Timber.i("应用终止，日志已刷新")
        }

        try {
            // 检查是否启用了数据库连接管理
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_CONNECTION_MANAGEMENT)) {
                if (::appDatabase.isInitialized && appDatabase.isOpen) {
                    // 在关闭前执行数据库维护
                    try {
                        // 执行 PRAGMA optimize
                        appDatabase.query("PRAGMA optimize", emptyArray())
                        Log.i("CCJiZhangApp", "应用结束前执行 PRAGMA optimize 成功")

                        // 执行 VACUUM
                        appDatabase.query("VACUUM", emptyArray())
                        Log.i("CCJiZhangApp", "应用结束前执行 VACUUM 成功")
                    } catch (e: Exception) {
                        Log.e("CCJiZhangApp", "应用结束前执行数据库维护失败", e)
                    }

                    // 关闭数据库连接
                    appDatabase.close()
                    Log.i("CCJiZhangApp", "数据库连接已安全关闭")
                }
            } else {
                Log.i("CCJiZhangApp", "数据库连接管理未启用，跳过关闭操作")
            }
        } catch (e: Exception) {
            Log.e("CCJiZhangApp", "关闭数据库连接失败", e)
        }

        // 停止数据库监控
        try {
            if (::databaseMonitor.isInitialized) {
                databaseMonitor.stopMonitoring()
                Log.i("CCJiZhangApp", "数据库监控已停止")
            }

            // 停止高级数据库监控
            if (::advancedDatabaseMonitor.isInitialized) {
                advancedDatabaseMonitor.stopMonitoring()
                Log.i("CCJiZhangApp", "高级数据库监控已停止")
            }

            // 停止查询日志
            if (::queryLogManager.isInitialized && ::appDatabase.isInitialized) {
                queryLogManager.disableQueryLogging(appDatabase)
                Log.i("CCJiZhangApp", "查询日志已停止")
            }
        } catch (e: Exception) {
            Log.e("CCJiZhangApp", "停止数据库监控失败", e)
        }
    }

    /**
     * 设置数据归档
     */
    private fun setupDatabaseArchiving() {
        if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_DATA_ARCHIVING)) {
            try {
                // 初始化数据归档管理器
                applicationScope.launch {
                    databaseArchiveManager.initialize()
                    Log.i("CCJiZhangApp", "数据归档管理器初始化成功")

                    // 获取归档数据库信息
                    val archiveInfo = databaseArchiveManager.getArchiveDatabaseInfo()
                    if (archiveInfo.isNotEmpty()) {
                        Log.i("CCJiZhangApp", "当前有 ${archiveInfo.size} 个归档数据库")
                        archiveInfo.forEach { info ->
                            Log.i("CCJiZhangApp", "归档数据库: ${info.year}, 大小: ${info.formattedSize}")
                        }
                    } else {
                        Log.i("CCJiZhangApp", "当前没有归档数据库")
                    }
                }

                Log.i("CCJiZhangApp", "数据归档功能已设置")
            } catch (e: Exception) {
                Log.e("CCJiZhangApp", "设置数据归档功能失败", e)
            }
        } else {
            Log.i("CCJiZhangApp", "数据归档功能未启用")
        }
    }
}