package com.ccjizhang.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 导航路由单元测试
 */
class NavRoutesTest {

    @Test
    fun testHomeRoute() {
        // 测试主页路由
        assertEquals("home", NavRoutes.Home)
    }

    @Test
    fun testTransactionsRoute() {
        // 测试交易列表路由
        assertEquals("transactions", NavRoutes.Transactions)
    }

    @Test
    fun testAccountsRoute() {
        // 测试账户列表路由
        assertEquals("accounts", NavRoutes.Accounts)
    }

    @Test
    fun testSettingsRoute() {
        // 测试设置页面路由
        assertEquals("settings", NavRoutes.Settings)
    }

    @Test
    fun testNavAnalyticsReportRoute() {
        // 测试导航分析报告路由
        assertEquals("nav_analytics_report", NavRoutes.NavAnalyticsReport)
    }
}
