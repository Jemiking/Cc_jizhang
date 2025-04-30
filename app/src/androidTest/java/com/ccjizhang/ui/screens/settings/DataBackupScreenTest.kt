package com.ccjizhang.ui.screens.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ccjizhang.ui.common.OperationResult
import com.ccjizhang.ui.viewmodels.BackupRestoreState
import com.ccjizhang.ui.viewmodels.BackupRestoreViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DataBackupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: BackupRestoreViewModel
    private lateinit var navController: NavHostController
    private val backupRestoreState = MutableStateFlow(BackupRestoreState())
    private val operationResult = MutableSharedFlow<OperationResult>()

    @Before
    fun setup() {
        // 模拟ViewModel
        viewModel = mockk(relaxed = true)
        every { viewModel.backupRestoreState } returns backupRestoreState
        every { viewModel.operationResult } returns operationResult

        // 设置Compose测试规则
        composeTestRule.setContent {
            navController = rememberNavController()
            DataBackupScreen(navController = navController, viewModel = viewModel)
        }
    }

    @Test
    fun testAutoBackupSwitchDisplayed() {
        composeTestRule.onNodeWithText("启用自动备份").assertIsDisplayed()
        composeTestRule.onNode(hasText("启用自动备份") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun testAutoBackupSwitchToggle() {
        // 初始状态：自动备份禁用
        backupRestoreState.value = BackupRestoreState(isAutoBackupEnabled = false)

        // 点击开关
        composeTestRule.onNode(hasText("启用自动备份").parent()).performClick()

        // 验证ViewModel方法被调用
        verify { viewModel.setAutoBackupEnabled(true) }
    }

    @Test
    fun testBackupIntervalSelectorDisplayed() {
        // 设置状态：自动备份启用
        backupRestoreState.value = BackupRestoreState(isAutoBackupEnabled = true, backupIntervalDays = 3)

        // 验证备份频率选择器显示
        composeTestRule.onNodeWithText("备份频率").assertIsDisplayed()
        composeTestRule.onNodeWithText("每3天").assertIsDisplayed()
    }

    @Test
    fun testBackupReminderSwitchDisplayed() {
        // 设置状态：自动备份启用
        backupRestoreState.value = BackupRestoreState(isAutoBackupEnabled = true)

        // 验证备份提醒开关显示
        composeTestRule.onNodeWithText("备份提醒").assertIsDisplayed()
    }

    @Test
    fun testBackupReminderSwitchToggle() {
        // 设置状态：自动备份启用，备份提醒禁用
        backupRestoreState.value = BackupRestoreState(
            isAutoBackupEnabled = true,
            isBackupReminderEnabled = false
        )

        // 点击开关
        composeTestRule.onNode(hasText("备份提醒").parent()).performClick()

        // 验证ViewModel方法被调用
        verify { viewModel.setBackupReminder(true) }
    }

    @Test
    fun testBackupReminderDaysSelectorDisplayed() {
        // 设置状态：自动备份启用，备份提醒启用
        backupRestoreState.value = BackupRestoreState(
            isAutoBackupEnabled = true,
            isBackupReminderEnabled = true,
            backupReminderDays = 7
        )

        // 验证备份提醒天数选择器显示
        composeTestRule.onNodeWithText("提醒天数").assertIsDisplayed()
        composeTestRule.onNodeWithText("7 天").assertIsDisplayed()
    }

    @Test
    fun testBackupFilesListDisplayed() {
        // 创建测试备份文件
        val backupFile = mockk<File>()
        every { backupFile.name } returns "test_backup.json"
        every { backupFile.lastModified() } returns System.currentTimeMillis()
        every { backupFile.length() } returns 1024L

        // 设置状态：有备份文件
        backupRestoreState.value = BackupRestoreState(
            backupFiles = listOf(backupFile)
        )

        // 验证备份文件列表显示
        composeTestRule.onNodeWithText("备份文件").assertIsDisplayed()
        composeTestRule.onNodeWithText("test_backup.json").assertIsDisplayed()
    }

    @Test
    fun testEmptyBackupFilesMessage() {
        // 设置状态：没有备份文件
        backupRestoreState.value = BackupRestoreState(
            backupFiles = emptyList()
        )

        // 验证空备份文件消息显示
        composeTestRule.onNodeWithText("暂无备份文件").assertIsDisplayed()
    }

    @Test
    fun testManualBackupButtonDisplayed() {
        // 设置状态：自动备份启用
        backupRestoreState.value = BackupRestoreState(isAutoBackupEnabled = true)

        // 验证手动备份按钮显示
        composeTestRule.onNodeWithText("立即备份").assertIsDisplayed()
    }

    @Test
    fun testManualBackupButtonClick() {
        // 设置状态：自动备份启用
        backupRestoreState.value = BackupRestoreState(isAutoBackupEnabled = true)

        // 点击手动备份按钮
        composeTestRule.onNodeWithText("立即备份").performClick()

        // 验证ViewModel方法被调用
        verify { viewModel.createManualBackup() }
    }

    @Test
    fun testDataExportImportCardsDisplayed() {
        // 验证数据导出导入卡片显示
        composeTestRule.onNodeWithText("数据导出").assertExists()
        composeTestRule.onNodeWithText("数据导入").assertExists()
    }
}
