package com.ccjizhang.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ccjizhang.data.model.WebDavConfig
import com.ccjizhang.ui.viewmodels.WebDavSettingsViewModel
import com.ccjizhang.utils.WebDavSyncManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * WebDAV设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavSettingsScreen(
    navController: NavController,
    viewModel: WebDavSettingsViewModel = hiltViewModel()
) {
    val webDavConfig by viewModel.webDavConfig.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // 临时配置状态，用于表单编辑
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var syncFolder by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(false) }
    var syncIntervalHours by remember { mutableFloatStateOf(24f) }

    // 当WebDAV配置加载完成后，更新表单状态
    LaunchedEffect(webDavConfig) {
        webDavConfig?.let {
            serverUrl = it.serverUrl
            username = it.username
            password = it.password
            syncFolder = it.syncFolder
            autoSync = it.autoSync
            syncIntervalHours = it.syncInterval.toFloat()
        }
    }

    // 监听操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { result ->
            val message = when (result) {
                is WebDavSettingsViewModel.OperationResult.Success -> result.message
                is WebDavSettingsViewModel.OperationResult.Error -> result.message
            }
            val duration = if (result is WebDavSettingsViewModel.OperationResult.Error) {
                SnackbarDuration.Long
            } else {
                SnackbarDuration.Short
            }
            snackbarHostState.showSnackbar(message = message, duration = duration)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV同步设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (webDavConfig != null && !serverUrl.isNullOrBlank() && !username.isNullOrBlank()) {
                FloatingActionButton(
                    onClick = {
                        val config = WebDavConfig(
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            syncFolder = syncFolder,
                            autoSync = autoSync,
                            syncInterval = syncIntervalHours.roundToInt().toLong()
                        )
                        viewModel.saveWebDavConfig(config)
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "保存")
                }
            }
        }
    ) { padding ->
        if (syncStatus is WebDavSyncManager.SyncStatus.Syncing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (syncStatus as WebDavSyncManager.SyncStatus.Syncing).message,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "服务器设置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("WebDAV服务器地址") },
                            placeholder = { Text("例如: https://dav.example.com/") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名") },
                            placeholder = { Text("WebDAV账号") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            placeholder = { Text("WebDAV密码") },
                            visualTransformation = if (showPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showPasswordVisible = !showPasswordVisible }) {
                                    Text(if (showPasswordVisible) "隐藏" else "显示")
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = syncFolder,
                            onValueChange = { syncFolder = it },
                            label = { Text("同步文件夹") },
                            placeholder = { Text("在WebDAV服务器上的文件夹名") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                val config = WebDavConfig(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    syncFolder = syncFolder,
                                    autoSync = autoSync,
                                    syncInterval = syncIntervalHours.roundToInt().toLong()
                                )
                                viewModel.testConnection(config)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = serverUrl.isNotBlank() && username.isNotBlank() && 
                                    connectionState != WebDavSettingsViewModel.ConnectionState.Testing
                        ) {
                            if (connectionState == WebDavSettingsViewModel.ConnectionState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试中...")
                            } else {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("测试连接")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "同步设置",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("自动同步")
                            Switch(
                                checked = autoSync,
                                onCheckedChange = { autoSync = it }
                            )
                        }
                        
                        if (autoSync) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "同步频率: ${syncIntervalHours.roundToInt()}小时",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Slider(
                                value = syncIntervalHours,
                                onValueChange = { syncIntervalHours = it },
                                valueRange = 1f..168f,
                                steps = 167
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (webDavConfig != null && webDavConfig?.serverUrl?.isNotBlank() == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "同步操作",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.sync() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("立即同步到服务器")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { showRestoreConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("从服务器恢复数据")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除WebDAV配置")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "提示：WebDAV是一种通用的文件访问协议，常用于网盘和云存储服务。通过设置WebDAV同步，可以将记账数据同步到支持WebDAV的网盘服务（如坚果云、Box、NextCloud等）。",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除配置") },
            text = { Text("确定要删除WebDAV配置吗？这将停止自动同步。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWebDavConfig()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("确定")
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
    
    // 恢复确认对话框
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("从服务器恢复") },
            text = { Text("确定要从服务器恢复数据吗？这将覆盖本地数据。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.downloadAndRestore()
                        showRestoreConfirmDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreConfirmDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
} 