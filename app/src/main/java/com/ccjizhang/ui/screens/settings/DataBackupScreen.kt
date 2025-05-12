package com.ccjizhang.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.ccjizhang.ui.common.DatePickerDialog

/**
 * 数据备份与恢复界面
 * 整合了自动备份设置功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBackupScreen(
    navController: NavHostController,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.backupRestoreState.collectAsState()

    // 对话框状态
    var showDataCleanupDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var showIntervalSelector by remember { mutableStateOf(false) }
    var showReminderDaysSelector by remember { mutableStateOf(false) }
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFileContentDialog by remember { mutableStateOf(false) }
    var fileContent by remember { mutableStateOf("") }
    var isLoadingContent by remember { mutableStateOf(false) }
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

    // 导出为JSON
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        viewModel.exportDataToJson(context, uri)
                    } else {
                        Toast.makeText(context, "导出失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "导出失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // 导出为CSV
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        viewModel.exportDataToCsv(context, uri)
                    } else {
                        Toast.makeText(context, "导出失败：未能获取文件夹路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "导出失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // 导入JSON
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        viewModel.importDataFromJson(context, uri)
                    } else {
                        Toast.makeText(context, "导入失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "导入失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导入失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // 数据验证
    val validateDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        viewModel.validateImportData(context, uri)
                    } else {
                        Toast.makeText(context, "验证失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "验证失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "验证失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // CSV导入相关
    var selectedCategoryUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAccountUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBudgetUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTransactionUri by remember { mutableStateOf<Uri?>(null) }

    val importCategoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        selectedCategoryUri = uri
                    } else {
                        Toast.makeText(context, "选择分类文件失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "选择分类文件失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "选择分类文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    val importAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        selectedAccountUri = uri
                    } else {
                        Toast.makeText(context, "选择账户文件失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "选择账户文件失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "选择账户文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    val importBudgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        selectedBudgetUri = uri
                    } else {
                        Toast.makeText(context, "选择预算文件失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "选择预算文件失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "选择预算文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    val importTransactionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val uri = intent.data
                    if (uri != null) {
                        selectedTransactionUri = uri
                    } else {
                        Toast.makeText(context, "选择交易文件失败：未能获取文件路径", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "选择交易文件失败：未能获取结果数据", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "选择交易文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    // 监听操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { result ->
            val message = when (result) {
                is com.ccjizhang.ui.common.OperationResult.Success -> result.message ?: "操作成功"
                is com.ccjizhang.ui.common.OperationResult.Error -> result.message
                is com.ccjizhang.ui.common.OperationResult.Loading -> "正在处理..."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // 如果是成功的导入操作，清除选择的CSV文件
            if (result is com.ccjizhang.ui.common.OperationResult.Success && showCsvImportDialog) {
                selectedCategoryUri = null
                selectedAccountUri = null
                selectedBudgetUri = null
                selectedTransactionUri = null
                showCsvImportDialog = false
            }
        }
    }

    // 手动创建备份
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportDataToJson(context, uri)
        } else {
            Toast.makeText(context, "未选择保存位置", Toast.LENGTH_SHORT).show()
        }
    }

    // 主界面
    UnifiedScaffold(
        title = "数据备份与恢复",
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = false
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 自动备份设置卡片
                PrimaryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        viewModel.enableAutoBackup(state.backupIntervalDays)
                                    } else {
                                        viewModel.disableAutoBackup()
                                    }
                                }
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
                                    onClick = { showIntervalSelector = true }
                                ) {
                                    // 显示当前选中的备份频率
                                    val currentOption = backupIntervalOptions.find { it.first == state.backupIntervalDays }
                                    Text(text = currentOption?.second ?: "选择频率")
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
                                    checked = state.isBackupReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setBackupReminder(enabled)
                                    }
                                )
                            }

                            // 备份提醒天数设置
                            if (state.isBackupReminderEnabled) {
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
                                        onClick = { showReminderDaysSelector = true }
                                    ) {
                                        Text(text = "${state.backupReminderDays} 天")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 备份路径设置
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "当前备份路径",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // 选择文件夹按钮
                                OutlinedButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text("选择文件夹")
                                }
                            }

                            // 显示当前路径
                            Text(
                                text = if (state.displayPath.isNotEmpty())
                                    "当前路径: ${state.displayPath}"
                                else
                                    "默认路径（应用内部存储）",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.displayPath.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                            )

                            // 常用路径选项
                            Text(
                                text = "常用路径",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BackupRestoreViewModel.PRESET_PATHS.forEach { (_, displayName) ->
                                    OutlinedButton(
                                        onClick = {
                                            // 这里只是打开文件选择器，实际路径选择还是由系统完成
                                            folderPickerLauncher.launch(null)
                                        }
                                    ) {
                                        Text(displayName)
                                    }
                                }
                            }

                            // 恢复默认路径按钮
                            if (state.customBackupPath.isNotEmpty() || state.customBackupUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.clearCustomBackupPath() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("恢复默认路径")
                                }
                            }
                        }
                    }
                }

                // 备份文件卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 备份文件列表
                        if (state.backupFiles.isEmpty()) {
                            Text(
                                text = "暂无备份文件",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                items(state.backupFiles) { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                selectedBackupFile = file
                                                isLoadingContent = true
                                                showFileContentDialog = true
                                                scope.launch {
                                                    try {
                                                        val result = viewModel.openBackupFile(file)
                                                        fileContent = result.getOrElse { "无法读取文件内容: ${it.message}" }
                                                    } catch (e: Exception) {
                                                        fileContent = "无法读取文件内容: ${e.message}"
                                                    } finally {
                                                        isLoadingContent = false
                                                    }
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = file.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            val lastModified = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
                                            Text(
                                                text = lastModified,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedBackupFile = file
                                                showDeleteConfirmDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 刷新备份文件列表按钮
                        Button(
                            onClick = { viewModel.refreshBackupFiles() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "刷新备份文件列表")
                        }
                    }
                }

                // 数据导入导出卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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

                        // 导入数据按钮
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                    }
                                    importJsonLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "选择文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "从文件导入数据")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 验证导入数据按钮
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                    }
                                    validateDataLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "选择文件失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "验证导入数据")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 立即创建备份按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val result = viewModel.createManualBackup()
                                        if (result.isSuccess) {
                                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                            createBackupLauncher.launch("ccjizhang_backup_${timestamp}.json")
                                        } else {
                                            Toast.makeText(context, "创建备份失败: ${result.exceptionOrNull()?.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "创建备份失败: ${e.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "立即创建备份")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 导出为JSON文件按钮
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_TITLE, "ccjizhang_export_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.json")
                                    }
                                    exportJsonLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "选择导出目录失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "导出为JSON文件")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 导出为CSV文件按钮
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_TITLE, "ccjizhang_export_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
                                    }
                                    exportCsvLauncher.launch(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "选择导出目录失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "导出为CSV文件")
                        }

                        // WebDAV同步设置按钮
                        OutlinedButton(
                            onClick = { navController.navigate(NavRoutes.WebDavSettings) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null
                            )
                            Text(
                                text = "WebDAV同步设置",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        // 应用间数据迁移按钮
                        OutlinedButton(
                            onClick = { navController.navigate(NavRoutes.DataMigration) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null
                            )
                            Text(
                                text = "应用间数据迁移",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // 显示加载指示器
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // 备份频率选择器对话框
        if (showIntervalSelector) {
            AlertDialog(
                onDismissRequest = { showIntervalSelector = false },
                title = { Text("选择备份频率") },
                text = {
                    Column {
                        backupIntervalOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateBackupInterval(option.first)
                                        showIntervalSelector = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.backupIntervalDays == option.first,
                                    onClick = {
                                        viewModel.updateBackupInterval(option.first)
                                        showIntervalSelector = false
                                    }
                                )
                                Text(
                                    text = option.second,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
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

        // 备份提醒天数选择器对话框
        if (showReminderDaysSelector) {
            AlertDialog(
                onDismissRequest = { showReminderDaysSelector = false },
                title = { Text("选择提醒天数") },
                text = {
                    Column {
                        reminderDaysOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateBackupReminderDays(option.first)
                                        showReminderDaysSelector = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.backupReminderDays == option.first,
                                    onClick = {
                                        viewModel.updateBackupReminderDays(option.first)
                                        showReminderDaysSelector = false
                                    }
                                )
                                Text(
                                    text = option.second,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
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

        // 删除确认对话框
        if (showDeleteConfirmDialog && selectedBackupFile != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("删除备份文件") },
                text = { Text("确定要删除备份文件 ${selectedBackupFile?.name} 吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedBackupFile?.let { viewModel.deleteBackupFile(it) }
                            showDeleteConfirmDialog = false
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // 文件内容对话框
        if (showFileContentDialog && selectedBackupFile != null) {
            Dialog(
                onDismissRequest = { showFileContentDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "文件内容: ${selectedBackupFile?.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLoadingContent) {
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
                                text = fileContent,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showFileContentDialog = false }) {
                                Text("关闭")
                            }

                            // 从此文件恢复按钮
                            Button(
                                onClick = {
                                    selectedBackupFile?.let {
                                        scope.launch {
                                            try {
                                                val uri = Uri.fromFile(it)
                                                viewModel.importDataFromJson(context, uri)
                                                showFileContentDialog = false
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "恢复失败: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("从此文件恢复")
                            }
                        }
                    }
                }
            }
        }
    }
}