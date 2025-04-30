package com.ccjizhang.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.dao.AccountCategoryDao
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.db.dao.BudgetDao
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.db.dao.TransactionTagDao
import com.ccjizhang.data.db.dao.BudgetCategoryRelationDao
import com.ccjizhang.data.db.dao.FamilyMemberDao
import com.ccjizhang.data.db.dao.FinancialReportDao
import com.ccjizhang.data.db.dao.InvestmentDao
import com.ccjizhang.data.db.dao.RecurringTransactionDao
import com.ccjizhang.data.db.dao.SavingGoalDao
import com.ccjizhang.data.db.dao.TransactionWithAccessControlDao
import com.ccjizhang.data.db.TransactionManager
import com.ccjizhang.data.repository.AccountCategoryRepository
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.BudgetRepository
import com.ccjizhang.utils.DatabaseEncryptionManager
import net.sqlcipher.database.SupportFactory
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.data.repository.OptimizedTransactionRepository
import com.ccjizhang.data.repository.TransactionRepository
import com.ccjizhang.data.repository.TransactionWithAccessControlRepository
import com.ccjizhang.data.repository.TagRepository
import com.ccjizhang.utils.AppDataMigrationHelper
import com.ccjizhang.utils.AutoBackupWorker
import androidx.room.RoomDatabase.JournalMode
import com.ccjizhang.utils.AccessControlHelper
import com.ccjizhang.utils.DatabaseExceptionHandler
import com.ccjizhang.utils.DatabaseMonitor
import com.ccjizhang.utils.DatabaseRepairTool
import com.ccjizhang.utils.QueryPlanAnalyzer
import com.ccjizhang.data.db.DatabaseConnectionManager
import com.ccjizhang.data.db.DatabasePerformanceMonitor
import com.ccjizhang.utils.QueryLogManager
import com.ccjizhang.utils.DatabaseAnalyzer
import com.ccjizhang.utils.AdvancedDatabaseMonitor
import com.ccjizhang.utils.DatabaseSecurityManager
import com.ccjizhang.utils.DataIntegrityChecker
import com.ccjizhang.utils.FileLoggingTree
import com.ccjizhang.utils.GrayscaleReleaseConfig
import com.ccjizhang.utils.LogViewerHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用依赖注入模块
 * 提供数据库、DAO和Repository的实例
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供数据库加密管理器
     */
    @Singleton
    @Provides
    fun provideDatabaseEncryptionManager(@ApplicationContext context: Context): DatabaseEncryptionManager {
        return DatabaseEncryptionManager(context)
    }

    /**
     * 提供数据库实例
     */
    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        encryptionManager: DatabaseEncryptionManager,
        grayscaleReleaseConfig: GrayscaleReleaseConfig
    ): AppDatabase {
        try {
            // 检查数据库文件是否存在
            val dbFile = context.getDatabasePath("ccjizhang_database_plain")
            val dbExists = dbFile.exists()

            android.util.Log.i("AppModule", "检查数据库文件是否存在: $dbExists")

            // 如果数据库文件不存在，记录日志
            if (!dbExists) {
                android.util.Log.i("AppModule", "数据库文件不存在，将创建新数据库")
            } else {
                android.util.Log.i("AppModule", "数据库文件已存在，保留现有数据")
            }

            // 直接创建非加密数据库，避免加密相关问题
            android.util.Log.i("AppModule", "创建非加密数据库")

            // 使用固定的数据库名称，确保一致性
            val DB_NAME = "ccjizhang_database_plain"
            android.util.Log.i("AppModule", "使用数据库名称: $DB_NAME")

            val plainDatabaseBuilder = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )

            // 如果启用了WAL模式，设置数据库日志模式
            if (grayscaleReleaseConfig.isFeatureEnabled(GrayscaleReleaseConfig.FEATURE_WAL_MODE)) {
                try {
                    // 直接使用JournalMode枚举
                    plainDatabaseBuilder.setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    android.util.Log.i("AppModule", "成功设置数据库日志模式为WAL")
                } catch (e: Exception) {
                    android.util.Log.e("AppModule", "设置数据库日志模式失败", e)
                    // 如果设置失败，继续使用默认模式
                }
            } else {
                android.util.Log.i("AppModule", "使用默认数据库日志模式")
            }

            // 设置数据库配置
            plainDatabaseBuilder
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3,
                              AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                              AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
                .fallbackToDestructiveMigration() // 允许在迁移失败时重建数据库

            // 构建并返回数据库实例
            val db = plainDatabaseBuilder.build()
            android.util.Log.i("AppModule", "数据库创建成功")
            return db
        } catch (e: Exception) {
            android.util.Log.e("AppModule", "创建非加密数据库失败", e)

            // 如果创建失败，尝试创建一个最简单的数据库
            try {
                android.util.Log.i("AppModule", "尝试创建最简单的数据库")

                // 使用相同的数据库名称，确保一致性
                val DB_NAME = "ccjizhang_database_plain"
                android.util.Log.i("AppModule", "使用数据库名称: $DB_NAME")

                // 创建一个最简单的数据库，不使用迁移或特殊设置
                val db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DB_NAME
                )
                .fallbackToDestructiveMigration()
                .build()

                android.util.Log.i("AppModule", "最简单数据库创建成功")
                return db
            } catch (e2: Exception) {
                android.util.Log.e("AppModule", "创建最简单的数据库也失败", e2)
                throw e2 // 如果这也失败，则抛出异常，让应用崩溃并显示错误
            }
        }
    }

    /**
     * 提供应用上下文
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    /**
     * 提供交易DAO
     */
    @Singleton
    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    /**
     * 提供带访问控制的交易DAO
     */
    @Singleton
    @Provides
    fun provideTransactionWithAccessControlDao(database: AppDatabase): TransactionWithAccessControlDao {
        return database.transactionWithAccessControlDao()
    }

    /**
     * 提供分类DAO
     */
    @Singleton
    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * 提供账户DAO
     */
    @Singleton
    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    /**
     * 提供账户分类DAO
     */
    @Singleton
    @Provides
    fun provideAccountCategoryDao(database: AppDatabase): AccountCategoryDao {
        return database.accountCategoryDao()
    }

    /**
     * 提供预算DAO
     */
    @Singleton
    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    /**
     * 提供交易标签DAO
     */
    @Singleton
    @Provides
    fun provideTransactionTagDao(database: AppDatabase): TransactionTagDao {
        return database.transactionTagDao()
    }

    /**
     * 提供预算分类关系DAO
     */
    @Singleton
    @Provides
    fun provideBudgetCategoryRelationDao(database: AppDatabase): BudgetCategoryRelationDao {
        return database.budgetCategoryRelationDao()
    }

    /**
     * 提供账户Repository
     * 注意：必须先提供AccountRepository，因为TransactionRepository依赖它
     */
    @Singleton
    @Provides
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository {
        return AccountRepository(accountDao)
    }

    /**
     * 提供账户分类Repository
     */
    @Singleton
    @Provides
    fun provideAccountCategoryRepository(accountCategoryDao: AccountCategoryDao): AccountCategoryRepository {
        return AccountCategoryRepository(accountCategoryDao)
    }

    /**
     * 提供交易Repository
     */
    @Singleton
    @Provides
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        accountRepository: AccountRepository,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): TransactionRepository {
        return TransactionRepository(transactionDao, accountRepository, databaseExceptionHandler)
    }

    /**
     * 提供带访问控制的交易Repository
     */
    @Singleton
    @Provides
    fun provideTransactionWithAccessControlRepository(
        transactionWithAccessControlDao: TransactionWithAccessControlDao,
        accessControlHelper: AccessControlHelper,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): TransactionWithAccessControlRepository {
        return TransactionWithAccessControlRepository(transactionWithAccessControlDao, accessControlHelper, databaseExceptionHandler)
    }

    /**
     * 提供优化的交易Repository
     */
    @Singleton
    @Provides
    fun provideOptimizedTransactionRepository(
        transactionDao: TransactionDao,
        accountRepository: AccountRepository,
        transactionManager: TransactionManager,
        databaseExceptionHandler: DatabaseExceptionHandler
    ): OptimizedTransactionRepository {
        return OptimizedTransactionRepository(transactionDao, accountRepository, transactionManager, databaseExceptionHandler)
    }

    /**
     * 提供分类Repository
     */
    @Singleton
    @Provides
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepository(categoryDao)
    }

    /**
     * 提供预算Repository
     */
    @Singleton
    @Provides
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        transactionDao: TransactionDao
    ): BudgetRepository {
        return BudgetRepository(budgetDao, transactionDao)
    }

    /**
     * 提供标签Repository
     */
    @Singleton
    @Provides
    fun provideTagRepository(
        transactionTagDao: TransactionTagDao
    ): TagRepository {
        return TagRepository(transactionTagDao)
    }

    /**
     * 提供数据导入导出Repository
     */
    @Singleton
    @Provides
    fun provideDataExportImportRepository(
        appDatabase: AppDatabase,
        transactionDao: TransactionDao,
        categoryDao: CategoryDao,
        accountDao: AccountDao,
        budgetDao: BudgetDao
    ): DataExportImportRepository {
        return DataExportImportRepository(
            appDatabase,
            transactionDao,
            categoryDao,
            accountDao,
            budgetDao
        )
    }

    /**
     * 提供分类建议服务
     */
    @Singleton
    @Provides
    fun provideCategorySuggestionService(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository
    ): com.ccjizhang.data.service.CategorySuggestionService {
        return com.ccjizhang.data.service.CategorySuggestionService(
            transactionRepository,
            categoryRepository
        )
    }

    /**
     * 提供数据一致性检查工具
     */
    @Singleton
    @Provides
    fun provideDataIntegrityChecker(
        @ApplicationContext context: Context,
        transactionRepository: TransactionRepository,
        accountRepository: AccountRepository,
        appDatabase: AppDatabase,
        grayscaleReleaseConfig: GrayscaleReleaseConfig
    ): DataIntegrityChecker {
        return DataIntegrityChecker(context, transactionRepository, accountRepository, appDatabase, grayscaleReleaseConfig)
    }

    /**
     * 提供自动备份调度器
     */
    @Singleton
    @Provides
    fun provideAutoBackupScheduler(
        @ApplicationContext context: Context
    ): AutoBackupWorker.Scheduler {
        return AutoBackupWorker.Scheduler(context)
    }

    /**
     * 提供应用间数据迁移助手
     */
    @Singleton
    @Provides
    fun provideAppDataMigrationHelper(
        @ApplicationContext context: Context,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        accountRepository: AccountRepository
    ): AppDataMigrationHelper {
        return AppDataMigrationHelper(
            context,
            transactionRepository,
            categoryRepository,
            accountRepository
        )
    }

    /**
     * 提供投资DAO
     */
    @Singleton
    @Provides
    fun provideInvestmentDao(database: AppDatabase): InvestmentDao {
        return database.investmentDao()
    }

    /**
     * 提供财务报告DAO
     */
    @Singleton
    @Provides
    fun provideFinancialReportDao(database: AppDatabase): FinancialReportDao {
        return database.financialReportDao()
    }

    /**
     * 提供定期交易DAO
     */
    @Singleton
    @Provides
    fun provideRecurringTransactionDao(database: AppDatabase): RecurringTransactionDao {
        return database.recurringTransactionDao()
    }

    /**
     * 提供储蓄目标DAO
     */
    @Singleton
    @Provides
    fun provideSavingGoalDao(database: AppDatabase): SavingGoalDao {
        return database.savingGoalDao()
    }

    /**
     * 提供家庭成员DAO
     */
    @Singleton
    @Provides
    fun provideFamilyMemberDao(database: AppDatabase): FamilyMemberDao {
        return database.familyMemberDao()
    }

    /**
     * 提供数据库修复工具
     */
    @Singleton
    @Provides
    fun provideDatabaseRepairTool(
        @ApplicationContext context: Context,
        categoryRepository: CategoryRepository,
        accountRepository: AccountRepository
    ): DatabaseRepairTool {
        return DatabaseRepairTool(context, categoryRepository, accountRepository)
    }

    /**
     * 提供数据库异常处理器
     */
    @Singleton
    @Provides
    fun provideDatabaseExceptionHandler(
        databaseRepairTool: DatabaseRepairTool
    ): DatabaseExceptionHandler {
        return DatabaseExceptionHandler(databaseRepairTool)
    }

    /**
     * 提供灰度发布配置
     */
    @Singleton
    @Provides
    fun provideGrayscaleReleaseConfig(
        @ApplicationContext context: Context
    ): GrayscaleReleaseConfig {
        return GrayscaleReleaseConfig(context)
    }

    /**
     * 提供数据库监控工具
     */
    @Singleton
    @Provides
    fun provideDatabaseMonitor(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        grayscaleReleaseConfig: GrayscaleReleaseConfig
    ): DatabaseMonitor {
        return DatabaseMonitor(context, appDatabase, grayscaleReleaseConfig)
    }

    /**
     * 提供高级数据库监控工具
     */
    @Singleton
    @Provides
    fun provideAdvancedDatabaseMonitor(
        @ApplicationContext context: Context,
        appDatabase: AppDatabase,
        databaseMonitor: DatabaseMonitor,
        databasePerformanceMonitor: DatabasePerformanceMonitor,
        connectionManager: DatabaseConnectionManager,
        grayscaleReleaseConfig: GrayscaleReleaseConfig
    ): AdvancedDatabaseMonitor {
        return AdvancedDatabaseMonitor(context, appDatabase, databaseMonitor, databasePerformanceMonitor, connectionManager, grayscaleReleaseConfig)
    }

    /**
     * 提供查询日志管理器
     */
    @Singleton
    @Provides
    fun provideQueryLogManager(
        @ApplicationContext context: Context,
        grayscaleReleaseConfig: GrayscaleReleaseConfig
    ): QueryLogManager {
        return QueryLogManager(context, grayscaleReleaseConfig)
    }

    /**
     * 提供文件日志树
     */
    @Singleton
    @Provides
    fun provideFileLoggingTree(
        @ApplicationContext context: Context
    ): FileLoggingTree {
        return FileLoggingTree(context)
    }

    /**
     * 提供日志查看器帮助类
     */
    @Singleton
    @Provides
    fun provideLogViewerHelper(
        @ApplicationContext context: Context,
        fileLoggingTree: FileLoggingTree
    ): LogViewerHelper {
        return LogViewerHelper(context, fileLoggingTree)
    }

    /**
     * 提供数据库分析器
     */
    @Singleton
    @Provides
    fun provideDatabaseAnalyzer(
        @ApplicationContext context: Context,
        connectionManager: DatabaseConnectionManager
    ): DatabaseAnalyzer {
        return DatabaseAnalyzer(context, connectionManager)
    }

    /**
     * 提供查询计划分析器
     */
    @Singleton
    @Provides
    fun provideQueryPlanAnalyzer(
        @ApplicationContext context: Context,
        connectionManager: DatabaseConnectionManager
    ): QueryPlanAnalyzer {
        return QueryPlanAnalyzer(context, connectionManager)
    }

    /**
     * 提供数据库安全管理器
     */
    @Provides
    @Singleton
    fun provideDatabaseSecurityManager(
        @ApplicationContext context: Context
    ): DatabaseSecurityManager {
        return DatabaseSecurityManager(context).apply {
            // 初始化安全管理器
            initialize()
        }
    }

    /**
     * 提供访问控制助手
     */
    @Provides
    @Singleton
    fun provideAccessControlHelper(
        familyMemberDao: FamilyMemberDao,
        databaseSecurityManager: DatabaseSecurityManager
    ): AccessControlHelper {
        return AccessControlHelper(familyMemberDao, databaseSecurityManager)
    }

    /**
     * 提供备份提醒调度器
     */
    @Provides
    @Singleton
    fun provideBackupReminderScheduler(
        @ApplicationContext context: Context
    ): com.ccjizhang.utils.BackupReminderWorker.Scheduler {
        return com.ccjizhang.utils.BackupReminderWorker.Scheduler(context)
    }
}