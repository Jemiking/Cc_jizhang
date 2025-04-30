package com.ccjizhang.ui.navigation

import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import java.util.concurrent.ConcurrentHashMap

/**
 * 导航分析类，用于跟踪用户的导航行为
 */
object NavAnalytics {
    private const val TAG = "NavAnalytics"
    private val screenTimeMap = ConcurrentHashMap<String, Long>()
    private var currentScreen: String? = null
    private var currentScreenStartTime: Long = 0

    /**
     * 初始化导航分析
     * 
     * @param navController 导航控制器
     */
    fun init(navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            trackScreenChange(destination)
        }
    }

    /**
     * 跟踪屏幕变化
     * 
     * @param destination 导航目的地
     */
    private fun trackScreenChange(destination: NavDestination) {
        val destinationRoute = destination.route ?: return
        val currentTime = System.currentTimeMillis()

        // 记录上一个屏幕的停留时间
        currentScreen?.let { screen ->
            val timeSpent = currentTime - currentScreenStartTime
            val totalTime = screenTimeMap.getOrDefault(screen, 0L) + timeSpent
            screenTimeMap[screen] = totalTime
            
            // 记录导航事件
            logNavigationEvent(screen, destinationRoute, timeSpent)
        }

        // 更新当前屏幕
        currentScreen = destinationRoute
        currentScreenStartTime = currentTime
    }

    /**
     * 记录导航事件
     * 
     * @param fromScreen 来源屏幕
     * @param toScreen 目标屏幕
     * @param timeSpent 停留时间
     */
    private fun logNavigationEvent(fromScreen: String, toScreen: String, timeSpent: Long) {
        Log.d(TAG, "Navigation: $fromScreen -> $toScreen, Time spent: ${timeSpent}ms")
        
        // 这里可以添加更多的分析逻辑，例如发送到分析服务
        // analyticsService.trackNavigation(fromScreen, toScreen, timeSpent)
    }

    /**
     * 获取屏幕停留时间报告
     * 
     * @return 屏幕停留时间报告
     */
    fun getScreenTimeReport(): Map<String, Long> {
        // 确保包含当前屏幕的停留时间
        currentScreen?.let { screen ->
            val timeSpent = System.currentTimeMillis() - currentScreenStartTime
            val totalTime = screenTimeMap.getOrDefault(screen, 0L) + timeSpent
            screenTimeMap[screen] = totalTime
        }
        
        return screenTimeMap.toMap()
    }

    /**
     * 重置分析数据
     */
    fun reset() {
        screenTimeMap.clear()
        currentScreen = null
        currentScreenStartTime = 0
    }
}
