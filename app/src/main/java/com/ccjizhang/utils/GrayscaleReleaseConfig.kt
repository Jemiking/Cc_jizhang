package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * 灰度发布配置管理器
 * 用于控制数据库优化功能的灰度发布
 */
@Singleton
class GrayscaleReleaseConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "grayscale_release_prefs"
        private const val KEY_ENABLED = "feature_enabled"
        private const val KEY_USER_GROUP = "user_group"
        private const val KEY_ROLLOUT_PERCENTAGE = "rollout_percentage"

        // 灰度发布的功能标识
        const val FEATURE_WAL_MODE = "wal_mode"
        const val FEATURE_AUTO_BACKUP = "auto_backup"
        const val FEATURE_DB_MONITORING = "db_monitoring"
        const val FEATURE_CONNECTION_MANAGEMENT = "connection_management"
        const val FEATURE_DATA_ARCHIVING = "data_archiving"
        const val FEATURE_DB_STRUCTURE_ANALYSIS = "db_structure_analysis"

        // 高级监控相关的功能标识
        const val FEATURE_ADVANCED_DB_MONITORING = "advanced_db_monitoring"
        const val FEATURE_QUERY_LOGGING = "query_logging"
        const val FEATURE_PERFORMANCE_ANALYSIS = "performance_analysis"
        const val FEATURE_SLOW_QUERY_DETECTION = "slow_query_detection"
        const val FEATURE_DATABASE_ANALYZER = "database_analyzer"

        // 用户组
        const val GROUP_INTERNAL = "internal"
        const val GROUP_BETA = "beta"
        const val GROUP_PRODUCTION = "production"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 检查特定功能是否启用
     * @param featureKey 功能标识
     * @return 功能是否启用
     */
    fun isFeatureEnabled(featureKey: String): Boolean {
        return prefs.getBoolean("${KEY_ENABLED}_$featureKey", false)
    }

    /**
     * 启用特定功能
     * @param featureKey 功能标识
     * @param enabled 是否启用
     */
    fun setFeatureEnabled(featureKey: String, enabled: Boolean) {
        prefs.edit().putBoolean("${KEY_ENABLED}_$featureKey", enabled).apply()
    }

    /**
     * 获取当前用户组
     * @return 用户组标识
     */
    fun getUserGroup(): String {
        return prefs.getString(KEY_USER_GROUP, GROUP_PRODUCTION) ?: GROUP_PRODUCTION
    }

    /**
     * 设置用户组
     * @param group 用户组标识
     */
    fun setUserGroup(group: String) {
        prefs.edit().putString(KEY_USER_GROUP, group).apply()
    }

    /**
     * 获取功能的灰度发布百分比
     * @param featureKey 功能标识
     * @return 灰度发布百分比 (0-100)
     */
    fun getRolloutPercentage(featureKey: String): Int {
        return prefs.getInt("${KEY_ROLLOUT_PERCENTAGE}_$featureKey", 0)
    }

    /**
     * 设置功能的灰度发布百分比
     * @param featureKey 功能标识
     * @param percentage 灰度发布百分比 (0-100)
     */
    fun setRolloutPercentage(featureKey: String, percentage: Int) {
        val validPercentage = percentage.coerceIn(0, 100)
        prefs.edit().putInt("${KEY_ROLLOUT_PERCENTAGE}_$featureKey", validPercentage).apply()
    }

    /**
     * 检查用户是否在灰度发布范围内
     * @param featureKey 功能标识
     * @param userId 用户ID
     * @return 用户是否在灰度发布范围内
     */
    fun isUserInRollout(featureKey: String, userId: String): Boolean {
        // 如果用户组是内部测试或beta测试，直接返回true
        val userGroup = getUserGroup()
        if (userGroup == GROUP_INTERNAL || userGroup == GROUP_BETA) {
            return true
        }

        // 对于生产用户，根据灰度发布百分比决定
        val percentage = getRolloutPercentage(featureKey)
        if (percentage >= 100) {
            return true
        } else if (percentage <= 0) {
            return false
        }

        // 使用用户ID的哈希值来确保同一用户始终获得相同的结果
        val hash = userId.hashCode().absoluteValue
        val userPercentile = hash % 100

        return userPercentile < percentage
    }

    /**
     * 初始化默认灰度发布配置
     */
    fun initDefaultConfig() {
        // 默认情况下，所有功能对内部测试用户启用
        setUserGroup(GROUP_INTERNAL)

        // 设置各功能的默认灰度发布配置
        setFeatureEnabled(FEATURE_WAL_MODE, true) // 启用WAL模式，提高数据安全性
        setFeatureEnabled(FEATURE_AUTO_BACKUP, true)
        setFeatureEnabled(FEATURE_DB_MONITORING, true)
        setFeatureEnabled(FEATURE_CONNECTION_MANAGEMENT, true)
        setFeatureEnabled(FEATURE_DATA_ARCHIVING, true)
        setFeatureEnabled(FEATURE_DB_STRUCTURE_ANALYSIS, true)

        // 设置高级监控相关功能的默认配置
        setFeatureEnabled(FEATURE_ADVANCED_DB_MONITORING, true)
        setFeatureEnabled(FEATURE_QUERY_LOGGING, true)
        setFeatureEnabled(FEATURE_PERFORMANCE_ANALYSIS, true)
        setFeatureEnabled(FEATURE_SLOW_QUERY_DETECTION, true)
        setFeatureEnabled(FEATURE_DATABASE_ANALYZER, true)

        // 设置各功能的默认灰度发布百分比
        setRolloutPercentage(FEATURE_WAL_MODE, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_AUTO_BACKUP, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_DB_MONITORING, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_CONNECTION_MANAGEMENT, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_DATA_ARCHIVING, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_DB_STRUCTURE_ANALYSIS, 0) // 生产环境暂不启用

        // 设置高级监控相关功能的默认灰度发布百分比
        setRolloutPercentage(FEATURE_ADVANCED_DB_MONITORING, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_QUERY_LOGGING, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_PERFORMANCE_ANALYSIS, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_SLOW_QUERY_DETECTION, 0) // 生产环境暂不启用
        setRolloutPercentage(FEATURE_DATABASE_ANALYZER, 0) // 生产环境暂不启用
    }
}
