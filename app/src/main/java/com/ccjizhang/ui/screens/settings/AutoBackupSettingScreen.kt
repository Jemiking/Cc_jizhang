package com.ccjizhang.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent as AndroidIntent
import android.net.Uri as AndroidUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.AutoBackupState
import com.ccjizhang.ui.viewmodels.AutoBackupViewModel
import com.ccjizhang.ui.common.OperationResult
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collectLatest

/**
 * 自动备份设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupSettingScreen(
    navController: NavHostController,
    viewModel: AutoBackupViewModel = hiltViewModel()
) {
    val state by viewModel.autoBackupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 备份频率选项
    val backupIntervalOptions = listOf(
        Pair(1, "每天"),
        Pair(3, "每3天"),
        Pair(7, "每周"),
        Pair(14, "每两周"),
        Pair(30, "每月")
    )

    // 状态
    var showIntervalSelector by remember { mutableStateOf(false) }
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFileContentDialog by remember { mutableStateOf(false) }
    var fileContent by remember { mutableStateOf("") }
    var isLoadingContent by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    var customPath by remember { mutableStateOf(state.customBackupPath) }
    var showPresetPathsDialog by remember { mutableStateOf(false) }

    // 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: AndroidUri? ->
        try {
            if (uri != null) {
                viewModel.setCustomBackupUri(uri)
            } else {
                // 用户取消选择或选择失败
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "未选择文件夹，请重试",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "选择文件夹失败：${e.localizedMessage ?: "未知错误"}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // 监听操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { result ->
            val message = when (result) {
                is OperationResult.Success -> result.message ?: "操作成功"
                is OperationResult.Error -> result.message
                is OperationResult.Loading -> "正在处理..."
            }
            val duration = if (result is OperationResult.Error) {
                SnackbarDuration.Long
            } else {
                SnackbarDuration.Short
            }
            scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = duration)
            }
        }
    }

    RoundedTopBarScaffold(
        title = "自动备份设置",
        onBackClick = { navController.navigateUp() },
        showBackButton = true
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 自动备份开关设置卡片
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
                            checked = state.isEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    viewModel.enableAutoBackup(state.intervalDays)
                                } else {
                                    viewModel.disableAutoBackup()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 备份频率设置
                    if (state.isEnabled) {
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
                                val currentOption = backupIntervalOptions.find { it.first == state.intervalDays }
                                Text(text = currentOption?.second ?: "选择频率")
                            }
                        }
                    }
                }
            }

            // 现有备份文件卡片
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

                    if (state.isLoadingFiles) {
                        Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state.backupFiles.isEmpty()) {
                        Text(
                            text = "暂无自动备份文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 列出备份文件
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            items(state.backupFiles) { file ->
                                val formattedDate = SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date(file.lastModified()))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable {
                                            // 点击打开文件内容
                                            selectedBackupFile = file
                                            isLoadingContent = true
                                            showFileContentDialog = true
                                            scope.launch {
                                                try {
                                                    val result = viewModel.openBackupFile(file)
                                                    if (result.isSuccess) {
                                                        fileContent = result.getOrDefault("")
                                                    } else {
                                                        fileContent = "无法读取文件内容: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                                                    }
                                                } catch (e: Exception) {
                                                    fileContent = "读取文件失败: ${e.message}"
                                                } finally {
                                                    isLoadingContent = false
                                                }
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 文件图标
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 文件信息
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "创建时间: $formattedDate",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "大小: ${String.format("%.2f", file.length() / 1024.0)} KB",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "点击查看内容",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // 操作按钮
                                    IconButton(
                                        onClick = {
                                            selectedBackupFile = file
                                            showDeleteConfirmDialog = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除备份文件",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Divider()
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 手动创建备份按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.createManualBackup()
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(text = "立即备份")
                        }
                    }
                }
            }

            // 备份路径设置卡片
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
                        text = "备份路径设置",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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

                        // 手动输入按钮
                        OutlinedButton(
                            onClick = { showPathDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("手动输入")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 显示当前路径
                    Text(
                        text = if (state.displayPath.isNotEmpty())
                                  "当前路径: ${state.displayPath}"
                              else
                                  "默认路径（应用内部存储）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.displayPath.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                    )

                    // 常用路径选项
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "常用路径",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 常用路径按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoBackupViewModel.PRESET_PATHS.forEach { (path, displayName) ->
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
                        Spacer(modifier = Modifier.height(16.dp))
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

            // 备份说明信息
            Card(
                modifier = Modifier.fillMaxWidth()
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
                        text = "自动备份会定期将您的记账数据保存到指定存储路径，仅保留最近5次备份。" +
                              "如需更安全的备份方式，请使用数据备份功能导出数据到外部存储。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // 备份频率选择对话框
    if (showIntervalSelector) {
        Dialog(onDismissRequest = { showIntervalSelector = false }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("选择备份频率", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    backupIntervalOptions.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (days == state.intervalDays),
                                onClick = {
                                    viewModel.updateBackupInterval(days)
                                    showIntervalSelector = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    // 删除备份确认对话框
    if (showDeleteConfirmDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除备份文件 \"${selectedBackupFile?.name}\" 吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedBackupFile?.let {
                            viewModel.deleteBackupFile(it)
                        }
                        showDeleteConfirmDialog = false
                        selectedBackupFile = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // 文件内容查看对话框
    if (showFileContentDialog && selectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = {
                showFileContentDialog = false
                selectedBackupFile = null
                fileContent = ""
            },
            title = { Text("备份文件内容") },
            text = {
                if (isLoadingContent) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column {
                        Text("文件名：${selectedBackupFile?.name}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("路径：${selectedBackupFile?.absolutePath}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("内容预览：")
                        Spacer(modifier = Modifier.height(4.dp))
                        // 显示文件内容，限制长度
                        val previewContent = if (fileContent.length > 500) {
                            fileContent.substring(0, 500) + "..."
                        } else {
                            fileContent
                        }
                        Text(
                            text = previewContent,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .height(200.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFileContentDialog = false
                        selectedBackupFile = null
                        fileContent = ""
                    }
                ) {
                    Text("关闭")
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
                        text = "提示：建议使用“选择文件夹”功能而非手动输入，以确保应用有正确的访问权限。",
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

                    // 添加“选择文件夹”按钮
                    OutlinedButton(
                        onClick = {
                            showPathDialog = false
                            folderPickerLauncher.launch(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
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
}