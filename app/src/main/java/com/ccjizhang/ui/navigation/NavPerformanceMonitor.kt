package com.ccjizhang.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * 导航性能监控工具
 * 
 * 用于监控导航性能指标，包括导航时间、页面加载时间等
 */
object NavPerformanceMonitor {
    private const val TAG = "NavPerformanceMonitor"
    
    // 性能阈值（毫秒）
    private const val NAVIGATION_TIME_THRESHOLD = 300L
    private const val PAGE_LOAD_TIME_THRESHOLD = 500L
    
    // 性能数据
    private val _performanceData = MutableStateFlow<Map<String, PerformanceMetrics>>(emptyMap())
    val performanceData: StateFlow<Map<String, PerformanceMetrics>> = _performanceData.asStateFlow()
    
    // 性能警告
    private val _performanceWarnings = MutableStateFlow<List<PerformanceWarning>>(emptyList())
    val performanceWarnings: StateFlow<List<PerformanceWarning>> = _performanceWarnings.asStateFlow()
    
    // 当前路由
    private var currentRoute: String? = null
    
    // 导航开始时间
    private var navigationStartTime: Long = 0
    
    // 页面加载开始时间
    private val pageLoadStartTimes = ConcurrentHashMap<String, Long>()
    
    /**
     * 初始化导航性能监控
     */
    fun init(navController: NavHostController) {
        // 监听导航事件
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val route = destination.route ?: return@addOnDestinationChangedListener
            
            // 记录导航结束时间
            val navigationEndTime = System.currentTimeMillis()
            
            // 如果有上一个路由，计算导航时间
            currentRoute?.let { prevRoute ->
                val navigationTime = navigationEndTime - navigationStartTime
                
                // 更新性能数据
                updatePerformanceData(prevRoute, navigationTime)
                
                // 检查性能是否超过阈值
                checkPerformanceThresholds(prevRoute, navigationTime)
            }
            
            // 更新当前路由和导航开始时间
            currentRoute = route
            navigationStartTime = navigationEndTime
            
            // 记录页面加载开始时间
            pageLoadStartTimes[route] = navigationEndTime
            
            Log.d(TAG, "Navigation to $route started")
        }
    }
    
    /**
     * 记录页面加载完成
     */
    fun recordPageLoadComplete(route: String) {
        val startTime = pageLoadStartTimes[route] ?: return
        val loadTime = System.currentTimeMillis() - startTime
        
        // 更新性能数据
        _performanceData.update { currentData ->
            val metrics = currentData[route] ?: PerformanceMetrics(route)
            val updatedMetrics = metrics.copy(
                pageLoadTime = loadTime,
                pageLoadCount = metrics.pageLoadCount + 1,
                totalPageLoadTime = metrics.totalPageLoadTime + loadTime
            )
            
            currentData + (route to updatedMetrics)
        }
        
        // 检查页面加载时间是否超过阈值
        if (loadTime > PAGE_LOAD_TIME_THRESHOLD) {
            _performanceWarnings.update { warnings ->
                warnings + PerformanceWarning(
                    route = route,
                    warningType = PerformanceWarningType.PAGE_LOAD_TIME,
                    value = loadTime,
                    threshold = PAGE_LOAD_TIME_THRESHOLD,
                    timestamp = System.currentTimeMillis()
                )
            }
            
            Log.w(TAG, "Page load time for $route is too long: $loadTime ms")
        }
        
        Log.d(TAG, "Page load for $route completed in $loadTime ms")
    }
    
    /**
     * 更新性能数据
     */
    private fun updatePerformanceData(route: String, navigationTime: Long) {
        _performanceData.update { currentData ->
            val metrics = currentData[route] ?: PerformanceMetrics(route)
            val updatedMetrics = metrics.copy(
                navigationTime = navigationTime,
                navigationCount = metrics.navigationCount + 1,
                totalNavigationTime = metrics.totalNavigationTime + navigationTime
            )
            
            currentData + (route to updatedMetrics)
        }
    }
    
    /**
     * 检查性能是否超过阈值
     */
    private fun checkPerformanceThresholds(route: String, navigationTime: Long) {
        if (navigationTime > NAVIGATION_TIME_THRESHOLD) {
            _performanceWarnings.update { warnings ->
                warnings + PerformanceWarning(
                    route = route,
                    warningType = PerformanceWarningType.NAVIGATION_TIME,
                    value = navigationTime,
                    threshold = NAVIGATION_TIME_THRESHOLD,
                    timestamp = System.currentTimeMillis()
                )
            }
            
            Log.w(TAG, "Navigation time for $route is too long: $navigationTime ms")
        }
    }
    
    /**
     * 获取性能报告
     */
    fun getPerformanceReport(): PerformanceReport {
        val data = performanceData.value
        
        // 计算平均导航时间
        val avgNavigationTime = data.values
            .filter { it.navigationCount > 0 }
            .map { it.totalNavigationTime.toDouble() / it.navigationCount }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        
        // 计算平均页面加载时间
        val avgPageLoadTime = data.values
            .filter { it.pageLoadCount > 0 }
            .map { it.totalPageLoadTime.toDouble() / it.pageLoadCount }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0
        
        // 找出导航时间最长的路由
        val slowestNavigation = data.values
            .filter { it.navigationCount > 0 }
            .maxByOrNull { it.navigationTime }
        
        // 找出页面加载时间最长的路由
        val slowestPageLoad = data.values
            .filter { it.pageLoadCount > 0 }
            .maxByOrNull { it.pageLoadTime }
        
        // 计算性能警告数量
        val warningCount = performanceWarnings.value.size
        
        return PerformanceReport(
            avgNavigationTime = avgNavigationTime,
            avgPageLoadTime = avgPageLoadTime,
            slowestNavigation = slowestNavigation,
            slowestPageLoad = slowestPageLoad,
            warningCount = warningCount,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 重置性能数据
     */
    fun reset() {
        _performanceData.value = emptyMap()
        _performanceWarnings.value = emptyList()
        currentRoute = null
        navigationStartTime = 0
        pageLoadStartTimes.clear()
    }
}

/**
 * 性能指标
 */
data class PerformanceMetrics(
    val route: String,
    val navigationTime: Long = 0,
    val pageLoadTime: Long = 0,
    val navigationCount: Int = 0,
    val pageLoadCount: Int = 0,
    val totalNavigationTime: Long = 0,
    val totalPageLoadTime: Long = 0
)

/**
 * 性能警告类型
 */
enum class PerformanceWarningType {
    NAVIGATION_TIME,
    PAGE_LOAD_TIME
}

/**
 * 性能警告
 */
data class PerformanceWarning(
    val route: String,
    val warningType: PerformanceWarningType,
    val value: Long,
    val threshold: Long,
    val timestamp: Long
)

/**
 * 性能报告
 */
data class PerformanceReport(
    val avgNavigationTime: Double,
    val avgPageLoadTime: Double,
    val slowestNavigation: PerformanceMetrics?,
    val slowestPageLoad: PerformanceMetrics?,
    val warningCount: Int,
    val timestamp: Long
)

/**
 * 导航性能监控组件
 */
@Composable
fun NavPerformanceMonitorComponent(navController: NavHostController) {
    // 初始化导航性能监控
    LaunchedEffect(navController) {
        NavPerformanceMonitor.init(navController)
    }
    
    // 获取当前路由
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    
    // 记录页面加载完成
    LaunchedEffect(currentRoute) {
        currentRoute?.let {
            NavPerformanceMonitor.recordPageLoadComplete(it)
        }
    }
    
    // 获取性能数据
    val performanceData by NavPerformanceMonitor.performanceData.collectAsState()
    
    // 获取性能警告
    val performanceWarnings by NavPerformanceMonitor.performanceWarnings.collectAsState()
    
    // 记录性能报告
    val performanceReport = remember(performanceData, performanceWarnings) {
        NavPerformanceMonitor.getPerformanceReport()
    }
    
    // 记录性能报告
    LaunchedEffect(performanceReport) {
        Log.d("NavPerformanceMonitor", "Performance Report: $performanceReport")
    }
}
