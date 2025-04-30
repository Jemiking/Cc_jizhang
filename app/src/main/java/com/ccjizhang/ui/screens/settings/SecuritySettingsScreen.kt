package com.ccjizhang.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.SecuritySettingsViewModel
import kotlinx.coroutines.launch

/**
 * 安全设置界面
 */
@Composable
fun SecuritySettingsScreen(
    navController: NavHostController,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // 状态
    var showResetDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 操作结果处理
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { result ->
            snackbarHostState.showSnackbar(result)
        }
    }
    
    RoundedTopBarScaffold(
        title = "安全设置",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.padding(paddingValues)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // 数据库加密状态卡片
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "数据库加密",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "您的数据库已加密，所有财务数据都受到保护。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = "重置加密密码"
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text("重置加密密码")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 加密说明
                Text(
                    text = "关于数据库加密",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "CC记账使用业界标准的SQLCipher技术对您的财务数据进行加密存储，即使设备丢失或被盗，您的数据也是安全的。",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "加密密码存储在设备的安全区域，除非重置应用数据，否则不会丢失。重置密码将需要对整个数据库重新加密，可能需要较长时间。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // 重置密码确认对话框
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("重置加密密码") },
                    text = { 
                        Text("重置加密密码将对整个数据库重新加密，这可能需要较长时间，取决于数据库大小。在此期间请勿关闭应用。确定要继续吗？") 
                    },
                    confirmButton = { 
                        Button(
                            onClick = {
                                showResetDialog = false
                                isLoading = true
                                scope.launch {
                                    val success = viewModel.resetDatabasePassword()
                                    isLoading = false
                                    if (success) {
                                        snackbarHostState.showSnackbar("加密密码重置成功")
                                    } else {
                                        snackbarHostState.showSnackbar("加密密码重置失败")
                                    }
                                }
                            }
                        ) {
                            Text("确定重置")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
            
            // 加载对话框
            if (isLoading) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("正在重置数据库密码") },
                    text = { 
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在对数据库重新加密，请勿关闭应用...")
                        }
                    },
                    confirmButton = { }
                )
            }
        }
    }
} 