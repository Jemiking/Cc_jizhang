package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.data.db.converters.Converters
import com.ccjizhang.data.db.dao.AccountCategoryDao
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.db.dao.BudgetCategoryRelationDao
import com.ccjizhang.data.db.dao.BudgetDao
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.db.dao.FamilyMemberDao
import com.ccjizhang.data.db.dao.FinancialReportDao
import com.ccjizhang.data.db.dao.InvestmentDao
import com.ccjizhang.data.db.dao.RecurringTransactionDao
import com.ccjizhang.data.db.dao.SavingGoalDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.db.dao.TransactionTagDao
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.model.BudgetCategoryRelation
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.FamilyMember
import com.ccjizhang.data.model.FinancialReport
import com.ccjizhang.data.model.Investment
import com.ccjizhang.data.model.RecurringTransaction
import com.ccjizhang.data.model.SavingGoal
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionTag
import com.ccjizhang.utils.DatabaseEncryptionManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * 应用数据库定义
 * 使用Room持久化库，SQLCipher提供加密支持
 */
@Database(
    entities = [
        Transaction::class,
        Category::class,
        Account::class,
        Budget::class,
        BudgetCategoryRelation::class,
        TransactionTag::class,
        SavingGoal::class,
        RecurringTransaction::class,
        FamilyMember::class,
        Investment::class,
        FinancialReport::class,
        AccountCategory::class
    ],
    version = 7,  // 更新版本号为7，添加账户分类相关字段
    exportSchema = true // 启用架构导出，便于版本管理
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // 数据访问对象
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun accountCategoryDao(): AccountCategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetCategoryRelationDao(): BudgetCategoryRelationDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun financialReportDao(): FinancialReportDao
    abstract fun transactionWithAccessControlDao(): com.ccjizhang.data.db.dao.TransactionWithAccessControlDao

    companion object {
        // 单例实例
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 数据库迁移
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建账户分类表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `account_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                    """
                )

                // 修改accounts表，添加categoryId和displayOrder字段
                database.execSQL("ALTER TABLE accounts ADD COLUMN categoryId INTEGER")
                database.execSQL("ALTER TABLE accounts ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_categoryId` ON accounts (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_displayOrder` ON accounts (`displayOrder`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_categoryId_displayOrder` ON accounts (`categoryId`, `displayOrder`)")

                // 创建账户分类表的索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_account_categories_sortOrder` ON account_categories (`sortOrder`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_account_categories_isDefault` ON account_categories (`isDefault`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 在事务中执行迁移
                database.execSQL("BEGIN TRANSACTION")
                try {
                    // 添加安全相关字段
                    database.execSQL("ALTER TABLE transactions ADD COLUMN createdBy INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER")
                    database.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER")
                    database.execSQL("ALTER TABLE transactions ADD COLUMN isPrivate INTEGER NOT NULL DEFAULT 0")

                    // 初始化创建时间和更新时间为当前时间
                    val currentTime = System.currentTimeMillis()
                    database.execSQL("UPDATE transactions SET createdAt = $currentTime, updatedAt = $currentTime")

                    database.execSQL("COMMIT")
                } catch (e: Exception) {
                    database.execSQL("ROLLBACK")
                    throw e
                }
            }
        }

        /**
         * 从版本1迁移到版本2的迁移策略
         * 版本2添加了TransactionTag表和BudgetCategoryRelation表
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建TransactionTag表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `tag` TEXT NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
                )

                // 创建BudgetCategoryRelation表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budget_category_relations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `budgetId` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        FOREIGN KEY(`budgetId`) REFERENCES `budgets`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """
                )

                // 为TransactionTag表添加索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tags_transactionId` ON `transaction_tags` (`transactionId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tags_tag` ON `transaction_tags` (`tag`)")

                // 为BudgetCategoryRelation表添加索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_category_relations_budgetId` ON `budget_category_relations` (`budgetId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_category_relations_categoryId` ON `budget_category_relations` (`categoryId`)")
            }
        }

        /**
         * 从版本2迁移到版本3的迁移策略
         * 版本3添加了分类层级功能，为Category表添加了parentId和level字段
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为分类表添加父分类ID字段和层级字段
                database.execSQL("ALTER TABLE `categories` ADD COLUMN `parentId` INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE `categories` ADD COLUMN `level` INTEGER NOT NULL DEFAULT 0")

                // 为parentId添加索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_parentId` ON `categories` (`parentId`)")

                // 为默认分类设置level为0（顶级分类）
                database.execSQL("UPDATE `categories` SET `level` = 0 WHERE `parentId` IS NULL")
            }
        }

        /**
         * 从版本3迁移到版本4的迁移策略
         * 版本4添加了高级功能所需的表：SavingGoal, RecurringTransaction, FamilyMember, Investment, FinancialReport
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建SavingGoal表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saving_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL DEFAULT 0.0,
                        `accountId` INTEGER,
                        `startDate` INTEGER NOT NULL,
                        `targetDate` INTEGER NOT NULL,
                        `priority` INTEGER NOT NULL DEFAULT 3,
                        `iconUri` TEXT,
                        `color` INTEGER NOT NULL DEFAULT -13330461,
                        `note` TEXT,
                        `autoSaveAmount` REAL,
                        `autoSaveFrequencyDays` INTEGER,
                        `lastAutoSaveDate` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """
                )

                // 创建RecurringTransaction表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `fromAccountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER,
                        `firstExecutionDate` INTEGER NOT NULL,
                        `endDate` INTEGER,
                        `recurrenceType` INTEGER NOT NULL,
                        `customRecurrenceDays` INTEGER,
                        `specificRecurrenceDay` TEXT,
                        `weekdayMask` INTEGER,
                        `lastExecutionDate` INTEGER,
                        `nextExecutionDate` INTEGER NOT NULL,
                        `totalExecutions` INTEGER NOT NULL DEFAULT 0,
                        `maxExecutions` INTEGER NOT NULL DEFAULT 0,
                        `status` INTEGER NOT NULL DEFAULT 0,
                        `note` TEXT,
                        `notifyBeforeExecution` INTEGER NOT NULL DEFAULT 0,
                        `notifyDaysBefore` INTEGER,
                        `templateDataJson` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`fromAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """
                )

                // 创建FamilyMember表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `family_members` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `avatarUri` TEXT,
                        `role` INTEGER NOT NULL DEFAULT 2,
                        `email` TEXT,
                        `phone` TEXT,
                        `uniqueId` TEXT,
                        `status` INTEGER NOT NULL DEFAULT 0,
                        `lastActiveTime` INTEGER,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """
                )

                // 创建Investment表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `investments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `initialAmount` REAL NOT NULL,
                        `currentValue` REAL NOT NULL,
                        `totalReturn` REAL NOT NULL DEFAULT 0.0,
                        `accountId` INTEGER,
                        `institution` TEXT,
                        `productCode` TEXT,
                        `expectedAnnualReturn` REAL,
                        `actualAnnualReturn` REAL,
                        `riskLevel` INTEGER,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER,
                        `status` INTEGER NOT NULL DEFAULT 0,
                        `redemptionDate` INTEGER,
                        `lastValueUpdateDate` INTEGER NOT NULL,
                        `autoUpdateFrequencyDays` INTEGER,
                        `note` TEXT,
                        `attachmentsJson` TEXT,
                        `transactionHistoryJson` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """
                )

                // 创建FinancialReport表
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `financial_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL,
                        `generatedDate` INTEGER NOT NULL,
                        `totalIncome` REAL NOT NULL DEFAULT 0.0,
                        `totalExpense` REAL NOT NULL DEFAULT 0.0,
                        `netCashflow` REAL NOT NULL DEFAULT 0.0,
                        `savingRate` REAL,
                        `initialTotalAssets` REAL,
                        `finalTotalAssets` REAL,
                        `assetGrowthRate` REAL,
                        `reportDataJson` TEXT NOT NULL,
                        `configJson` TEXT,
                        `pdfUri` TEXT,
                        `shareUrl` TEXT,
                        `status` INTEGER NOT NULL DEFAULT 1,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """
                )

                // 为新表添加索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_saving_goals_accountId` ON `saving_goals` (`accountId`)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_categoryId` ON `recurring_transactions` (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_fromAccountId` ON `recurring_transactions` (`fromAccountId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_transactions_toAccountId` ON `recurring_transactions` (`toAccountId`)")

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_investments_accountId` ON `investments` (`accountId`)")
            }
        }

        /**
         * 从版本4迁移到版本5的迁移策略
         * 版本5在Transaction表中添加了toAccountId字段，用于支持转账功能
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为Transaction表添加toAccountId字段
                database.execSQL("ALTER TABLE `transactions` ADD COLUMN `toAccountId` INTEGER DEFAULT NULL")

                // 创建外键关系
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER,
                        `accountId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `isIncome` INTEGER NOT NULL,
                        `location` TEXT NOT NULL,
                        `imageUri` TEXT NOT NULL,
                        `toAccountId` INTEGER,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)

                // 转移数据
                database.execSQL("""
                    INSERT INTO `transactions_new`
                    (`id`, `amount`, `categoryId`, `accountId`, `date`, `note`, `isIncome`, `location`, `imageUri`, `toAccountId`)
                    SELECT `id`, `amount`, `categoryId`, `accountId`, `date`, `note`, `isIncome`, `location`, `imageUri`, NULL
                    FROM `transactions`
                """)

                // 删除旧表
                database.execSQL("DROP TABLE `transactions`")

                // 重命名新表
                database.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
            }
        }

        /**
         * 获取数据库实例
         * 注意：这个方法现在主要用于兼容性目的，新代码应该使用Hilt注入的AppDatabase实例
         */
        fun getDatabase(
            context: Context,
            encryptionManager: DatabaseEncryptionManager
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // 如果实例已经存在（通过Hilt创建），直接返回
                INSTANCE?.let { return it }

                try {
                    // 尝试删除可能损坏的数据库文件
                    val dbFile = context.getDatabasePath("ccjizhang_database")
                    if (dbFile.exists()) {
                        // 如果数据库文件存在，尝试删除它
                        try {
                            dbFile.delete()
                            android.util.Log.i("AppDatabase", "删除可能损坏的数据库文件成功")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "删除数据库文件失败", e)
                        }
                    }

                    // 获取新的加密密码
                    val password = encryptionManager.resetDatabasePassword() // 重置密码以确保一致性
                    val passphrase = SQLiteDatabase.getBytes(password.toCharArray())
                    val factory = SupportFactory(passphrase)

                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ccjizhang_database"
                    )
                    .fallbackToDestructiveMigration() // 如果迁移失败，允许重建数据库
                    .openHelperFactory(factory) // 使用SQLCipher加密
                    .build()

                    INSTANCE = instance
                    return instance
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "创建加密数据库失败，尝试创建非加密数据库", e)

                    // 如果加密数据库创建失败，尝试创建非加密数据库
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ccjizhang_database_plain"
                    )
                    .fallbackToDestructiveMigration() // 允许在迁移失败时重建数据库
                    .build()

                    INSTANCE = instance
                    return instance
                }
            }
        }
    }
}