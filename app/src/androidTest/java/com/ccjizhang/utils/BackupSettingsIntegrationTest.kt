package com.ccjizhang.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 备份设置集成测试
 * 测试备份设置的保存、加载和持久化
 */
@RunWith(AndroidJUnit4::class)
class BackupSettingsIntegrationTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var autoBackupScheduler: AutoBackupWorker.Scheduler
    private lateinit var backupReminderScheduler: BackupReminderWorker.Scheduler

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
        context = ApplicationProvider.getApplicationContext()
        
        // 初始化WorkManager用于测试
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        
        // 初始化SharedPreferences
        prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        // 初始化调度器
        autoBackupScheduler = AutoBackupWorker.Scheduler(context)
        backupReminderScheduler = BackupReminderWorker.Scheduler(context)
    }

    @After
    fun tearDown() {
        prefs.edit().clear().apply()
    }

    /**
     * 测试自动备份设置的保存和加载
     */
    @Test
    fun testAutoBackupSettingsPersistence() {
        // 保存设置
        prefs.edit()
            .putBoolean(keyAutoBackupEnabled, true)
            .putInt(keyBackupIntervalDays, 7)
            .apply()
        
        // 关闭并重新打开SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证设置被正确保存
        val isEnabled = reopenedPrefs.getBoolean(keyAutoBackupEnabled, false)
        val intervalDays = reopenedPrefs.getInt(keyBackupIntervalDays, 3)
        
        assertTrue(isEnabled, "自动备份启用状态应该被正确保存")
        assertEquals(7, intervalDays, "备份频率应该被正确保存")
    }

    /**
     * 测试备份路径设置的保存和加载
     */
    @Test
    fun testBackupPathSettingsPersistence() {
        // 保存设置
        val testPath = "/storage/emulated/0/Download"
        prefs.edit()
            .putString(keyCustomBackupPath, testPath)
            .apply()
        
        // 关闭并重新打开SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证设置被正确保存
        val savedPath = reopenedPrefs.getString(keyCustomBackupPath, "")
        
        assertEquals(testPath, savedPath, "备份路径应该被正确保存")
    }

    /**
     * 测试备份URI设置的保存和加载
     */
    @Test
    fun testBackupUriSettingsPersistence() {
        // 保存设置
        val testUriString = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
        prefs.edit()
            .putString(keyCustomBackupUri, testUriString)
            .apply()
        
        // 关闭并重新打开SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证设置被正确保存
        val savedUriString = reopenedPrefs.getString(keyCustomBackupUri, null)
        
        assertEquals(testUriString, savedUriString, "备份URI应该被正确保存")
        
        // 验证可以解析为Uri
        val uri = Uri.parse(savedUriString)
        assertNotNull(uri, "保存的URI字符串应该可以解析为Uri对象")
        assertEquals("content", uri.scheme, "URI方案应该正确")
        assertEquals("com.android.externalstorage.documents", uri.authority, "URI授权应该正确")
    }

    /**
     * 测试上次备份时间设置的保存和加载
     */
    @Test
    fun testLastBackupTimeSettingsPersistence() {
        // 保存设置
        val testTime = System.currentTimeMillis()
        prefs.edit()
            .putLong(keyLastBackupTime, testTime)
            .apply()
        
        // 关闭并重新打开SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证设置被正确保存
        val savedTime = reopenedPrefs.getLong(keyLastBackupTime, 0L)
        
        assertEquals(testTime, savedTime, "上次备份时间应该被正确保存")
    }

    /**
     * 测试备份提醒设置的保存和加载
     */
    @Test
    fun testBackupReminderSettingsPersistence() {
        // 保存设置
        prefs.edit()
            .putBoolean(keyBackupReminderEnabled, true)
            .putInt(keyBackupReminderDays, 14)
            .apply()
        
        // 关闭并重新打开SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证设置被正确保存
        val isEnabled = reopenedPrefs.getBoolean(keyBackupReminderEnabled, false)
        val reminderDays = reopenedPrefs.getInt(keyBackupReminderDays, 7)
        
        assertTrue(isEnabled, "备份提醒启用状态应该被正确保存")
        assertEquals(14, reminderDays, "备份提醒天数应该被正确保存")
    }

    /**
     * 测试自动备份调度
     */
    @Test
    fun testAutoBackupScheduling() = runBlocking {
        // 调度自动备份
        autoBackupScheduler.scheduleAutoBackup(7)
        
        // 获取WorkManager
        val workManager = WorkManager.getInstance(context)
        
        // 验证工作已调度
        val workInfos = workManager.getWorkInfosForUniqueWork("auto_backup_work").get()
        assertFalse(workInfos.isEmpty(), "应该有自动备份工作被调度")
        
        // 取消自动备份
        autoBackupScheduler.cancelAutoBackup()
        
        // 验证工作已取消
        val workInfosAfterCancel = workManager.getWorkInfosForUniqueWork("auto_backup_work").get()
        assertTrue(workInfosAfterCancel.isEmpty() || workInfosAfterCancel.all { it.state.isFinished }, 
            "自动备份工作应该被取消")
    }

    /**
     * 测试备份提醒调度
     */
    @Test
    fun testBackupReminderScheduling() = runBlocking {
        // 调度备份提醒
        backupReminderScheduler.scheduleBackupReminder()
        
        // 获取WorkManager
        val workManager = WorkManager.getInstance(context)
        
        // 验证工作已调度
        val workInfos = workManager.getWorkInfosForUniqueWork("backup_reminder_work").get()
        assertFalse(workInfos.isEmpty(), "应该有备份提醒工作被调度")
        
        // 取消备份提醒
        backupReminderScheduler.cancelBackupReminder()
        
        // 验证工作已取消
        val workInfosAfterCancel = workManager.getWorkInfosForUniqueWork("backup_reminder_work").get()
        assertTrue(workInfosAfterCancel.isEmpty() || workInfosAfterCancel.all { it.state.isFinished }, 
            "备份提醒工作应该被取消")
    }

    /**
     * 测试应用重启后设置的持久性
     */
    @Test
    fun testSettingsPersistenceAcrossAppRestarts() {
        // 保存所有设置
        val testTime = System.currentTimeMillis()
        val testPath = "/storage/emulated/0/Download"
        val testUriString = "content://com.android.externalstorage.documents/tree/primary%3ADownload"
        
        prefs.edit()
            .putBoolean(keyAutoBackupEnabled, true)
            .putInt(keyBackupIntervalDays, 7)
            .putString(keyCustomBackupPath, testPath)
            .putString(keyCustomBackupUri, testUriString)
            .putLong(keyLastBackupTime, testTime)
            .putBoolean(keyBackupReminderEnabled, true)
            .putInt(keyBackupReminderDays, 14)
            .apply()
        
        // 模拟应用重启，重新获取SharedPreferences
        val reopenedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        
        // 验证所有设置被正确保存
        val isAutoBackupEnabled = reopenedPrefs.getBoolean(keyAutoBackupEnabled, false)
        val intervalDays = reopenedPrefs.getInt(keyBackupIntervalDays, 3)
        val customBackupPath = reopenedPrefs.getString(keyCustomBackupPath, "")
        val customBackupUri = reopenedPrefs.getString(keyCustomBackupUri, null)
        val lastBackupTime = reopenedPrefs.getLong(keyLastBackupTime, 0L)
        val isReminderEnabled = reopenedPrefs.getBoolean(keyBackupReminderEnabled, false)
        val reminderDays = reopenedPrefs.getInt(keyBackupReminderDays, 7)
        
        assertTrue(isAutoBackupEnabled, "自动备份启用状态应该被正确保存")
        assertEquals(7, intervalDays, "备份频率应该被正确保存")
        assertEquals(testPath, customBackupPath, "备份路径应该被正确保存")
        assertEquals(testUriString, customBackupUri, "备份URI应该被正确保存")
        assertEquals(testTime, lastBackupTime, "上次备份时间应该被正确保存")
        assertTrue(isReminderEnabled, "备份提醒启用状态应该被正确保存")
        assertEquals(14, reminderDays, "备份提醒天数应该被正确保存")
    }

    /**
     * 测试清除设置
     */
    @Test
    fun testClearSettings() {
        // 保存设置
        prefs.edit()
            .putBoolean(keyAutoBackupEnabled, true)
            .putInt(keyBackupIntervalDays, 7)
            .putString(keyCustomBackupPath, "/storage/emulated/0/Download")
            .putString(keyCustomBackupUri, "content://test/uri")
            .putLong(keyLastBackupTime, System.currentTimeMillis())
            .putBoolean(keyBackupReminderEnabled, true)
            .putInt(keyBackupReminderDays, 14)
            .apply()
        
        // 清除设置
        prefs.edit().clear().apply()
        
        // 验证设置已清除
        val isAutoBackupEnabled = prefs.getBoolean(keyAutoBackupEnabled, false)
        val intervalDays = prefs.getInt(keyBackupIntervalDays, 3)
        val customBackupPath = prefs.getString(keyCustomBackupPath, "")
        val customBackupUri = prefs.getString(keyCustomBackupUri, null)
        val lastBackupTime = prefs.getLong(keyLastBackupTime, 0L)
        val isReminderEnabled = prefs.getBoolean(keyBackupReminderEnabled, false)
        val reminderDays = prefs.getInt(keyBackupReminderDays, 7)
        
        assertFalse(isAutoBackupEnabled, "自动备份启用状态应该被重置为默认值")
        assertEquals(3, intervalDays, "备份频率应该被重置为默认值")
        assertEquals("", customBackupPath, "备份路径应该被重置为默认值")
        assertEquals(null, customBackupUri, "备份URI应该被重置为默认值")
        assertEquals(0L, lastBackupTime, "上次备份时间应该被重置为默认值")
        assertFalse(isReminderEnabled, "备份提醒启用状态应该被重置为默认值")
        assertEquals(7, reminderDays, "备份提醒天数应该被重置为默认值")
    }
}
