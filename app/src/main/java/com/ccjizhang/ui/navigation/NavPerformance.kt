package com.ccjizhang.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 导航性能优化类，用于优化导航性能
 */
object NavPerformance {
    private const val TAG = "NavPerformance"
    private val navigationTimes = mutableMapOf<String, Long>()
    private val _navigationPerformance = MutableStateFlow<Map<String, Long>>(emptyMap())
    val navigationPerformance: StateFlow<Map<String, Long>> = _navigationPerformance

    /**
     * 监控导航性能
     * 
     * @param navController 导航控制器
     */
    @Composable
    fun MonitorNavigationPerformance(navController: NavHostController) {
        val listener = remember {
            NavController.OnDestinationChangedListener { _, destination, _ ->
                trackNavigationPerformance(destination)
            }
        }

        DisposableEffect(navController) {
            navController.addOnDestinationChangedListener(listener)
            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }
    }

    /**
     * 跟踪导航性能
     * 
     * @param destination 导航目的地
     */
    private fun trackNavigationPerformance(destination: NavDestination) {
        val destinationRoute = destination.route ?: return
        val endTime = System.currentTimeMillis()
        val startTime = navigationTimes[destinationRoute] ?: endTime
        val navigationTime = endTime - startTime
        
        Log.d(TAG, "Navigation to $destinationRoute took ${navigationTime}ms")
        
        // 更新性能数据
        val updatedPerformance = _navigationPerformance.value.toMutableMap()
        updatedPerformance[destinationRoute] = navigationTime
        _navigationPerformance.value = updatedPerformance
    }

    /**
     * 开始跟踪导航性能
     * 
     * @param route 导航路由
     */
    fun startTracking(route: String) {
        navigationTimes[route] = System.currentTimeMillis()
    }

    /**
     * 优化导航选项
     * 
     * 根据导航目的地自动设置最佳的导航选项
     * 
     * @param route 导航路由
     * @param builder 导航选项构建器
     */
    fun optimizeNavOptions(route: String, builder: NavOptionsBuilder) {
        when {
            // 主要导航目的地
            route == NavRoutes.Home || 
            route == NavRoutes.Transactions || 
            route == NavRoutes.Accounts || 
            route == NavRoutes.Statistics || 
            route == NavRoutes.Settings -> {
                builder.apply {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(NavRoutes.Home) {
                        saveState = true
                    }
                }
            }
            
            // 详情页面
            route.contains("detail") -> {
                builder.apply {
                    launchSingleTop = true
                }
            }
            
            // 编辑页面
            route.contains("edit") -> {
                builder.apply {
                    launchSingleTop = true
                }
            }
            
            // 添加页面
            route.contains("add") -> {
                builder.apply {
                    launchSingleTop = true
                }
            }
            
            // 默认选项
            else -> {
                builder.apply {
                    launchSingleTop = true
                }
            }
        }
    }
}

/**
 * 导航选项构建器
 */
class NavOptionsBuilder {
    var launchSingleTop: Boolean = false
    var restoreState: Boolean = false
    var popUpTo: String? = null
    var popUpToInclusive: Boolean = false
    var saveState: Boolean = false
    
    fun popUpTo(route: String, block: PopUpToBuilder.() -> Unit) {
        popUpTo = route
        val builder = PopUpToBuilder().apply(block)
        popUpToInclusive = builder.inclusive
        saveState = builder.saveState
    }
}

/**
 * PopUpTo构建器
 */
class PopUpToBuilder {
    var inclusive: Boolean = false
    var saveState: Boolean = false
}
