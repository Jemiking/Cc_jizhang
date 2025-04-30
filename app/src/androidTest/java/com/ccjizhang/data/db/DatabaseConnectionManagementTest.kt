package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ccjizhang.CCJiZhangApp
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 数据库连接管理集成测试
 * 测试应用生命周期中的数据库连接管理
 */
@RunWith(AndroidJUnit4::class)
class DatabaseConnectionManagementTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        
        // 创建内存数据库用于测试
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        if (::db.isInitialized && db.isOpen) {
            db.close()
        }
    }

    /**
     * 测试数据库连接正确关闭
     */
    @Test
    fun testDatabaseConnectionClose() {
        // 验证数据库初始状态是打开的
        assertTrue("数据库应该处于打开状态", db.isOpen)
        
        // 关闭数据库
        db.close()
        
        // 验证数据库已关闭
        assertFalse("数据库应该已关闭", db.isOpen)
    }

    /**
     * 测试在关闭前执行检查点操作
     */
    @Test
    fun testCheckpointBeforeClose() = runBlocking {
        // 执行一些写入操作
        val transactionDao = db.transactionDao()
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "测试交易",
            categoryId = null,
            accountId = 1,
            toAccountId = null,
            isRecurring = false
        )
        transactionDao.insert(transaction)
        
        // 执行检查点操作
        val cursor = db.query("PRAGMA wal_checkpoint(FULL)", null)
        cursor.moveToFirst()
        
        // 验证检查点操作结果
        // 返回值是一个包含3个整数的数组：
        // 1. 检查点是否完成（0表示完成）
        // 2. WAL帧的总数
        // 3. 已检查点的帧数
        val checkpointResult = cursor.getInt(0)
        assertEquals("检查点操作应该成功完成", 0, checkpointResult)
        
        cursor.close()
        
        // 关闭数据库
        db.close()
        
        // 验证数据库已关闭
        assertFalse("数据库应该已关闭", db.isOpen)
    }

    /**
     * 测试并发数据库访问
     */
    @Test
    fun testConcurrentDatabaseAccess() = runBlocking {
        val transactionDao = db.transactionDao()
        val accountDao = db.accountDao()
        
        // 创建测试账户
        val account = Account(
            id = 1,
            name = "测试账户",
            balance = 1000.0,
            type = "现金",
            color = "#FF0000",
            icon = "cash",
            isDefault = true,
            sortOrder = 0,
            isArchived = false
        )
        accountDao.insert(account)
        
        // 并发执行多个数据库操作
        val numThreads = 10
        val operationsPerThread = 10
        val latch = CountDownLatch(numThreads)
        
        val jobs = List(numThreads) { threadId ->
            async(Dispatchers.IO) {
                try {
                    repeat(operationsPerThread) { i ->
                        val transaction = Transaction(
                            id = threadId * operationsPerThread + i + 1,
                            amount = 10.0,
                            type = TransactionType.EXPENSE,
                            date = Date(),
                            note = "线程$threadId-操作$i",
                            categoryId = null,
                            accountId = account.id,
                            toAccountId = null,
                            isRecurring = false
                        )
                        transactionDao.insert(transaction)
                        
                        // 模拟一些处理时间
                        delay(10)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // 等待所有线程完成
        jobs.awaitAll()
        latch.await(30, TimeUnit.SECONDS)
        
        // 验证所有交易都被正确插入
        val allTransactions = transactionDao.getAllTransactionsSync()
        assertEquals("应该插入正确数量的交易", numThreads * operationsPerThread, allTransactions.size)
        
        // 执行检查点操作
        db.query("PRAGMA wal_checkpoint(FULL)", null)
        
        // 关闭数据库
        db.close()
    }

    /**
     * 测试异常情况下的数据库连接管理
     */
    @Test
    fun testDatabaseConnectionManagementOnException() = runBlocking {
        val transactionDao = db.transactionDao()
        
        try {
            // 尝试执行一个会导致异常的操作
            // 例如，插入一个ID冲突的记录
            val transaction1 = Transaction(
                id = 1,
                amount = 100.0,
                type = TransactionType.EXPENSE,
                date = Date(),
                note = "交易1",
                categoryId = null,
                accountId = 1,
                toAccountId = null,
                isRecurring = false
            )
            transactionDao.insert(transaction1)
            
            val transaction2 = Transaction(
                id = 1, // 相同的ID，会导致冲突
                amount = 200.0,
                type = TransactionType.EXPENSE,
                date = Date(),
                note = "交易2",
                categoryId = null,
                accountId = 1,
                toAccountId = null,
                isRecurring = false
            )
            transactionDao.insert(transaction2)
        } catch (e: Exception) {
            // 异常被捕获，数据库连接应该仍然有效
            assertTrue("数据库应该仍然处于打开状态", db.isOpen)
            
            // 验证可以继续执行其他操作
            val transaction3 = Transaction(
                id = 2,
                amount = 300.0,
                type = TransactionType.EXPENSE,
                date = Date(),
                note = "交易3",
                categoryId = null,
                accountId = 1,
                toAccountId = null,
                isRecurring = false
            )
            transactionDao.insert(transaction3)
            
            // 验证交易3被正确插入
            val retrievedTransaction = transactionDao.getTransactionByIdSync(2)
            assertEquals("交易3", retrievedTransaction?.note)
        }
        
        // 关闭数据库
        db.close()
        assertFalse("数据库应该已关闭", db.isOpen)
    }
}
