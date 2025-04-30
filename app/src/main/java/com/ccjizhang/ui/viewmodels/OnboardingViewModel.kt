package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 处理引导页面的ViewModel
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // 页面状态
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // 引导页面的总数
    val totalPages = 3

    /**
     * 检查用户是否已完成引导
     */
    suspend fun hasCompletedOnboarding(): Boolean {
        return userPreferencesRepository.getHasCompletedOnboarding()
    }

    /**
     * 设置用户已完成引导
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setHasCompletedOnboarding(true)
        }
    }

    /**
     * 导航到下一页
     */
    fun nextPage() {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value = _currentPage.value + 1
        }
    }

    /**
     * 导航到上一页
     */
    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value = _currentPage.value - 1
        }
    }

    /**
     * 导航到指定页面
     */
    fun goToPage(page: Int) {
        if (page in 0 until totalPages) {
            _currentPage.value = page
        }
    }
} 