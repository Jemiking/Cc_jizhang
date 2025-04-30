package com.ccjizhang.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 导航性能单元测试
 */
class NavPerformanceTest {

    private lateinit var navController: NavHostController
    private lateinit var navDestination: NavDestination
    @Before
    fun setUp() {
        // 使用mockk模拟NavHostController和NavDestination
        navController = mockk(relaxed = true)
        navDestination = mockk(relaxed = true)

        // 设置NavDestination的route属性
        every { navDestination.route } returns "test_route"

        // 初始化NavPerformance
        NavPerformance.startTracking("test_route")
    }

    @Test
    fun testStartTracking() {
        // 测试开始跟踪
        NavPerformance.startTracking("test_route")

        // 验证性能数据不为空
        runBlocking {
            val performance = NavPerformance.navigationPerformance.first()
            assertNotNull(performance)
        }
    }

    @Test
    fun testNavigationPerformance() {
        // 测试导航性能

        // 开始跟踪
        NavPerformance.startTracking("test_route")

        // 等待一段时间，模拟导航过程
        Thread.sleep(100)

        // 验证性能数据不为空
        runBlocking {
            val performance = NavPerformance.navigationPerformance.first()
            assertNotNull(performance)
        }
    }

    @Test
    fun testOptimizeNavOptions() {
        // 测试优化导航选项

        // 创建NavOptionsBuilder
        val builder = NavOptionsBuilder()

        // 主要导航页面
        NavPerformance.optimizeNavOptions(NavRoutes.Home, builder)
        assertTrue(builder.launchSingleTop)
        assertTrue(builder.restoreState)
        assertEquals(NavRoutes.Home, builder.popUpTo)
        assertTrue(builder.saveState)

        // 详情页面
        val builder2 = NavOptionsBuilder()
        NavPerformance.optimizeNavOptions(NavRoutes.TransactionDetail, builder2)
        assertTrue(builder2.launchSingleTop)

        // 编辑页面
        val builder3 = NavOptionsBuilder()
        NavPerformance.optimizeNavOptions(NavRoutes.TransactionEdit, builder3)
        assertTrue(builder3.launchSingleTop)

        // 添加页面
        val builder4 = NavOptionsBuilder()
        NavPerformance.optimizeNavOptions(NavRoutes.TransactionAdd, builder4)
        assertTrue(builder4.launchSingleTop)

        // 默认选项
        val builder5 = NavOptionsBuilder()
        NavPerformance.optimizeNavOptions("unknown_route", builder5)
        assertTrue(builder5.launchSingleTop)
    }
}
