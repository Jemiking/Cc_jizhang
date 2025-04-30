package com.ccjizhang.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.common.OperationResult
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.BackupFileInfo
import com.ccjizhang.ui.viewmodels.BackupFileType
import com.ccjizhang.ui.viewmodels.UnifiedBackupViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一备份与恢复界面
 * 整合了自动备份设置和数据备份恢复功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedBackupScreen(
    navController: NavHostController,
    viewModel: UnifiedBackupViewModel = hiltViewModel()
) {
    // 获取状态和上下文
    val state by viewModel.unifiedBackupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 选项卡状态
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("备份与恢复", "自动备份设置", "高级选项")

    // 对话框状态
    var showDataCleanupDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var showIntervalSelector by remember { mutableStateOf(false) }
    var showReminderDaysSelector by remember { mutableStateOf(false) }
    var selectedBackupFile by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    var customPath by remember { mutableStateOf(state.customBackupPath) }

    // 备份频率选项
    val backupIntervalOptions = listOf(
        Pair(1, "每天"),
        Pair(3, "每3天"),
        Pair(7, "每周"),
        Pair(14, "每两周"),
        Pair(30, "每月")
    )

    // 备份提醒天数选项
    val reminderDaysOptions = listOf(
        Pair(3, "3 天"),
        Pair(5, "5 天"),
        Pair(7, "7 天"),
        Pair(10, "10 天"),
        Pair(14, "14 天"),
        Pair(30, "30 天")
    )

    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        try {
            if (uri != null) {
                viewModel.setCustomBackupUri(uri)
            } else {
                // 用户取消选择或选择失败
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "未选择文件夹，请重试"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "选择文件夹失败：${e.localizedMessage ?: "未知错误"}"
                )
            }
        }
    }

    // 文件选择器（用于CSV导入）
    val csvFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // 这里暂时不处理CSV导入，后续实现
    }

    // 目录选择器（用于CSV导出）
    val csvExportDirPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDataToCsv(uri)
        }
    }

    // 收集操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { result ->
            when (result) {
                is OperationResult.Success -> {
                    snackbarHostState.showSnackbar(result.message ?: "操作成功")
                }
                is OperationResult.Error -> {
                    snackbarHostState.showSnackbar(result.message ?: "操作失败")
                }
                else -> {
                    // 处理其他可能的结果类型
                }
            }
        }
    }

    // 主界面
    RoundedTopBarScaffold(
        title = "数据备份与恢复",
        navController = navController,
        showBackButton = true,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 选项卡
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // 显示加载状态
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 主内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> BackupRestoreTab(
                            state = state,
                            onCreateBackup = { viewModel.createManualBackup() },
                            onSelectBackupFile = { backupFile ->
                                selectedBackupFile = backupFile
                                showRestoreConfirmDialog = true
                            },
                            onViewBackupFile = { viewModel.openBackupFile(it) },
                            onDeleteBackupFile = { backupFile ->
                                selectedBackupFile = backupFile
                                showDeleteConfirmDialog = true
                            },
                            onRefreshBackupFiles = { viewModel.loadBackupFiles() }
                        )
                        1 -> AutoBackupSettingsTab(
                            state = state,
                            onToggleAutoBackup = { enabled ->
                                if (enabled) {
                                    showIntervalSelector = true
                                } else {
                                    viewModel.disableAutoBackup()
                                }
                            },
                            onSetBackupInterval = { interval ->
                                viewModel.enableAutoBackup(interval)
                            },
                            onToggleReminder = { enabled ->
                                if (enabled) {
                                    showReminderDaysSelector = true
                                } else {
                                    viewModel.setBackupReminder(false)
                                }
                            },
                            onSetReminderDays = { days ->
                                viewModel.setBackupReminder(true, days)
                            },
                            onOpenFolderPicker = { folderPickerLauncher.launch(null) },
                            onShowPathDialog = { showPathDialog = true },
                            onClearCustomPath = { viewModel.clearCustomBackupPath() }
                        )
                        2 -> AdvancedOptionsTab(
                            onExportCsv = { csvExportDirPicker.launch(null) },
                            onImportCsv = { showCsvImportDialog = true },
                            onOpenWebDavSettings = { navController.navigate(NavRoutes.WebDavSettings) },
                            onOpenDataMigration = { navController.navigate(NavRoutes.DataMigration) },
                            onShowDataCleanup = { showDataCleanupDialog = true }
                        )
                    }
                }
            }
        }
    }

    // 文件内容查看对话框
    if (state.showFileContent && state.selectedBackupFile != null) {
        Dialog(
            onDismissRequest = { viewModel.closeFileContent() }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 标题
                    Text(
                        text = "备份文件内容",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 文件信息
                    val selectedFile = state.selectedBackupFile
                    if (selectedFile != null) {
                        val file = selectedFile.file
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                        Text(
                            text = "文件名: ${file.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "创建时间: ${dateFormat.format(Date(file.lastModified()))}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "文件大小: ${formatFileSize(file.length())}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 文件内容
                        if (state.isFileContentLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Text(
                                text = "文件内容预览:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 显示文件内容，限制最大高度并添加滚动
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = state.fileContent.take(2000) +
                                        (if (state.fileContent.length > 2000) "\n...(内容过长，已截断)" else ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 关闭按钮
                    Button(
                        onClick = { viewModel.closeFileContent() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteConfirmDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                selectedBackupFile = null
            },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除备份文件 ${selectedBackupFile?.file?.name} 吗？此操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupFile?.let { viewModel.deleteBackupFile(it) }
                        showDeleteConfirmDialog = false
                        selectedBackupFile = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        selectedBackupFile = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 恢复确认对话框
    if (showRestoreConfirmDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                selectedBackupFile = null
            },
            title = { Text("确认恢复") },
            text = {
                Column {
                    Text("确定要从备份文件 ${selectedBackupFile?.file?.name} 恢复数据吗？")
                    Text(
                        "警告：此操作将覆盖当前所有数据，建议先创建一个新的备份。",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupFile?.let { viewModel.restoreFromBackup(it) }
                        showRestoreConfirmDialog = false
                        selectedBackupFile = null
                    }
                ) {
                    Text("恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        selectedBackupFile = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 备份频率选择对话框
    if (showIntervalSelector) {
        AlertDialog(
            onDismissRequest = { showIntervalSelector = false },
            title = { Text("选择备份频率") },
            text = {
                Column {
                    backupIntervalOptions.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.enableAutoBackup(days)
                                    showIntervalSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            if (days == state.backupIntervalDays) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (days != backupIntervalOptions.last().first) {
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalSelector = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 提醒天数选择对话框
    if (showReminderDaysSelector) {
        AlertDialog(
            onDismissRequest = { showReminderDaysSelector = false },
            title = { Text("选择提醒天数") },
            text = {
                Column {
                    reminderDaysOptions.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBackupReminder(true, days)
                                    showReminderDaysSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )

                            if (days == state.reminderDays) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "已选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (days != reminderDaysOptions.last().first) {
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDaysSelector = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 路径设置对话框
    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text("手动输入备份路径") },
            text = {
                Column {
                    Text("请输入完整的备份文件夹路径，留空则使用默认路径。")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPath,
                        onValueChange = { customPath = it },
                        label = { Text("备份路径") },
                        placeholder = { Text("/storage/emulated/0/CCJiZhang/backups") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：建议使用\"选择文件夹\"功能而非手动输入，以确保应用有正确的访问权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "注意：请确保该路径存在且应用有写入权限，否则备份可能失败。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 添加"选择文件夹"按钮
                    OutlinedButton(
                        onClick = {
                            showPathDialog = false
                            folderPickerLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("打开文件夹选择器")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customPath.trim().isNotEmpty()) {
                            viewModel.setCustomBackupPath(customPath.trim())
                        }
                        showPathDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPathDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 数据清理对话框
    if (showDataCleanupDialog) {
        var clearTransactions by remember { mutableStateOf(true) }
        var clearCategories by remember { mutableStateOf(false) }
        var clearAccounts by remember { mutableStateOf(false) }
        var clearBudgets by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<Date?>(null) }

        AlertDialog(
            onDismissRequest = { showDataCleanupDialog = false },
            title = { Text("数据清理") },
            text = {
                Column {
                    Text(
                        text = "警告：此操作将永久删除选定的数据，无法恢复。建议在清理前先备份数据。",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("请选择要清理的数据类型：")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearTransactions,
                            onCheckedChange = { clearTransactions = it }
                        )
                        Text(
                            text = "交易记录",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearCategories,
                            onCheckedChange = { clearCategories = it }
                        )
                        Text(
                            text = "分类",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearAccounts,
                            onCheckedChange = { clearAccounts = it }
                        )
                        Text(
                            text = "账户",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = clearBudgets,
                            onCheckedChange = { clearBudgets = it }
                        )
                        Text(
                            text = "预算",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cleanupData(
                            clearTransactions = clearTransactions,
                            clearCategories = clearCategories,
                            clearAccounts = clearAccounts,
                            clearBudgets = clearBudgets,
                            beforeDate = selectedDate
                        )
                        showDataCleanupDialog = false
                    },
                    enabled = clearTransactions || clearCategories || clearAccounts || clearBudgets
                ) {
                    Text("清理", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataCleanupDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}

/**
 * 备份与恢复选项卡
 */
@Composable
fun BackupRestoreTab(
    state: com.ccjizhang.ui.viewmodels.UnifiedBackupState,
    onCreateBackup: () -> Unit,
    onSelectBackupFile: (com.ccjizhang.ui.viewmodels.BackupFileInfo) -> Unit,
    onViewBackupFile: (com.ccjizhang.ui.viewmodels.BackupFileInfo) -> Unit,
    onDeleteBackupFile: (com.ccjizhang.ui.viewmodels.BackupFileInfo) -> Unit,
    onRefreshBackupFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 快速操作卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "快速操作",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 创建备份按钮
                Button(
                    onClick = onCreateBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("立即创建备份")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 刷新备份列表按钮
                OutlinedButton(
                    onClick = onRefreshBackupFiles,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("刷新备份列表")
                }
            }
        }

        // 备份文件列表卡片
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "备份文件",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state.isLoadingFiles) {
                    // 显示加载状态
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.backupFiles.isEmpty()) {
                    // 显示空状态
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有找到备份文件",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 显示备份文件列表
                    Column {
                        state.backupFiles.forEach { backupFile ->
                            BackupFileItem(
                                backupFile = backupFile,
                                onSelect = { onSelectBackupFile(backupFile) },
                                onView = { onViewBackupFile(backupFile) },
                                onDelete = { onDeleteBackupFile(backupFile) }
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 备份路径信息
                Text(
                    text = "备份路径: ${state.displayPath.ifEmpty { "默认路径" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 备份文件项
 */
@Composable
fun BackupFileItem(
    backupFile: com.ccjizhang.ui.viewmodels.BackupFileInfo,
    onSelect: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val typeText = when (backupFile.type) {
        com.ccjizhang.ui.viewmodels.BackupFileType.AUTO -> "自动备份"
        com.ccjizhang.ui.viewmodels.BackupFileType.MANUAL -> "手动备份"
        com.ccjizhang.ui.viewmodels.BackupFileType.UNKNOWN -> "未知类型"
    }
    val typeColor = when (backupFile.type) {
        com.ccjizhang.ui.viewmodels.BackupFileType.AUTO -> MaterialTheme.colorScheme.tertiary
        com.ccjizhang.ui.viewmodels.BackupFileType.MANUAL -> MaterialTheme.colorScheme.primary
        com.ccjizhang.ui.viewmodels.BackupFileType.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件图标
        Icon(
            imageVector = Icons.Default.Backup,
            contentDescription = null,
            tint = typeColor,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 文件信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = backupFile.file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = typeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = typeColor,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(backupFile.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatFileSize(backupFile.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 操作按钮
        IconButton(onClick = onView) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "查看",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 自动备份设置选项卡
 */
@Composable
fun AutoBackupSettingsTab(
    state: com.ccjizhang.ui.viewmodels.UnifiedBackupState,
    onToggleAutoBackup: (Boolean) -> Unit,
    onSetBackupInterval: (Int) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onSetReminderDays: (Int) -> Unit,
    onOpenFolderPicker: () -> Unit,
    onShowPathDialog: () -> Unit,
    onClearCustomPath: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 自动备份开关设置卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "自动备份设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 自动备份开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "启用自动备份",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Switch(
                        checked = state.isAutoBackupEnabled,
                        onCheckedChange = onToggleAutoBackup
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 备份频率设置
                if (state.isAutoBackupEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "备份频率",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        OutlinedButton(
                            onClick = { onSetBackupInterval(state.backupIntervalDays) }
                        ) {
                            // 显示当前选中的备份频率
                            val intervalText = when (state.backupIntervalDays) {
                                1 -> "每天"
                                3 -> "每3天"
                                7 -> "每周"
                                14 -> "每两周"
                                30 -> "每月"
                                else -> "${state.backupIntervalDays}天"
                            }
                            Text(text = intervalText)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 备份提醒设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "备份提醒",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = onToggleReminder
                        )
                    }

                    if (state.reminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "提醒天数",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            OutlinedButton(
                                onClick = { onSetReminderDays(state.reminderDays) }
                            ) {
                                // 显示当前选中的提醒天数
                                Text(text = "${state.reminderDays}天")
                            }
                        }
                    }
                }
            }
        }

        // 备份路径设置卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "备份路径设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 当前路径显示
                Text(
                    text = "当前备份路径:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.displayPath.ifEmpty { "默认路径 (应用内部存储)" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 选择文件夹按钮
                OutlinedButton(
                    onClick = onOpenFolderPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("选择备份文件夹")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 手动输入路径按钮
                OutlinedButton(
                    onClick = onShowPathDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("手动输入路径")
                }

                // 恢复默认路径按钮
                if (state.customBackupPath.isNotEmpty() || state.customBackupUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onClearCustomPath,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("恢复默认路径")
                    }
                }
            }
        }

        // 备份说明信息卡片
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "关于自动备份",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "自动备份会定期将您的记账数据保存到指定存储路径，仅保留最近10次备份。" +
                          "如需更安全的备份方式，请使用手动备份功能导出数据到外部存储。",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "提示：为确保数据安全，建议定期手动备份数据并将备份文件保存到云存储或其他设备上。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 高级选项选项卡
 */
@Composable
fun AdvancedOptionsTab(
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onOpenWebDavSettings: () -> Unit,
    onOpenDataMigration: () -> Unit,
    onShowDataCleanup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 数据导入导出卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "数据导入导出",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CSV导出按钮
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("导出数据为CSV格式")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CSV导入按钮
                OutlinedButton(
                    onClick = onImportCsv,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("从CSV文件导入数据")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "CSV格式适用于在电子表格软件中查看和编辑数据，也可用于与其他记账软件交换数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 云同步设置卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "云同步设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // WebDAV同步设置按钮
                OutlinedButton(
                    onClick = onOpenWebDavSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("WebDAV同步设置")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "WebDAV允许您将备份文件同步到支持该协议的云存储服务，如坚果云、NextCloud等。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 数据迁移卡片
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "数据迁移",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 应用间数据迁移按钮
                OutlinedButton(
                    onClick = onOpenDataMigration,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("应用间数据迁移")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "从其他记账应用导入数据，或将数据导出到其他应用支持的格式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 数据清理卡片
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "数据清理",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 数据清理按钮
                OutlinedButton(
                    onClick = onShowDataCleanup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("清理数据")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "警告：此功能将永久删除选定的数据，无法恢复。请在清理前先备份数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}