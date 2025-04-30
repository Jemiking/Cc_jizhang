package com.ccjizhang.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 导航动画单元测试
 */
class NavAnimationsTest {

    @Test
    fun testHorizontalSlideEnterTransition() {
        // 测试水平滑动进入动画
        val enterTransition = NavAnimations.horizontalSlideEnterTransition
        assertNotNull(enterTransition)
    }

    @Test
    fun testHorizontalSlideExitTransition() {
        // 测试水平滑动退出动画
        val exitTransition = NavAnimations.horizontalSlideExitTransition
        assertNotNull(exitTransition)
    }

    @Test
    fun testFadeEnterTransition() {
        // 测试淡入动画
        val enterTransition = NavAnimations.fadeEnterTransition
        assertNotNull(enterTransition)
    }

    @Test
    fun testFadeExitTransition() {
        // 测试淡出动画
        val exitTransition = NavAnimations.fadeExitTransition
        assertNotNull(exitTransition)
    }
}
