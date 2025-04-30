package com.ccjizhang.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// 创建DataStore实例
private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/**
 * 用户偏好设置实现类
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    // 偏好设置的键
    private object PreferencesKeys {
        // 引导和主题相关
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USER_THEME_COLOR = stringPreferencesKey("user_theme_color")

        // 设置项自定义分组相关
        val CUSTOM_SETTINGS_GROUPS = stringPreferencesKey("custom_settings_groups")
        val FAVORITE_SETTING_ITEMS = stringPreferencesKey("favorite_setting_items")
        val RECENT_SETTING_ITEMS = stringPreferencesKey("recent_setting_items")
        val QUICK_ACTION_ITEMS = stringPreferencesKey("quick_action_items")

        // 通知相关
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val BUDGET_NOTIFICATIONS_ENABLED = booleanPreferencesKey("budget_notifications_enabled")
        val BUDGET_WARNING_THRESHOLD = floatPreferencesKey("budget_warning_threshold")
        val BUDGET_EXCEEDED_NOTIFICATIONS_ENABLED = booleanPreferencesKey("budget_exceeded_notifications_enabled")
        val CREDIT_CARD_NOTIFICATIONS_ENABLED = booleanPreferencesKey("credit_card_notifications_enabled")
        val CREDIT_CARD_REMINDER_DAYS = intPreferencesKey("credit_card_reminder_days")
        val RECURRING_TRANSACTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("recurring_transaction_notifications_enabled")
        val RECURRING_TRANSACTION_REMINDER_DAYS = intPreferencesKey("recurring_transaction_reminder_days")
        val SYSTEM_NOTIFICATIONS_ENABLED = booleanPreferencesKey("system_notifications_enabled")
        val BACKUP_NOTIFICATIONS_ENABLED = booleanPreferencesKey("backup_notifications_enabled")
        val SYNC_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sync_notifications_enabled")
    }

    /**
     * 获取用户是否已完成引导页面
     */
    override suspend fun getHasCompletedOnboarding(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    /**
     * 设置用户已完成引导页面
     */
    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    /**
     * 获取是否启用深色模式
     */
    override suspend fun getDarkModeEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.DARK_MODE_ENABLED] ?: false
    }

    /**
     * 设置是否启用深色模式
     */
    override suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }

    /**
     * 获取主题模式
     */
    override suspend fun getThemeMode(): String {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.THEME_MODE] ?: "system"
    }

    /**
     * 设置主题模式
     */
    override suspend fun setThemeMode(mode: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    /**
     * 获取用户选择的主题颜色
     */
    override suspend fun getUserThemeColor(): String {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.USER_THEME_COLOR] ?: "#1976D2" // 默认蓝色
    }

    /**
     * 设置用户选择的主题颜色
     */
    override suspend fun setUserThemeColor(colorHex: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_THEME_COLOR] = colorHex
        }
    }

    // 通知相关方法实现

    /**
     * 获取是否启用通知
     */
    override suspend fun getNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用通知
     */
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取是否启用预算通知
     */
    override suspend fun getBudgetNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.BUDGET_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用预算通知
     */
    override suspend fun setBudgetNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取预算警告阈值
     */
    override suspend fun getBudgetWarningThreshold(): Float {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.BUDGET_WARNING_THRESHOLD] ?: 80f
    }

    /**
     * 设置预算警告阈值
     */
    override suspend fun setBudgetWarningThreshold(threshold: Float) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_WARNING_THRESHOLD] = threshold
        }
    }

    /**
     * 获取是否启用预算超支通知
     */
    override suspend fun getBudgetExceededNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.BUDGET_EXCEEDED_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用预算超支通知
     */
    override suspend fun setBudgetExceededNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_EXCEEDED_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取是否启用信用卡通知
     */
    override suspend fun getCreditCardNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.CREDIT_CARD_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用信用卡通知
     */
    override suspend fun setCreditCardNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CREDIT_CARD_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取信用卡提醒天数
     */
    override suspend fun getCreditCardReminderDays(): Int {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.CREDIT_CARD_REMINDER_DAYS] ?: 5
    }

    /**
     * 设置信用卡提醒天数
     */
    override suspend fun setCreditCardReminderDays(days: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CREDIT_CARD_REMINDER_DAYS] = days
        }
    }

    /**
     * 获取是否启用定期交易通知
     */
    override suspend fun getRecurringTransactionNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.RECURRING_TRANSACTION_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用定期交易通知
     */
    override suspend fun setRecurringTransactionNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECURRING_TRANSACTION_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取定期交易提醒天数
     */
    override suspend fun getRecurringTransactionReminderDays(): Int {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.RECURRING_TRANSACTION_REMINDER_DAYS] ?: 1
    }

    /**
     * 设置定期交易提醒天数
     */
    override suspend fun setRecurringTransactionReminderDays(days: Int) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECURRING_TRANSACTION_REMINDER_DAYS] = days
        }
    }

    /**
     * 获取是否启用系统通知
     */
    override suspend fun getSystemNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.SYSTEM_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用系统通知
     */
    override suspend fun setSystemNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取是否启用备份通知
     */
    override suspend fun getBackupNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.BACKUP_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用备份通知
     */
    override suspend fun setBackupNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取是否启用同步通知
     */
    override suspend fun getSyncNotificationsEnabled(): Boolean {
        return context.userPreferencesDataStore.data.first()[PreferencesKeys.SYNC_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 设置是否启用同步通知
     */
    override suspend fun setSyncNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * 获取设置项自定义分组
     */
    override suspend fun getCustomSettingsGroups(): Map<String, List<String>> {
        val groupsJson = context.userPreferencesDataStore.data.first()[PreferencesKeys.CUSTOM_SETTINGS_GROUPS] ?: "{}"
        return try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            Gson().fromJson(groupsJson, type)
        } catch (e: Exception) {
            Timber.e(e, "解析自定义设置分组失败")
            emptyMap()
        }
    }

    /**
     * 保存设置项自定义分组
     */
    override suspend fun saveCustomSettingsGroups(groups: Map<String, List<String>>) {
        try {
            val groupsJson = Gson().toJson(groups)
            context.userPreferencesDataStore.edit { preferences ->
                preferences[PreferencesKeys.CUSTOM_SETTINGS_GROUPS] = groupsJson
            }
        } catch (e: Exception) {
            Timber.e(e, "保存自定义设置分组失败")
        }
    }

    /**
     * 获取收藏的设置项ID列表
     */
    override suspend fun getFavoriteSettingItems(): List<String> {
        val favoritesJson = context.userPreferencesDataStore.data.first()[PreferencesKeys.FAVORITE_SETTING_ITEMS] ?: "[]"
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(favoritesJson, type)
        } catch (e: Exception) {
            Timber.e(e, "解析收藏设置项失败")
            emptyList()
        }
    }

    /**
     * 保存收藏的设置项ID列表
     */
    override suspend fun saveFavoriteSettingItems(itemIds: List<String>) {
        try {
            val favoritesJson = Gson().toJson(itemIds)
            context.userPreferencesDataStore.edit { preferences ->
                preferences[PreferencesKeys.FAVORITE_SETTING_ITEMS] = favoritesJson
            }
        } catch (e: Exception) {
            Timber.e(e, "保存收藏设置项失败")
        }
    }

    /**
     * 获取最近使用的设置项ID列表
     */
    override suspend fun getRecentSettingItems(): List<String> {
        val recentJson = context.userPreferencesDataStore.data.first()[PreferencesKeys.RECENT_SETTING_ITEMS] ?: "[]"
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(recentJson, type)
        } catch (e: Exception) {
            Timber.e(e, "解析最近使用设置项失败")
            emptyList()
        }
    }

    /**
     * 保存最近使用的设置项ID列表
     */
    override suspend fun saveRecentSettingItems(itemIds: List<String>) {
        try {
            val recentJson = Gson().toJson(itemIds)
            context.userPreferencesDataStore.edit { preferences ->
                preferences[PreferencesKeys.RECENT_SETTING_ITEMS] = recentJson
            }
        } catch (e: Exception) {
            Timber.e(e, "保存最近使用设置项失败")
        }
    }

    /**
     * 获取快速操作设置项ID列表
     */
    override suspend fun getQuickActionItems(): List<String> {
        val quickActionsJson = context.userPreferencesDataStore.data.first()[PreferencesKeys.QUICK_ACTION_ITEMS] ?: "[]"
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson(quickActionsJson, type)
        } catch (e: Exception) {
            Timber.e(e, "解析快速操作设置项失败")
            emptyList()
        }
    }

    /**
     * 保存快速操作设置项ID列表
     */
    override suspend fun saveQuickActionItems(itemIds: List<String>) {
        try {
            val quickActionsJson = Gson().toJson(itemIds)
            context.userPreferencesDataStore.edit { preferences ->
                preferences[PreferencesKeys.QUICK_ACTION_ITEMS] = quickActionsJson
            }
        } catch (e: Exception) {
            Timber.e(e, "保存快速操作设置项失败")
        }
    }
}