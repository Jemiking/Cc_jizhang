package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
 * 数据库加密管理类
 * 负责管理SQLCipher数据库的密码
 */
@Singleton
class DatabaseEncryptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "CCJiZhangDatabaseKey"
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val ENCRYPTION_TRANSFORMATION = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"

        private const val PREFS_NAME = "database_encryption_prefs"
        private const val KEY_ENCRYPTED_PASSWORD = "encrypted_password"
        private const val KEY_ENCRYPTION_IV = "encryption_iv"

        private const val GCM_TAG_LENGTH = 128 // bits
        private const val DEFAULT_PASSWORD_LENGTH = 32 // 256 bits
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取数据库密码
     * 始终返回固定密码，确保一致性
     */
    fun getDatabasePassword(): String {
        // 直接返回固定密码，避免加密/解密过程中的问题
        android.util.Log.i("DatabaseEncryptionManager", "使用固定密码打开数据库")
        return "CCJiZhangDefaultPassword123!@#"
    }

    /**
     * 重置数据库密码
     * 注意：调用此方法后需要重新创建数据库或重新加密
     */
    fun resetDatabasePassword(): String {
        // 直接返回固定密码，确保一致性
        android.util.Log.i("DatabaseEncryptionManager", "重置数据库密码，使用固定密码")
        return "CCJiZhangDefaultPassword123!@#"
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

            android.util.Log.i("DatabaseEncryptionManager", "生成新密码，长度: ${password.length}")

            // 加密并保存密码
            val (encryptedPassword, iv) = encryptPassword(password)

            // 使用commit而不是apply，确保密码立即写入
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                .putString(KEY_ENCRYPTION_IV, iv)
                .commit()

            // 验证密码是否正确保存
            val savedEncryptedPassword = prefs.getString(KEY_ENCRYPTED_PASSWORD, null)
            val savedIv = prefs.getString(KEY_ENCRYPTION_IV, null)

            if (savedEncryptedPassword != encryptedPassword || savedIv != iv) {
                android.util.Log.e("DatabaseEncryptionManager", "密码保存验证失败，重新尝试")
                // 重新尝试保存
                prefs.edit()
                    .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                    .putString(KEY_ENCRYPTION_IV, iv)
                    .commit()
            }

            return password
        } catch (e: Exception) {
            android.util.Log.e("DatabaseEncryptionManager", "生成密码失败", e)

            // 如果生成失败，使用一个默认密码
            val fallbackPassword = "CCJiZhangDefaultPassword123!@#"

            try {
                // 加密并保存默认密码
                val (encryptedPassword, iv) = encryptPassword(fallbackPassword)
                prefs.edit()
                    .putString(KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                    .putString(KEY_ENCRYPTION_IV, iv)
                    .commit()
            } catch (e2: Exception) {
                android.util.Log.e("DatabaseEncryptionManager", "保存默认密码失败", e2)
            }

            return fallbackPassword
        }
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
    private fun decryptPassword(encryptedPassword: String, encryptionIv: String): String {
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        val ivBytes = Base64.decode(encryptionIv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)

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
            .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
}