package com.ccjizhang.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 主题相关
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val PRIMARY_COLOR = longPreferencesKey("primary_color")
    
    // 首次使用引导相关
    private val HAS_SEEN_ONBOARDING = stringPreferencesKey("has_seen_onboarding")
    
    // 保存主题模式（浅色/深色/系统）
    suspend fun saveThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode
        }
    }
    
    // 获取主题模式
    suspend fun getThemeMode(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[THEME_MODE]
    }
    
    // 保存主题主色调
    suspend fun savePrimaryColor(color: Long) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_COLOR] = color
        }
    }
    
    // 获取主题主色调
    suspend fun getPrimaryColor(): Long? {
        val preferences = context.dataStore.data.first()
        return preferences[PRIMARY_COLOR]
    }
    
    // 标记已完成引导流程
    suspend fun markOnboardingComplete() {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING] = "true"
        }
    }
    
    // 检查用户是否已完成引导流程
    suspend fun hasCompletedOnboarding(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[HAS_SEEN_ONBOARDING] == "true"
    }
} 