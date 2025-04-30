package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库安全管理器
 * 负责数据库加密、密钥管理和访问控制
 */
@Singleton
class DatabaseSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DatabaseSecurityManager"

        // 密钥存储相关常量
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "CCJiZhangDatabaseKey"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val ENCRYPTION_TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

        // 偏好设置相关常量
        private const val PREFS_NAME = "database_security_prefs"
        private const val KEY_ENCRYPTED_PASSWORD = "encrypted_password"
        private const val KEY_ENCRYPTION_IV = "encryption_iv"
        private const val KEY_ENCRYPTION_ENABLED = "encryption_enabled"
        private const val KEY_LAST_KEY_ROTATION = "last_key_rotation"
        private const val KEY_ACCESS_CONTROL_ENABLED = "access_control_enabled"

        // 加密相关常量
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val DEFAULT_PASSWORD_LENGTH = 32 // 256 bits
        private const val KEY_ROTATION_INTERVAL = 90 * 24 * 60 * 60 * 1000L // 90天，毫秒
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 初始化安全管理器
     */
    fun initialize() {
        try {
            // 检查是否需要轮换密钥
            checkAndRotateKeyIfNeeded()

            // 确保密码已经生成
            if (!prefs.contains(KEY_ENCRYPTED_PASSWORD)) {
                generateAndSaveNewPassword()
            }

            Log.i(TAG, "数据库安全管理器初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "数据库安全管理器初始化失败", e)
        }
    }

    /**
     * 获取数据库密码
     */
    fun getDatabasePassword(): String {
        // 检查是否启用了加密
        if (!isEncryptionEnabled()) {
            Log.i(TAG, "数据库加密未启用，使用默认密码")
            return getDefaultPassword()
        }

        try {
            // 尝试获取已保存的加密密码
            val encryptedPassword = prefs.getString(KEY_ENCRYPTED_PASSWORD, null)
            val iv = prefs.getString(KEY_ENCRYPTION_IV, null)

            if (encryptedPassword != null && iv != null) {
                // 解密密码
                return decryptPassword(encryptedPassword, iv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取数据库密码失败", e)
        }

        // 如果获取失败，生成新密码
        return generateAndSaveNewPassword()
    }

    /**
     * 重置数据库密码
     */
    fun resetDatabasePassword(): String {
        return generateAndSaveNewPassword()
    }

    /**
     * 启用或禁用数据库加密
     */
    fun setEncryptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENCRYPTION_ENABLED, enabled).apply()
        Log.i(TAG, "数据库加密已${if (enabled) "启用" else "禁用"}")
    }

    /**
     * 检查数据库加密是否启用
     */
    fun isEncryptionEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENCRYPTION_ENABLED, true)
    }

    /**
     * 启用或禁用访问控制
     */
    fun setAccessControlEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ACCESS_CONTROL_ENABLED, enabled).apply()
        Log.i(TAG, "数据库访问控制已${if (enabled) "启用" else "禁用"}")
    }

    /**
     * 检查访问控制是否启用
     */
    fun isAccessControlEnabled(): Boolean {
        return prefs.getBoolean(KEY_ACCESS_CONTROL_ENABLED, false)
    }

    /**
     * 检查用户是否有权限访问指定资源
     * @param userId 用户ID
     * @param resourceId 资源ID
     * @param requiredPermission 所需权限级别
     * @return 是否有权限
     */
    fun hasPermission(userId: Long, resourceId: Long, requiredPermission: Int): Boolean {
        // 如果访问控制未启用，直接返回true
        if (!isAccessControlEnabled()) {
            return true
        }

        // TODO: 实现基于角色的权限检查
        // 这里需要根据实际需求实现，可能需要查询数据库中的权限表

        return true // 默认允许访问
    }

    /**
     * 生成并保存新密码
     */
    private fun generateAndSaveNewPassword(): String {
        try {
            // 生成随机密码
            val random = SecureRandom()
            val passwordBytes = ByteArray(DEFAULT_PASSWORD_LENGTH)
            random.nextBytes(passwordBytes)

            // 确保密码不包含特殊字符，使用URL安全编码
            val password = Base64.encodeToString(passwordBytes, Base64.URL_SAFE or Base64.NO_WRAP)

            Log.i(TAG, "生成新密码，长度: ${password.length}")

            // 加密并保存密码
            val (encryptedPassword, iv) = encryptPassword(password)

            // 使用commit而不是apply，确保密码立即写入
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                .putString(KEY_ENCRYPTION_IV, iv)
                .commit()

            return password
        } catch (e: Exception) {
            Log.e(TAG, "生成密码失败", e)

            // 如果生成失败，使用默认密码
            return getDefaultPassword()
        }
    }

    /**
     * 获取默认密码
     */
    private fun getDefaultPassword(): String {
        return "CCJiZhangDefaultPassword123!@#"
    }

    /**
     * 加密密码
     */
    private fun encryptPassword(password: String): Pair<String, String> {
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(password.toByteArray())
        val encryptedPassword = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)

        return Pair(encryptedPassword, iv)
    }

    /**
     * 解密密码
     */
    private fun decryptPassword(encryptedPassword: String, ivString: String): String {
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        val iv = Base64.decode(ivString, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

        val encryptedBytes = Base64.decode(encryptedPassword, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes)
    }

    /**
     * 获取或创建密钥
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        return if (keyStore.containsAlias(KEY_ALIAS)) {
            // 获取已存在的密钥
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        } else {
            // 创建新密钥
            val keyGenerator = KeyGenerator.getInstance(
                ENCRYPTION_ALGORITHM,
                KEYSTORE_PROVIDER
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false) // 不需要用户认证
                .build()

            keyGenerator.init(keyGenParameterSpec)
            val key = keyGenerator.generateKey()

            // 记录密钥创建时间
            prefs.edit().putLong(KEY_LAST_KEY_ROTATION, System.currentTimeMillis()).apply()

            key
        }
    }

    /**
     * 检查并在需要时轮换密钥
     */
    private fun checkAndRotateKeyIfNeeded() {
        val lastRotation = prefs.getLong(KEY_LAST_KEY_ROTATION, 0)
        val currentTime = System.currentTimeMillis()

        // 如果密钥已经超过轮换间隔，或者从未轮换过，则轮换密钥
        if (lastRotation == 0L || currentTime - lastRotation > KEY_ROTATION_INTERVAL) {
            try {
                // 先获取当前密码
                val currentPassword = getDatabasePassword()

                // 删除旧密钥
                val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
                keyStore.load(null)
                if (keyStore.containsAlias(KEY_ALIAS)) {
                    keyStore.deleteEntry(KEY_ALIAS)
                }

                // 创建新密钥
                getOrCreateSecretKey()

                // 使用新密钥重新加密当前密码
                val (encryptedPassword, iv) = encryptPassword(currentPassword)
                prefs.edit()
                    .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                    .putString(KEY_ENCRYPTION_IV, iv)
                    .putLong(KEY_LAST_KEY_ROTATION, currentTime)
                    .apply()

                Log.i(TAG, "密钥轮换成功")
            } catch (e: Exception) {
                Log.e(TAG, "密钥轮换失败", e)
            }
        }
    }

    /**
     * 获取当前用户ID
     * 注意：这里简化实现，返回固定值。实际应用中应该从用户会话或偏好设置中获取
     * @return 当前用户ID
     */
    fun getCurrentUserId(): Long {
        // 这里简化实现，返回固定值
        // 实际应用中应该从用户会话或偏好设置中获取
        return 1L
    }

    /**
     * 检查当前用户是否为管理员
     * @return 是否为管理员
     */
    fun isCurrentUserAdmin(): Boolean {
        // 这里简化实现，假设当前用户是管理员
        // 实际应用中应该根据当前用户角色判断
        return true
    }

    /**
     * 获取安全状态报告
     */
    fun getSecurityReport(): Map<String, Any> {
        val report = mutableMapOf<String, Any>()

        // 加密状态
        report["encryption_enabled"] = isEncryptionEnabled()

        // 访问控制状态
        report["access_control_enabled"] = isAccessControlEnabled()

        // 密钥信息
        val lastRotation = prefs.getLong(KEY_LAST_KEY_ROTATION, 0)
        report["key_last_rotated"] = if (lastRotation > 0) {
            val daysAgo = (System.currentTimeMillis() - lastRotation) / (24 * 60 * 60 * 1000)
            "$daysAgo 天前"
        } else {
            "从未"
        }

        // 密钥健康状态
        val keyHealth = when {
            lastRotation == 0L -> "未知"
            System.currentTimeMillis() - lastRotation > KEY_ROTATION_INTERVAL -> "需要轮换"
            System.currentTimeMillis() - lastRotation > KEY_ROTATION_INTERVAL * 0.8 -> "即将过期"
            else -> "良好"
        }
        report["key_health"] = keyHealth

        return report
    }
}
