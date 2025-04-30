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
 * 通知设置状态数据类
 */
data class NotificationSettingsState(
    // 主开关
    val masterNotificationsEnabled: Boolean = true,
    
    // 预算通知
    val budgetNotificationsEnabled: Boolean = true,
    val budgetWarningThreshold: Float = 80f,
    val budgetExceededNotificationsEnabled: Boolean = true,
    
    // 信用卡通知
    val creditCardNotificationsEnabled: Boolean = true,
    val creditCardReminderDays: Int = 5,
    
    // 定期交易通知
    val recurringTransactionNotificationsEnabled: Boolean = true,
    val recurringTransactionReminderDays: Int = 1,
    
    // 系统通知
    val systemNotificationsEnabled: Boolean = true,
    val backupNotificationsEnabled: Boolean = true,
    val syncNotificationsEnabled: Boolean = true
)

/**
 * 通知设置的ViewModel
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    // 通知设置状态
    private val _notificationState = MutableStateFlow(NotificationSettingsState())
    val notificationState: StateFlow<NotificationSettingsState> = _notificationState.asStateFlow()
    
    init {
        loadNotificationSettings()
    }
    
    /**
     * 加载通知设置
     */
    private fun loadNotificationSettings() {
        viewModelScope.launch {
            // 从用户偏好设置中加载通知配置
            val masterEnabled = userPreferencesRepository.getNotificationsEnabled()
            val budgetEnabled = userPreferencesRepository.getBudgetNotificationsEnabled()
            val budgetThreshold = userPreferencesRepository.getBudgetWarningThreshold()
            val budgetExceededEnabled = userPreferencesRepository.getBudgetExceededNotificationsEnabled()
            val creditCardEnabled = userPreferencesRepository.getCreditCardNotificationsEnabled()
            val creditCardDays = userPreferencesRepository.getCreditCardReminderDays()
            val recurringEnabled = userPreferencesRepository.getRecurringTransactionNotificationsEnabled()
            val recurringDays = userPreferencesRepository.getRecurringTransactionReminderDays()
            val systemEnabled = userPreferencesRepository.getSystemNotificationsEnabled()
            val backupEnabled = userPreferencesRepository.getBackupNotificationsEnabled()
            val syncEnabled = userPreferencesRepository.getSyncNotificationsEnabled()
            
            // 更新状态
            _notificationState.value = NotificationSettingsState(
                masterNotificationsEnabled = masterEnabled,
                budgetNotificationsEnabled = budgetEnabled,
                budgetWarningThreshold = budgetThreshold,
                budgetExceededNotificationsEnabled = budgetExceededEnabled,
                creditCardNotificationsEnabled = creditCardEnabled,
                creditCardReminderDays = creditCardDays,
                recurringTransactionNotificationsEnabled = recurringEnabled,
                recurringTransactionReminderDays = recurringDays,
                systemNotificationsEnabled = systemEnabled,
                backupNotificationsEnabled = backupEnabled,
                syncNotificationsEnabled = syncEnabled
            )
        }
    }
    
    /**
     * 设置主通知开关
     */
    fun setMasterNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(masterNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置预算通知开关
     */
    fun setBudgetNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(budgetNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置预算警告阈值
     */
    fun setBudgetWarningThreshold(threshold: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetWarningThreshold(threshold)
            _notificationState.value = _notificationState.value.copy(budgetWarningThreshold = threshold)
        }
    }
    
    /**
     * 设置预算超支通知开关
     */
    fun setBudgetExceededNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBudgetExceededNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(budgetExceededNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置信用卡通知开关
     */
    fun setCreditCardNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCreditCardNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(creditCardNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置信用卡提醒天数
     */
    fun setCreditCardReminderDays(days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setCreditCardReminderDays(days)
            _notificationState.value = _notificationState.value.copy(creditCardReminderDays = days)
        }
    }
    
    /**
     * 设置定期交易通知开关
     */
    fun setRecurringTransactionNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRecurringTransactionNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(recurringTransactionNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置定期交易提醒天数
     */
    fun setRecurringTransactionReminderDays(days: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setRecurringTransactionReminderDays(days)
            _notificationState.value = _notificationState.value.copy(recurringTransactionReminderDays = days)
        }
    }
    
    /**
     * 设置系统通知开关
     */
    fun setSystemNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSystemNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(systemNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置备份通知开关
     */
    fun setBackupNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(backupNotificationsEnabled = enabled)
        }
    }
    
    /**
     * 设置同步通知开关
     */
    fun setSyncNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSyncNotificationsEnabled(enabled)
            _notificationState.value = _notificationState.value.copy(syncNotificationsEnabled = enabled)
        }
    }
} 