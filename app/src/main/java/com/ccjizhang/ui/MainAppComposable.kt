package com.ccjizhang.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.components.BottomNavBar
import com.ccjizhang.ui.navigation.BottomNavItem
import com.ccjizhang.ui.navigation.NavAnalytics
import com.ccjizhang.ui.navigation.NavPerformance
import com.ccjizhang.ui.navigation.NavPerformanceMonitor
import com.ccjizhang.ui.navigation.NavPerformanceMonitorComponent
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.UnifiedNavGraph
import com.ccjizhang.ui.theme.CCJiZhangTheme
import com.ccjizhang.ui.viewmodels.OnboardingViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


/**
 * 应用程序主UI组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppComposable() {
    // 初始化导航控制器
    val navController = rememberNavController()

    // 初始化导航分析和性能监控
    LaunchedEffect(navController) {
        NavAnalytics.init(navController)
        NavPerformanceMonitor.init(navController)
    }

    // 监控导航性能
    NavPerformance.MonitorNavigationPerformance(navController)

    // 添加导航性能监控组件
    NavPerformanceMonitorComponent(navController)

    // 获取当前导航堆栈
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 引导页面相关逻辑
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    // 确保用户已完成引导
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // 将用户标记为已完成引导
            onboardingViewModel.completeOnboarding()
            isLoading = false
        }
    }

    // 底部导航项列表 - 进一步精简
    val bottomNavItems = listOf(
        BottomNavItem(
            title = "首页",
            route = NavRoutes.Home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            title = "交易",
            route = NavRoutes.Transactions,
            selectedIcon = Icons.Filled.SwapHoriz,
            unselectedIcon = Icons.Outlined.SwapHoriz
        )
    )

    // 判断是否显示底部导航栏和悬浮按钮
    val isBottomBarVisible = shouldShowBottomBar(currentRoute)

    // 应用主题
    CCJiZhangTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isLoading) {
                Scaffold(
                    bottomBar = {
                        if (isBottomBarVisible) {
                            BottomNavBar(
                                navController = navController,
                                currentDestination = navBackStackEntry?.destination,
                                items = bottomNavItems
                            )
                        }
                    },
                    floatingActionButton = {
                        CCJiZhangFloatingActionButton(navController, isBottomBarVisible, currentRoute)
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    UnifiedNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Composable
private fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return when (currentRoute) {
        NavRoutes.Home, NavRoutes.Transactions -> true
        else -> false
    }
}

@Composable
private fun CCJiZhangFloatingActionButton(
    navController: NavHostController,
    isBottomBarVisible: Boolean,
    currentRoute: String?
) {
    if (isBottomBarVisible) {
        FloatingActionButton(
            onClick = { navController.navigate(NavRoutes.TransactionAdd) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier
                .size(52.dp)
                .offset(y = (-4).dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加交易",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// 添加预览
@Preview(
    name = "应用主界面预览",
    showSystemUi = true
)
@Composable
fun PreviewMainAppComposable() {
    CCJiZhangTheme {
        MainAppComposable()
    }
}
