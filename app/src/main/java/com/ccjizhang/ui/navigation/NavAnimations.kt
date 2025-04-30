package com.ccjizhang.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/**
 * 导航动画类，用于定义不同类型的导航动画
 */
object NavAnimations {
    private const val ANIMATION_DURATION = 300

    /**
     * 水平滑动动画
     */
    val horizontalSlideEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val horizontalSlideExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    val horizontalSlidePopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val horizontalSlidePopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    /**
     * 垂直滑动动画
     */
    val verticalSlideEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val verticalSlideExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    val verticalSlidePopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val verticalSlidePopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    /**
     * 淡入淡出动画
     */
    val fadeEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val fadeExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    /**
     * 缩放动画
     */
    val scaleEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseIn
            )
        )
    }

    val scaleExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseInOut
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = EaseOut
            )
        )
    }

    /**
     * 根据路由类型获取合适的动画
     */
    fun getEnterTransitionByRoute(route: String?): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return when {
            // 主要导航页面使用水平滑动
            route == NavRoutes.Home || 
            route == NavRoutes.Transactions || 
            route == NavRoutes.Statistics || 
            route == NavRoutes.Accounts || 
            route == NavRoutes.Settings -> horizontalSlideEnterTransition
            
            // 详情页面使用垂直滑动
            route?.contains("detail") == true -> verticalSlideEnterTransition
            
            // 添加/编辑页面使用缩放
            route?.contains("add") == true || route?.contains("edit") == true -> scaleEnterTransition
            
            // 其他页面使用淡入淡出
            else -> fadeEnterTransition
        }
    }

    fun getExitTransitionByRoute(route: String?): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return when {
            // 主要导航页面使用水平滑动
            route == NavRoutes.Home || 
            route == NavRoutes.Transactions || 
            route == NavRoutes.Statistics || 
            route == NavRoutes.Accounts || 
            route == NavRoutes.Settings -> horizontalSlideExitTransition
            
            // 详情页面使用垂直滑动
            route?.contains("detail") == true -> verticalSlideExitTransition
            
            // 添加/编辑页面使用缩放
            route?.contains("add") == true || route?.contains("edit") == true -> scaleExitTransition
            
            // 其他页面使用淡入淡出
            else -> fadeExitTransition
        }
    }

    fun getPopEnterTransitionByRoute(route: String?): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        return when {
            // 主要导航页面使用水平滑动
            route == NavRoutes.Home || 
            route == NavRoutes.Transactions || 
            route == NavRoutes.Statistics || 
            route == NavRoutes.Accounts || 
            route == NavRoutes.Settings -> horizontalSlidePopEnterTransition
            
            // 详情页面使用垂直滑动
            route?.contains("detail") == true -> verticalSlidePopEnterTransition
            
            // 添加/编辑页面使用缩放
            route?.contains("add") == true || route?.contains("edit") == true -> scaleEnterTransition
            
            // 其他页面使用淡入淡出
            else -> fadeEnterTransition
        }
    }

    fun getPopExitTransitionByRoute(route: String?): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        return when {
            // 主要导航页面使用水平滑动
            route == NavRoutes.Home || 
            route == NavRoutes.Transactions || 
            route == NavRoutes.Statistics || 
            route == NavRoutes.Accounts || 
            route == NavRoutes.Settings -> horizontalSlidePopExitTransition
            
            // 详情页面使用垂直滑动
            route?.contains("detail") == true -> verticalSlidePopExitTransition
            
            // 添加/编辑页面使用缩放
            route?.contains("add") == true || route?.contains("edit") == true -> scaleExitTransition
            
            // 其他页面使用淡入淡出
            else -> fadeExitTransition
        }
    }
}
