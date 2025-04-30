package com.ccjizhang.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.TransactionType
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.TransactionRepository
import com.ccjizhang.utils.DataIntegrityChecker
import com.ccjizhang.utils.DatabaseExceptionHandler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.Date

/**
 * 数据库监控系统集成测试
 * 测试数据库监控系统的告警机制和数据完整性检查功能
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMonitoringTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var dataIntegrityChecker: DataIntegrityChecker
    private lateinit var databaseExceptionHandler: DatabaseExceptionHandler

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 创建内存数据库用于测试
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()

        // 初始化仓库
        // 创建DatabaseRepairTool
        val databaseRepairTool = DatabaseRepairTool(
            context,
            CategoryRepository(db.categoryDao()),
            AccountRepository(db.accountDao())
        )

        // 创建DatabaseExceptionHandler
        val databaseExceptionHandler = DatabaseExceptionHandler(databaseRepairTool)

        transactionRepository = TransactionRepository(
            db.transactionDao(),
            accountRepository = AccountRepository(db.accountDao()),
            categoryRepository = CategoryRepository(db.categoryDao()),
            databaseExceptionHandler = databaseExceptionHandler
        )

        accountRepository = AccountRepository(db.accountDao())
        categoryRepository = CategoryRepository(db.categoryDao())

        // 初始化数据完整性检查器
        dataIntegrityChecker = DataIntegrityChecker(
            context,
            transactionRepository,
            accountRepository,
            db
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * 测试数据库完整性检查功能
     */
    @Test
    fun testDatabaseIntegrityCheck() = runBlocking {
        // 验证新创建的数据库完整性正常
        val integrityResult = dataIntegrityChecker.checkDatabaseIntegrity()
        assertTrue("新创建的数据库应该通过完整性检查", integrityResult)
    }

    /**
     * 测试WAL模式检查功能
     */
    @Test
    fun testWalModeCheck() = runBlocking {
        // 通过反射访问私有方法进行测试
        val checkWalModeMethod = DataIntegrityChecker::class.java.getDeclaredMethod("checkWalMode")
        checkWalModeMethod.isAccessible = true

        val result = checkWalModeMethod.invoke(dataIntegrityChecker) as Boolean
        assertTrue("WAL模式应该已启用", result)
    }

    /**
     * 测试账户余额一致性检查和修复功能
     */
    @Test
    fun testAccountBalanceConsistencyCheck() = runBlocking {
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
        accountRepository.insertAccount(account)

        // 创建测试分类
        val category = Category(
            id = 1,
            name = "测试分类",
            type = TransactionType.EXPENSE,
            color = "#00FF00",
            icon = "food",
            sortOrder = 0,
            level = 1,
            parentId = null
        )
        categoryRepository.insertCategory(category)

        // 创建测试交易，但不通过正常渠道更新账户余额
        val transaction = Transaction(
            id = 1,
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "测试交易",
            categoryId = category.id,
            accountId = account.id,
            toAccountId = null,
            isRecurring = false
        )
        db.transactionDao().insert(transaction)

        // 验证账户余额与交易记录不一致
        val hasFixed = transactionRepository.verifyAndFixAccountBalances()
        assertTrue("应该检测到并修复账户余额不一致", hasFixed)

        // 验证修复后的账户余额
        val updatedAccount = accountRepository.getAccountById(account.id).first()
        assertEquals(500.0, updatedAccount.balance, 0.01)
    }

    /**
     * 测试孤立交易检查和修复功能
     */
    @Test
    fun testOrphanedTransactionsCheck() = runBlocking {
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
        accountRepository.insertAccount(account)

        // 创建测试交易，引用不存在的分类
        val transaction = Transaction(
            id = 1,
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "测试交易",
            categoryId = 999, // 不存在的分类ID
            accountId = account.id,
            toAccountId = null,
            isRecurring = false
        )
        db.transactionDao().insert(transaction)

        // 测试修复孤立交易功能
        val fixedCount = transactionRepository.fixTransactionsWithInvalidCategory()
        assertEquals("应该修复1条引用了无效分类的交易", 1, fixedCount)

        // 验证修复后的交易记录
        val updatedTransaction = db.transactionDao().getTransactionById(transaction.id).first()
        assertEquals("交易的分类ID应该被设置为null", null, updatedTransaction.categoryId)
    }

    /**
     * 测试数据库监控系统的全面检查功能
     */
    @Test
    fun testCompleteDataIntegrityCheck() = runBlocking {
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
        accountRepository.insertAccount(account)

        // 创建测试分类
        val category = Category(
            id = 1,
            name = "测试分类",
            type = TransactionType.EXPENSE,
            color = "#00FF00",
            icon = "food",
            sortOrder = 0,
            level = 1,
            parentId = null
        )
        categoryRepository.insertCategory(category)

        // 创建测试交易，但不通过正常渠道更新账户余额
        val transaction = Transaction(
            id = 1,
            amount = 500.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "测试交易",
            categoryId = category.id,
            accountId = account.id,
            toAccountId = null,
            isRecurring = false
        )
        db.transactionDao().insert(transaction)

        // 创建引用不存在分类的交易
        val orphanedTransaction = Transaction(
            id = 2,
            amount = 300.0,
            type = TransactionType.EXPENSE,
            date = Date(),
            note = "孤立交易",
            categoryId = 999, // 不存在的分类ID
            accountId = account.id,
            toAccountId = null,
            isRecurring = false
        )
        db.transactionDao().insert(orphanedTransaction)

        // 执行全面数据一致性检查
        val hasFixed = dataIntegrityChecker.checkAndFixAllIssues()
        assertTrue("应该检测到并修复数据一致性问题", hasFixed)

        // 验证修复后的账户余额
        val updatedAccount = accountRepository.getAccountById(account.id).first()
        assertEquals(200.0, updatedAccount.balance, 0.01) // 1000 - 500 - 300 = 200

        // 验证修复后的孤立交易
        val updatedOrphanedTransaction = db.transactionDao().getTransactionById(orphanedTransaction.id).first()
        assertEquals("孤立交易的分类ID应该被设置为null", null, updatedOrphanedTransaction.categoryId)
    }
}
