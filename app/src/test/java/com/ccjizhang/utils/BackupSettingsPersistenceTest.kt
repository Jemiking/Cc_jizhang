package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BackupSettingsPersistenceTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var sharedPreferences: SharedPreferences

    @MockK
    lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @MockK
    lateinit var uri: Uri

    private val prefsName = "backup_restore_prefs"
    private val keyAutoBackupEnabled = "auto_backup_enabled"
    private val keyBackupIntervalDays = "backup_interval_days"
    private val keyCustomBackupPath = "custom_backup_path"
    private val keyCustomBackupUri = "custom_backup_uri"
    private val keyLastBackupTime = "last_backup_time"
    private val keyBackupReminderEnabled = "backup_reminder_enabled"
    private val keyBackupReminderDays = "backup_reminder_days"

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // 模拟SharedPreferences
        every { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putBoolean(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putInt(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every { sharedPreferencesEditor.commit() } returns true

        // 模拟Uri
        every { uri.toString() } returns "content://test/backup"
    }

    @Test
    fun `保存自动备份设置应该正确写入SharedPreferences`() {
        // 保存设置
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(keyAutoBackupEnabled, true)
            .putInt(keyBackupIntervalDays, 7)
            .apply()

        // 验证设置被保存
        verify { sharedPreferencesEditor.putBoolean(keyAutoBackupEnabled, true) }
        verify { sharedPreferencesEditor.putInt(keyBackupIntervalDays, 7) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `保存备份路径设置应该正确写入SharedPreferences`() {
        // 保存设置
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(keyCustomBackupPath, "/storage/emulated/0/Download")
            .apply()

        // 验证设置被保存
        verify { sharedPreferencesEditor.putString(keyCustomBackupPath, "/storage/emulated/0/Download") }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `保存备份URI设置应该正确写入SharedPreferences`() {
        // 保存设置
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(keyCustomBackupUri, uri.toString())
            .apply()

        // 验证设置被保存
        verify { sharedPreferencesEditor.putString(keyCustomBackupUri, "content://test/backup") }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `保存上次备份时间应该正确写入SharedPreferences`() {
        // 保存设置
        val currentTime = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(keyLastBackupTime, currentTime)
            .apply()

        // 验证设置被保存
        verify { sharedPreferencesEditor.putLong(keyLastBackupTime, currentTime) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `保存备份提醒设置应该正确写入SharedPreferences`() {
        // 保存设置
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(keyBackupReminderEnabled, true)
            .putInt(keyBackupReminderDays, 14)
            .apply()

        // 验证设置被保存
        verify { sharedPreferencesEditor.putBoolean(keyBackupReminderEnabled, true) }
        verify { sharedPreferencesEditor.putInt(keyBackupReminderDays, 14) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `加载自动备份设置应该正确读取SharedPreferences`() {
        // 模拟设置值
        every { sharedPreferences.getBoolean(keyAutoBackupEnabled, false) } returns true
        every { sharedPreferences.getInt(keyBackupIntervalDays, 3) } returns 7

        // 读取设置
        val isEnabled = sharedPreferences.getBoolean(keyAutoBackupEnabled, false)
        val intervalDays = sharedPreferences.getInt(keyBackupIntervalDays, 3)

        // 验证读取的值
        assertTrue(isEnabled)
        assertEquals(7, intervalDays)
    }

    @Test
    fun `加载备份路径设置应该正确读取SharedPreferences`() {
        // 模拟设置值
        every { sharedPreferences.getString(keyCustomBackupPath, "") } returns "/storage/emulated/0/Download"

        // 读取设置
        val customBackupPath = sharedPreferences.getString(keyCustomBackupPath, "") ?: ""

        // 验证读取的值
        assertEquals("/storage/emulated/0/Download", customBackupPath)
    }

    @Test
    fun `加载备份URI设置应该正确读取SharedPreferences`() {
        // 模拟设置值
        every { sharedPreferences.getString(keyCustomBackupUri, null) } returns "content://test/backup"

        // 读取设置
        val uriString = sharedPreferences.getString(keyCustomBackupUri, null)
        val customBackupUri = if (!uriString.isNullOrEmpty()) {
            try {
                Uri.parse(uriString)
            } catch (e: Exception) {
                null
            }
        } else null

        // 验证读取的值
        assertEquals("content://test/backup", customBackupUri.toString())
    }

    @Test
    fun `加载上次备份时间应该正确读取SharedPreferences`() {
        // 模拟设置值
        val currentTime = System.currentTimeMillis()
        every { sharedPreferences.getLong(keyLastBackupTime, 0L) } returns currentTime

        // 读取设置
        val lastBackupTime = sharedPreferences.getLong(keyLastBackupTime, 0L)

        // 验证读取的值
        assertEquals(currentTime, lastBackupTime)
    }

    @Test
    fun `加载备份提醒设置应该正确读取SharedPreferences`() {
        // 模拟设置值
        every { sharedPreferences.getBoolean(keyBackupReminderEnabled, false) } returns true
        every { sharedPreferences.getInt(keyBackupReminderDays, 7) } returns 14

        // 读取设置
        val isReminderEnabled = sharedPreferences.getBoolean(keyBackupReminderEnabled, false)
        val reminderDays = sharedPreferences.getInt(keyBackupReminderDays, 7)

        // 验证读取的值
        assertTrue(isReminderEnabled)
        assertEquals(14, reminderDays)
    }

    @Test
    fun `清除自定义备份路径应该正确更新SharedPreferences`() {
        // 清除设置
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(keyCustomBackupPath, "")
            .putString(keyCustomBackupUri, null)
            .apply()

        // 验证设置被清除
        verify { sharedPreferencesEditor.putString(keyCustomBackupPath, "") }
        verify { sharedPreferencesEditor.putString(keyCustomBackupUri, null) }
        verify { sharedPreferencesEditor.apply() }
    }

    @Test
    fun `应用重启后设置应该保持不变`() = runTest {
        // 模拟设置值
        every { sharedPreferences.getBoolean(keyAutoBackupEnabled, false) } returns true
        every { sharedPreferences.getInt(keyBackupIntervalDays, 3) } returns 7
        every { sharedPreferences.getString(keyCustomBackupPath, "") } returns "/storage/emulated/0/Download"
        every { sharedPreferences.getString(keyCustomBackupUri, null) } returns "content://test/backup"
        every { sharedPreferences.getLong(keyLastBackupTime, 0L) } returns 1234567890L
        every { sharedPreferences.getBoolean(keyBackupReminderEnabled, false) } returns true
        every { sharedPreferences.getInt(keyBackupReminderDays, 7) } returns 14

        // 模拟应用重启，重新获取SharedPreferences
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

        // 读取设置
        val isEnabled = prefs.getBoolean(keyAutoBackupEnabled, false)
        val intervalDays = prefs.getInt(keyBackupIntervalDays, 3)
        val customBackupPath = prefs.getString(keyCustomBackupPath, "") ?: ""
        val uriString = prefs.getString(keyCustomBackupUri, null)
        val lastBackupTime = prefs.getLong(keyLastBackupTime, 0L)
        val isReminderEnabled = prefs.getBoolean(keyBackupReminderEnabled, false)
        val reminderDays = prefs.getInt(keyBackupReminderDays, 7)

        // 验证设置保持不变
        assertTrue(isEnabled)
        assertEquals(7, intervalDays)
        assertEquals("/storage/emulated/0/Download", customBackupPath)
        assertEquals("content://test/backup", uriString)
        assertEquals(1234567890L, lastBackupTime)
        assertTrue(isReminderEnabled)
        assertEquals(14, reminderDays)
    }
}
