package com.ccjizhang.data.db

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 自动化数据库迁移测试
 * 使用MigrationTestUtil工具类进行自动化测试
 */
@RunWith(AndroidJUnit4::class)
class AutomatedMigrationTest {
    private val TEST_DB = "automated-migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestUtil.getMigrationTestHelper()
    
    /**
     * 测试从版本1到版本2的自动化迁移
     */
    @Test
    @Throws(IOException::class)
    fun testAutomatedMigration1To2() {
        // 创建版本1的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 1) { db ->
            MigrationTestUtil.addTestData(db, 1)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            1,
            2,
            arrayOf(AppDatabase.MIGRATION_1_2)
        ) { db ->
            // 验证迁移结果
            assertTrue("迁移到版本2应该成功", MigrationTestUtil.validateMigration(db, 2))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM transactions WHERE note='测试交易V1'")
            assertTrue("原有交易数据应该保留", cursor.count > 0)
            cursor.close()
        }
    }
    
    /**
     * 测试从版本2到版本3的自动化迁移
     */
    @Test
    @Throws(IOException::class)
    fun testAutomatedMigration2To3() {
        // 创建版本2的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 2) { db ->
            MigrationTestUtil.addTestData(db, 2)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            2,
            3,
            arrayOf(AppDatabase.MIGRATION_2_3)
        ) { db ->
            // 验证迁移结果
            assertTrue("迁移到版本3应该成功", MigrationTestUtil.validateMigration(db, 3))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM transactions WHERE note='测试交易V2'")
            assertTrue("原有交易数据应该保留", cursor.count > 0)
            cursor.close()
        }
    }
    
    /**
     * 测试从版本3到版本4的自动化迁移
     */
    @Test
    @Throws(IOException::class)
    fun testAutomatedMigration3To4() {
        // 创建版本3的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 3) { db ->
            MigrationTestUtil.addTestData(db, 3)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            3,
            4,
            arrayOf(AppDatabase.MIGRATION_3_4)
        ) { db ->
            // 验证迁移结果
            assertTrue("迁移到版本4应该成功", MigrationTestUtil.validateMigration(db, 4))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM categories WHERE name='餐厅'")
            assertTrue("原有分类数据应该保留", cursor.count > 0)
            cursor.close()
        }
    }
    
    /**
     * 测试从版本4到版本5的自动化迁移
     */
    @Test
    @Throws(IOException::class)
    fun testAutomatedMigration4To5() {
        // 创建版本4的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 4) { db ->
            MigrationTestUtil.addTestData(db, 4)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            4,
            5,
            arrayOf(AppDatabase.MIGRATION_4_5)
        ) { db ->
            // 验证迁移结果
            assertTrue("迁移到版本5应该成功", MigrationTestUtil.validateMigration(db, 5))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM saving_goals WHERE name='买车'")
            assertTrue("原有目标数据应该保留", cursor.count > 0)
            cursor.close()
        }
    }
    
    /**
     * 测试从版本5到版本6的自动化迁移
     */
    @Test
    @Throws(IOException::class)
    fun testAutomatedMigration5To6() {
        // 创建版本5的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 5) { db ->
            MigrationTestUtil.addTestData(db, 5)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            5,
            6,
            arrayOf(AppDatabase.MIGRATION_5_6)
        ) { db ->
            // 验证迁移结果
            assertTrue("迁移到版本6应该成功", MigrationTestUtil.validateMigration(db, 6))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM transactions WHERE note='测试转账'")
            assertTrue("原有交易数据应该保留", cursor.count > 0)
            cursor.close()
            
            // 验证新字段是否有默认值
            cursor.moveToFirst()
            val createdBy = cursor.getInt(cursor.getColumnIndex("createdBy"))
            val isPrivate = cursor.getInt(cursor.getColumnIndex("isPrivate"))
            assertTrue("createdBy应该默认为0", createdBy == 0)
            assertTrue("isPrivate应该默认为0", isPrivate == 0)
            cursor.close()
        }
    }
    
    /**
     * 测试所有迁移路径
     */
    @Test
    @Throws(IOException::class)
    fun testAllMigrationPaths() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 测试所有迁移路径
        val result = MigrationTestUtil.testAllMigrations(
            context,
            TEST_DB,
            arrayOf(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
        )
        
        assertTrue("所有迁移路径应该成功", result)
    }
    
    /**
     * 测试跨版本迁移
     * 从版本1直接迁移到版本6
     */
    @Test
    @Throws(IOException::class)
    fun testCrossVersionMigration() {
        // 创建版本1的数据库并添加测试数据
        MigrationTestUtil.createDatabase(helper, 1) { db ->
            MigrationTestUtil.addTestData(db, 1)
        }
        
        // 执行迁移并验证
        MigrationTestUtil.testMigration(
            helper,
            1,
            6,
            arrayOf(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
        ) { db ->
            // 验证最终版本的结构
            assertTrue("迁移到版本6应该成功", MigrationTestUtil.validateMigration(db, 6))
            
            // 验证原有数据是否保留
            val cursor = db.query("SELECT * FROM transactions WHERE note='测试交易V1'")
            assertTrue("原有交易数据应该保留", cursor.count > 0)
            cursor.close()
            
            // 验证所有表是否存在
            val expectedTables = listOf(
                "transactions", "categories", "accounts", "budgets", 
                "budget_category_relations", "transaction_tags", "saving_goals", 
                "recurring_transactions", "family_members", "investments", "financial_reports"
            )
            
            for (tableName in expectedTables) {
                val tableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
                assertTrue("应该存在$tableName表", tableCursor.count > 0)
                tableCursor.close()
            }
        }
    }
}
