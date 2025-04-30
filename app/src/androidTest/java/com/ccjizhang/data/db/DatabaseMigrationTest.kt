package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

/**
 * 数据库迁移测试
 * 测试所有数据库版本之间的迁移路径
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"
    
    // 使用MigrationTestHelper来测试迁移
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    
    /**
     * 测试从版本1迁移到版本2
     * 版本2添加了TransactionTag表和BudgetCategoryRelation表
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // 创建版本1的数据库
        val db = helper.createDatabase(TEST_DB, 1).apply {
            // 在版本1的数据库中添加一些数据
            execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (100.0, 1, 1, ${Date().time}, '测试交易', 0)")
            close()
        }
        
        // 执行迁移
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)
        
        // 验证迁移后的数据库结构
        val cursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transaction_tags'")
        assertTrue("应该存在transaction_tags表", cursor.count > 0)
        cursor.close()
        
        // 验证原有数据是否保留
        val transactionCursor = migratedDb.query("SELECT * FROM transactions")
        assertEquals("原有交易数据应该保留", 1, transactionCursor.count)
        transactionCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试从版本2迁移到版本3
     * 版本3添加了分类层级功能，为Category表添加了parentId和level字段
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        // 创建版本2的数据库
        val db = helper.createDatabase(TEST_DB, 2).apply {
            // 在版本2的数据库中添加一些数据
            execSQL("INSERT INTO categories (name, type, iconName, color) VALUES ('餐饮', 0, 'food', -1)")
            close()
        }
        
        // 执行迁移
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3)
        
        // 验证迁移后的数据库结构
        val cursor = migratedDb.query("PRAGMA table_info(categories)")
        var hasParentId = false
        var hasLevel = false
        
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndex("name"))
            if (columnName == "parentId") hasParentId = true
            if (columnName == "level") hasLevel = true
        }
        cursor.close()
        
        assertTrue("categories表应该有parentId列", hasParentId)
        assertTrue("categories表应该有level列", hasLevel)
        
        // 验证原有数据是否保留并正确设置了level
        val categoryCursor = migratedDb.query("SELECT * FROM categories WHERE name='餐饮'")
        categoryCursor.moveToFirst()
        val level = categoryCursor.getInt(categoryCursor.getColumnIndex("level"))
        assertEquals("默认分类的level应该为0", 0, level)
        categoryCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试从版本3迁移到版本4
     * 版本4添加了高级功能所需的表：SavingGoal, RecurringTransaction, FamilyMember, Investment, FinancialReport
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        // 创建版本3的数据库
        val db = helper.createDatabase(TEST_DB, 3).apply {
            // 在版本3的数据库中添加一些数据
            execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (200.0, 1, 1, ${Date().time}, '测试交易2', 1)")
            close()
        }
        
        // 执行迁移
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 4, true, AppDatabase.MIGRATION_3_4)
        
        // 验证迁移后的数据库结构 - 检查新表是否存在
        val tableNames = listOf("saving_goals", "recurring_transactions", "family_members", "investments", "financial_reports")
        for (tableName in tableNames) {
            val cursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertTrue("应该存在$tableName表", cursor.count > 0)
            cursor.close()
        }
        
        // 验证原有数据是否保留
        val transactionCursor = migratedDb.query("SELECT * FROM transactions")
        assertEquals("原有交易数据应该保留", 1, transactionCursor.count)
        transactionCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试从版本4迁移到版本5
     * 版本5在Transaction表中添加了toAccountId字段，用于支持转账功能
     */
    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // 创建版本4的数据库
        val db = helper.createDatabase(TEST_DB, 4).apply {
            // 在版本4的数据库中添加一些数据
            execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (300.0, 1, 1, ${Date().time}, '测试交易3', 0)")
            close()
        }
        
        // 执行迁移
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)
        
        // 验证迁移后的数据库结构
        val cursor = migratedDb.query("PRAGMA table_info(transactions)")
        var hasToAccountId = false
        
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndex("name"))
            if (columnName == "toAccountId") hasToAccountId = true
        }
        cursor.close()
        
        assertTrue("transactions表应该有toAccountId列", hasToAccountId)
        
        // 验证原有数据是否保留
        val transactionCursor = migratedDb.query("SELECT * FROM transactions")
        assertEquals("原有交易数据应该保留", 1, transactionCursor.count)
        transactionCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试从版本5迁移到版本6
     * 版本6添加了安全相关字段：createdBy, createdAt, updatedAt, isPrivate
     */
    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // 创建版本5的数据库
        val db = helper.createDatabase(TEST_DB, 5).apply {
            // 在版本5的数据库中添加一些数据
            execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome, toAccountId) VALUES (400.0, 1, 1, ${Date().time}, '测试交易4', 1, 2)")
            close()
        }
        
        // 执行迁移
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 6, true, AppDatabase.MIGRATION_5_6)
        
        // 验证迁移后的数据库结构
        val cursor = migratedDb.query("PRAGMA table_info(transactions)")
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
        
        assertTrue("transactions表应该有createdBy列", hasCreatedBy)
        assertTrue("transactions表应该有createdAt列", hasCreatedAt)
        assertTrue("transactions表应该有updatedAt列", hasUpdatedAt)
        assertTrue("transactions表应该有isPrivate列", hasIsPrivate)
        
        // 验证原有数据是否保留并正确设置了默认值
        val transactionCursor = migratedDb.query("SELECT * FROM transactions")
        transactionCursor.moveToFirst()
        val createdBy = transactionCursor.getInt(transactionCursor.getColumnIndex("createdBy"))
        val isPrivate = transactionCursor.getInt(transactionCursor.getColumnIndex("isPrivate"))
        assertEquals("createdBy应该默认为0", 0, createdBy)
        assertEquals("isPrivate应该默认为0", 0, isPrivate)
        assertTrue("createdAt应该有值", transactionCursor.getLong(transactionCursor.getColumnIndex("createdAt")) > 0)
        assertTrue("updatedAt应该有值", transactionCursor.getLong(transactionCursor.getColumnIndex("updatedAt")) > 0)
        transactionCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试所有迁移路径
     * 从版本1迁移到最新版本
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // 创建版本1的数据库
        val db = helper.createDatabase(TEST_DB, 1).apply {
            // 在版本1的数据库中添加一些数据
            execSQL("INSERT INTO transactions (amount, categoryId, accountId, date, note, isIncome) VALUES (500.0, 1, 1, ${Date().time}, '测试交易5', 0)")
            close()
        }
        
        // 执行所有迁移
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB, 
            6, 
            true, 
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6
        )
        
        // 验证最终数据库结构
        // 检查所有表是否存在
        val expectedTables = listOf(
            "transactions", "categories", "accounts", "budgets", 
            "budget_category_relations", "transaction_tags", "saving_goals", 
            "recurring_transactions", "family_members", "investments", "financial_reports"
        )
        
        for (tableName in expectedTables) {
            val cursor = migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
            assertTrue("应该存在$tableName表", cursor.count > 0)
            cursor.close()
        }
        
        // 验证原有数据是否保留
        val transactionCursor = migratedDb.query("SELECT * FROM transactions")
        assertEquals("原有交易数据应该保留", 1, transactionCursor.count)
        transactionCursor.close()
        
        migratedDb.close()
    }
    
    /**
     * 测试使用Room API进行迁移
     */
    @Test
    @Throws(IOException::class)
    fun testRoomMigration() {
        // 创建一个测试数据库
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 删除已存在的测试数据库
        context.deleteDatabase(TEST_DB)
        
        // 创建版本1的数据库
        var db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        // 关闭数据库
        db.close()
        
        // 使用所有迁移重新打开数据库
        db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
        
        // 验证数据库是否成功打开
        assertTrue("数据库应该成功打开", db.isOpen)
        
        // 关闭数据库
        db.close()
    }
}
