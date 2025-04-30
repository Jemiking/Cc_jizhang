package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.Date

/**
 * 数据库迁移测试工具类
 * 提供自动化迁移测试的辅助方法
 */
class MigrationTestUtil {
    companion object {
        private const val TEST_DB = "migration-test-util"
        
        /**
         * 获取MigrationTestHelper实例
         */
        fun getMigrationTestHelper(): MigrationTestHelper {
            return MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                AppDatabase::class.java.canonicalName,
                FrameworkSQLiteOpenHelperFactory()
            )
        }
        
        /**
         * 创建指定版本的测试数据库
         * @param helper MigrationTestHelper实例
         * @param version 数据库版本
         * @param setupCallback 数据库设置回调，用于添加测试数据
         * @return 创建的数据库
         */
        @Throws(IOException::class)
        fun createDatabase(
            helper: MigrationTestHelper,
            version: Int,
            setupCallback: ((SupportSQLiteDatabase) -> Unit)? = null
        ): SupportSQLiteDatabase {
            return helper.createDatabase(TEST_DB, version).apply {
                setupCallback?.invoke(this)
                close()
            }
        }
        
        /**
         * 执行迁移并验证
         * @param helper MigrationTestHelper实例
         * @param startVersion 起始版本
         * @param endVersion 目标版本
         * @param migrations 迁移对象数组
         * @param validationCallback 验证回调，用于验证迁移结果
         */
        @Throws(IOException::class)
        fun testMigration(
            helper: MigrationTestHelper,
            startVersion: Int,
            endVersion: Int,
            migrations: Array<androidx.room.migration.Migration>,
            validationCallback: ((SupportSQLiteDatabase) -> Unit)? = null
        ) {
            val db = helper.runMigrationsAndValidate(TEST_DB, endVersion, true, *migrations)
            validationCallback?.invoke(db)
            db.close()
        }
        
        /**
         * 测试所有迁移路径
         * @param context 应用上下文
         * @param dbName 数据库名称
         * @param migrations 所有迁移对象
         * @return 是否成功
         */
        @Throws(IOException::class)
        fun testAllMigrations(
            context: Context,
            dbName: String,
            migrations: Array<androidx.room.migration.Migration>
        ): Boolean {
            // 删除已存在的测试数据库
            context.deleteDatabase(dbName)
            
            // 创建数据库并应用所有迁移
            val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(*migrations)
                .build()
            
            // 验证数据库是否成功打开
            val isOpen = db.isOpen
            
            // 关闭数据库
            db.close()
            
            return isOpen
        }
        
        /**
         * 为指定版本的数据库添加测试数据
         * @param db 数据库实例
         * @param version 数据库版本
         */
        fun addTestData(db: SupportSQLiteDatabase, version: Int) {
            when (version) {
                1 -> {
                    // 版本1的测试数据
                    db.execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (100.0, 1, 1, ${Date().time}, '测试交易V1', 0)")
                    db.execSQL("INSERT INTO categories (name, type, iconName, color) VALUES ('餐饮', 0, 'food', -1)")
                    db.execSQL("INSERT INTO accounts (name, balance, type, color) VALUES ('现金', 1000.0, 0, -1)")
                }
                2 -> {
                    // 版本2的测试数据
                    db.execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (200.0, 1, 1, ${Date().time}, '测试交易V2', 1)")
                    db.execSQL("INSERT INTO transaction_tags (transactionId, tag) VALUES (1, '测试标签')")
                }
                3 -> {
                    // 版本3的测试数据
                    db.execSQL("INSERT INTO categories (name, type, iconName, color, parentId, level) VALUES ('餐厅', 0, 'restaurant', -1, 1, 1)")
                }
                4 -> {
                    // 版本4的测试数据
                    db.execSQL("INSERT INTO saving_goals (name, targetAmount, currentAmount, startDate, targetDate, priority, createdAt) VALUES ('买车', 200000.0, 50000.0, ${Date().time}, ${Date().time + 31536000000}, 1, ${Date().time})")
                    db.execSQL("INSERT INTO family_members (name, role, status) VALUES ('测试成员', 1, 0)")
                }
                5 -> {
                    // 版本5的测试数据
                    db.execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome, toAccountId) VALUES (300.0, 1, 1, ${Date().time}, '测试转账', 0, 2)")
                }
            }
        }
        
        /**
         * 验证迁移后的数据库结构
         * @param db 数据库实例
         * @param version 数据库版本
         * @return 验证结果
         */
        fun validateMigration(db: SupportSQLiteDatabase, version: Int): Boolean {
            return when (version) {
                2 -> {
                    // 验证版本2的结构
                    val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transaction_tags'")
                    val result = cursor.count > 0
                    cursor.close()
                    result
                }
                3 -> {
                    // 验证版本3的结构
                    val cursor = db.query("PRAGMA table_info(categories)")
                    var hasParentId = false
                    var hasLevel = false
                    
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndex("name"))
                        if (columnName == "parentId") hasParentId = true
                        if (columnName == "level") hasLevel = true
                    }
                    cursor.close()
                    
                    hasParentId && hasLevel
                }
                4 -> {
                    // 验证版本4的结构
                    val tableNames = listOf("saving_goals", "recurring_transactions", "family_members", "investments", "financial_reports")
                    var allTablesExist = true
                    
                    for (tableName in tableNames) {
                        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
                        if (cursor.count == 0) {
                            allTablesExist = false
                        }
                        cursor.close()
                    }
                    
                    allTablesExist
                }
                5 -> {
                    // 验证版本5的结构
                    val cursor = db.query("PRAGMA table_info(transactions)")
                    var hasToAccountId = false
                    
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndex("name"))
                        if (columnName == "toAccountId") hasToAccountId = true
                    }
                    cursor.close()
                    
                    hasToAccountId
                }
                6 -> {
                    // 验证版本6的结构
                    val cursor = db.query("PRAGMA table_info(transactions)")
                    var hasCreatedBy = false
                    var hasCreatedAt = false
                    var hasUpdatedAt = false
                    var hasIsPrivate = false
                    
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(cursor.getColumnIndex("name"))
                        if (columnName == "createdBy") hasCreatedBy = true
                        if (columnName == "createdAt") hasCreatedAt = true
                        if (columnName == "updatedAt") hasUpdatedAt = true
                        if (columnName == "isPrivate") hasIsPrivate = true
                    }
                    cursor.close()
                    
                    hasCreatedBy && hasCreatedAt && hasUpdatedAt && hasIsPrivate
                }
                else -> false
            }
        }
    }
}
