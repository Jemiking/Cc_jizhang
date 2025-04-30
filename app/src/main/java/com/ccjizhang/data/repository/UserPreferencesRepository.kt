package com.ccjizhang.data.repository

/**
 * 用户偏好设置接口
 */
interface UserPreferencesRepository {

    /**
     * 获取设置项自定义分组
     */
    suspend fun getCustomSettingsGroups(): Map<String, List<String>>

    /**
     * 保存设置项自定义分组
     */
    suspend fun saveCustomSettingsGroups(groups: Map<String, List<String>>)

    /**
     * 获取收藏的设置项ID列表
     */
    suspend fun getFavoriteSettingItems(): List<String>

    /**
     * 保存收藏的设置项ID列表
     */
    suspend fun saveFavoriteSettingItems(itemIds: List<String>)

    /**
     * 获取最近使用的设置项ID列表
     */
    suspend fun getRecentSettingItems(): List<String>

    /**
     * 保存最近使用的设置项ID列表
     */
    suspend fun saveRecentSettingItems(itemIds: List<String>)

    /**
     * 获取快速操作设置项ID列表
     */
    suspend fun getQuickActionItems(): List<String>

    /**
     * 保存快速操作设置项ID列表
     */
    suspend fun saveQuickActionItems(itemIds: List<String>)

    /**
     * 获取用户是否已完成引导页面
     */
    suspend fun getHasCompletedOnboarding(): Boolean

    /**
     * 设置用户已完成引导页面
     */
    suspend fun setHasCompletedOnboarding(completed: Boolean)

    /**
     * 获取是否启用深色模式
     */
    suspend fun getDarkModeEnabled(): Boolean

    /**
     * 设置是否启用深色模式
     */
    suspend fun setDarkModeEnabled(enabled: Boolean)

    /**
     * 获取主题模式 ("system", "dark", "light")
     */
    suspend fun getThemeMode(): String

    /**
     * 设置主题模式 ("system", "dark", "light")
     */
    suspend fun setThemeMode(mode: String)

    /**
     * 获取用户选择的主题颜色
     */
    suspend fun getUserThemeColor(): String

    /**
     * 设置用户选择的主题颜色
     */
    suspend fun setUserThemeColor(colorHex: String)

    /**
     * 获取是否启用通知
     */
    suspend fun getNotificationsEnabled(): Boolean

    /**
     * 设置是否启用通知
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /**
     * 获取是否启用预算通知
     */
    suspend fun getBudgetNotificationsEnabled(): Boolean

    /**
     * 设置是否启用预算通知
     */
    suspend fun setBudgetNotificationsEnabled(enabled: Boolean)

    /**
     * 获取预算警告阈值
     */
    suspend fun getBudgetWarningThreshold(): Float

    /**
     * 设置预算警告阈值
     */
    suspend fun setBudgetWarningThreshold(threshold: Float)

    /**
     * 获取是否启用预算超支通知
     */
    suspend fun getBudgetExceededNotificationsEnabled(): Boolean

    /**
     * 设置是否启用预算超支通知
     */
    suspend fun setBudgetExceededNotificationsEnabled(enabled: Boolean)

    /**
     * 获取是否启用信用卡通知
     */
    suspend fun getCreditCardNotificationsEnabled(): Boolean

    /**
     * 设置是否启用信用卡通知
     */
    suspend fun setCreditCardNotificationsEnabled(enabled: Boolean)

    /**
     * 获取信用卡提醒天数
     */
    suspend fun getCreditCardReminderDays(): Int

    /**
     * 设置信用卡提醒天数
     */
    suspend fun setCreditCardReminderDays(days: Int)

    /**
     * 获取是否启用定期交易通知
     */
    suspend fun getRecurringTransactionNotificationsEnabled(): Boolean

    /**
     * 设置是否启用定期交易通知
     */
    suspend fun setRecurringTransactionNotificationsEnabled(enabled: Boolean)

    /**
     * 获取定期交易提醒天数
     */
    suspend fun getRecurringTransactionReminderDays(): Int

    /**
     * 设置定期交易提醒天数
     */
    suspend fun setRecurringTransactionReminderDays(days: Int)

    /**
     * 获取是否启用系统通知
     */
    suspend fun getSystemNotificationsEnabled(): Boolean

    /**
     * 设置是否启用系统通知
     */
    suspend fun setSystemNotificationsEnabled(enabled: Boolean)

    /**
     * 获取是否启用备份通知
     */
    suspend fun getBackupNotificationsEnabled(): Boolean

    /**
     * 设置是否启用备份通知
     */
    suspend fun setBackupNotificationsEnabled(enabled: Boolean)

    /**
     * 获取是否启用同步通知
     */
    suspend fun getSyncNotificationsEnabled(): Boolean

    /**
     * 设置是否启用同步通知
     */
    suspend fun setSyncNotificationsEnabled(enabled: Boolean)
}