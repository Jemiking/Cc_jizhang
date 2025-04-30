package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.Date

/**
 * 数据库迁移回滚测试
 * 测试迁移失败时的回滚机制
 */
@RunWith(AndroidJUnit4::class)
class MigrationRollbackTest {
    private val TEST_DB = "migration-rollback-test"
    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var backupFile: File
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // 删除已存在的测试数据库
        context.deleteDatabase(TEST_DB)
        
        // 获取数据库文件
        dbFile = context.getDatabasePath(TEST_DB)
        backupFile = File("${dbFile.path}.bak")
    }
    
    @After
    fun cleanup() {
        // 清理测试数据库
        context.deleteDatabase(TEST_DB)
        
        // 删除备份文件
        if (backupFile.exists()) {
            backupFile.delete()
        }
    }
    
    /**
     * 测试正常迁移
     */
    @Test
    @Throws(IOException::class)
    fun testSuccessfulMigration() {
        // 创建版本1的数据库
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        // 关闭数据库
        db.close()
        
        // 备份数据库
        dbFile.copyTo(backupFile, overwrite = true)
        
        // 执行迁移
        val migration = AppDatabase.MIGRATION_1_2
        val result = migrateWithRollback(dbFile, backupFile, migration)
        
        // 验证迁移成功
        assertTrue("正常迁移应该成功", result)
        
        // 验证备份文件已删除
        assertTrue("备份文件应该已删除", !backupFile.exists())
    }
    
    /**
     * 测试失败迁移的回滚
     */
    @Test
    @Throws(IOException::class)
    fun testFailedMigrationRollback() {
        // 创建版本1的数据库
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        // 关闭数据库
        db.close()
        
        // 备份数据库
        val originalSize = dbFile.length()
        dbFile.copyTo(backupFile, overwrite = true)
        
        // 创建一个会失败的迁移
        val failingMigration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 执行一个会失败的SQL语句
                database.execSQL("CREATE TABLE non_existent_table (id INTEGER PRIMARY KEY, FOREIGN KEY (invalid_column) REFERENCES non_existent_table2(id))")
            }
        }
        
        // 执行迁移
        val result = migrateWithRollback(dbFile, backupFile, failingMigration)
        
        // 验证迁移失败
        assertTrue("失败的迁移应该返回false", !result)
        
        // 验证数据库已恢复
        assertTrue("数据库文件应该存在", dbFile.exists())
        assertEquals("数据库文件大小应该与原始大小相同", originalSize, dbFile.length())
    }
    
    /**
     * 测试迁移过程中的异常处理
     */
    @Test
    @Throws(IOException::class)
    fun testMigrationExceptionHandling() {
        // 创建版本1的数据库
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        // 添加一些测试数据
        val transactionDao = db.transactionDao()
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            categoryId = 1,
            accountId = 1,
            date = Date(),
            note = "测试交易",
            isIncome = false
        )
        transactionDao.insert(transaction)
        
        // 关闭数据库
        db.close()
        
        // 备份数据库
        dbFile.copyTo(backupFile, overwrite = true)
        
        // 创建一个会抛出异常的迁移
        val exceptionMigration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                throw RuntimeException("测试迁移异常")
            }
        }
        
        // 执行迁移
        val result = migrateWithRollback(dbFile, backupFile, exceptionMigration)
        
        // 验证迁移失败
        assertTrue("抛出异常的迁移应该返回false", !result)
        
        // 验证数据库已恢复
        assertTrue("数据库文件应该存在", dbFile.exists())
        
        // 重新打开数据库，验证数据是否完好
        val reopenedDb = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        val transactions = reopenedDb.transactionDao().getAllTransactionsSync()
        assertEquals("应该有1条交易记录", 1, transactions.size)
        assertEquals("交易记录内容应该正确", "测试交易", transactions[0].note)
        
        reopenedDb.close()
    }
    
    /**
     * 测试多版本迁移的回滚
     */
    @Test
    @Throws(IOException::class)
    fun testMultiVersionMigrationRollback() {
        // 创建版本1的数据库
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .fallbackToDestructiveMigration()
            .build()
        
        // 关闭数据库
        db.close()
        
        // 备份数据库
        dbFile.copyTo(backupFile, overwrite = true)
        
        // 执行多个迁移，其中一个会失败
        val migrations = arrayOf(
            AppDatabase.MIGRATION_1_2,
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // 这个迁移会失败
                    database.execSQL("ALTER TABLE non_existent_table ADD COLUMN test TEXT")
                }
            }
        )
        
        // 执行迁移
        val result = migrateWithRollback(dbFile, backupFile, *migrations)
        
        // 验证迁移失败
        assertTrue("包含失败迁移的多版本迁移应该返回false", !result)
        
        // 验证数据库已恢复
        assertTrue("数据库文件应该存在", dbFile.exists())
    }
    
    /**
     * 使用回滚机制执行迁移
     * @param dbFile 数据库文件
     * @param backupFile 备份文件
     * @param migrations 迁移对象数组
     * @return 是否成功
     */
    private fun migrateWithRollback(
        dbFile: File,
        backupFile: File,
        vararg migrations: Migration
    ): Boolean {
        return try {
            // 打开数据库
            val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
                .addMigrations(*migrations)
                .build()
            
            // 验证数据库是否成功打开
            val isOpen = db.isOpen
            
            // 关闭数据库
            db.close()
            
            // 迁移成功，删除备份
            if (backupFile.exists()) {
                backupFile.delete()
            }
            
            isOpen
        } catch (e: Exception) {
            // 迁移失败，恢复备份
            if (backupFile.exists()) {
                if (dbFile.exists()) {
                    dbFile.delete()
                }
                backupFile.copyTo(dbFile, overwrite = true)
            }
            
            false
        }
    }
}
