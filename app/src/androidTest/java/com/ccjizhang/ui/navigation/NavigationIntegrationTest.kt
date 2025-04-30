package com.ccjizhang.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 导航集成测试
 */
@RunWith(AndroidJUnit4::class)
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        // 创建TestNavHostController
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        navController = TestNavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        
        // 设置UnifiedNavGraph
        composeTestRule.setContent {
            UnifiedNavGraph(navController = navController)
        }
    }

    @Test
    fun testStartDestination() {
        // 测试起始目的地
        assertEquals(NavRoutes.Home, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testNavigateToSettings() {
        // 测试导航到设置页面
        
        // 导航到设置页面
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.Settings)
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.Settings, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testNavigateToTransactions() {
        // 测试导航到交易列表
        
        // 导航到交易列表
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.Transactions)
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.Transactions, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testNavigateToAccounts() {
        // 测试导航到账户列表
        
        // 导航到账户列表
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.Accounts)
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.Accounts, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testNavigateToTransactionDetail() {
        // 测试导航到交易详情
        
        // 导航到交易详情
        val transactionId = 123L
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.transactionDetail(transactionId))
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.TransactionDetail, navController.currentBackStackEntry?.destination?.route)
        
        // 验证参数
        assertEquals(transactionId, navController.currentBackStackEntry?.arguments?.getLong("transactionId"))
    }

    @Test
    fun testNavigateToAccountDetail() {
        // 测试导航到账户详情
        
        // 导航到账户详情
        val accountId = 123L
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.accountDetail(accountId))
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.AccountDetail, navController.currentBackStackEntry?.destination?.route)
        
        // 验证参数
        assertEquals(accountId, navController.currentBackStackEntry?.arguments?.getLong("accountId"))
    }

    @Test
    fun testNavigateBack() {
        // 测试返回导航
        
        // 导航到设置页面
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.Settings)
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.Settings, navController.currentBackStackEntry?.destination?.route)
        
        // 返回
        composeTestRule.runOnUiThread {
            navController.navigateUp()
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.Home, navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun testDeepLinking() {
        // 测试深层链接
        
        // 导航到交易详情
        val transactionId = 123L
        composeTestRule.runOnUiThread {
            navController.navigate(NavRoutes.transactionDetail(transactionId))
        }
        
        // 验证当前路由
        assertEquals(NavRoutes.TransactionDetail, navController.currentBackStackEntry?.destination?.route)
        
        // 验证参数
        assertEquals(transactionId, navController.currentBackStackEntry?.arguments?.getLong("transactionId"))
    }
}
