package com.ccjizhang.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.DataMigrationViewModel
import com.ccjizhang.utils.AppDataMigrationHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 应用间数据迁移界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataMigrationScreen(
    navController: NavHostController,
    viewModel: DataMigrationViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val migrationState by viewModel.migrationState.collectAsState()
    
    var selectedAppType by remember { mutableStateOf(AppDataMigrationHelper.Companion.AppType.UNKNOWN) }
    var showAppTypeDialog by remember { mutableStateOf(false) }
    
    // 文件选择
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (selectedAppType == AppDataMigrationHelper.Companion.AppType.UNKNOWN) {
                showAppTypeDialog = true
            } else {
                viewModel.importFromThirdPartyApp(uri, selectedAppType)
            }
        }
    }
    
    // 监听操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    RoundedTopBarScaffold(
        title = "应用间数据迁移",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = Modifier.padding(paddingValues)
        ) { innerPadding ->
            if (migrationState is DataMigrationViewModel.MigrationState.Migrating) {
                // 显示导入进度
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (migrationState as DataMigrationViewModel.MigrationState.Migrating).message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (migrationState is DataMigrationViewModel.MigrationState.Success) {
                // 显示导入成功
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "导入成功",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (migrationState as DataMigrationViewModel.MigrationState.Success).message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetState() }
                        ) {
                            Text("返回")
                        }
                    }
                }
            } else if (migrationState is DataMigrationViewModel.MigrationState.Error) {
                // 显示导入失败
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "导入失败",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "导入失败",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (migrationState as DataMigrationViewModel.MigrationState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.resetState() }
                        ) {
                            Text("返回")
                        }
                    }
                }
            } else {
                // 默认界面 - 选择导入方式
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "从其他记账应用导入数据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "您可以从其他记账应用导出的文件导入数据，目前支持以下格式：",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            AppTypeSelector(
                                selectedAppType = selectedAppType,
                                onAppTypeSelected = { selectedAppType = it }
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { filePicker.launch("*/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("选择文件并导入")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "导入说明",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Divider()
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "1. Money Manager：请先在原应用中导出JSON格式数据",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "2. Money Lover：请先在原应用中通过设置-导出数据导出",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "3. AndMoney：请先在原应用中导出CSV格式数据",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Divider()
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "注意：导入过程可能需要一些时间，请耐心等待。导入完成后，您可以在账户、分类和交易记录中查看导入的数据。",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 应用类型选择对话框
    if (showAppTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAppTypeDialog = false },
            title = { Text("选择应用类型") },
            text = {
                Column {
                    Text("请选择导入文件的来源应用")
                    Spacer(modifier = Modifier.height(16.dp))
                    AppTypeSelector(
                        selectedAppType = selectedAppType,
                        onAppTypeSelected = { selectedAppType = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAppTypeDialog = false
                        // 重新打开文件选择器
                        filePicker.launch("*/*")
                    },
                    enabled = selectedAppType != AppDataMigrationHelper.Companion.AppType.UNKNOWN
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppTypeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 应用类型选择组件
 */
@Composable
fun AppTypeSelector(
    selectedAppType: AppDataMigrationHelper.Companion.AppType,
    onAppTypeSelected: (AppDataMigrationHelper.Companion.AppType) -> Unit
) {
    Column {
        AppTypeRadioButton(
            text = "Money Manager (JSON格式)",
            selected = selectedAppType == AppDataMigrationHelper.Companion.AppType.MONEY_MANAGER,
            onClick = { onAppTypeSelected(AppDataMigrationHelper.Companion.AppType.MONEY_MANAGER) }
        )
        
        AppTypeRadioButton(
            text = "Money Lover (JSON格式)",
            selected = selectedAppType == AppDataMigrationHelper.Companion.AppType.MONEY_LOVER,
            onClick = { onAppTypeSelected(AppDataMigrationHelper.Companion.AppType.MONEY_LOVER) }
        )
        
        AppTypeRadioButton(
            text = "AndMoney (CSV格式)",
            selected = selectedAppType == AppDataMigrationHelper.Companion.AppType.AND_MONEY,
            onClick = { onAppTypeSelected(AppDataMigrationHelper.Companion.AppType.AND_MONEY) }
        )
        
        AppTypeRadioButton(
            text = "自动检测 (推荐)",
            selected = selectedAppType == AppDataMigrationHelper.Companion.AppType.UNKNOWN,
            onClick = { onAppTypeSelected(AppDataMigrationHelper.Companion.AppType.UNKNOWN) }
        )
    }
}

/**
 * 应用类型单选按钮
 */
@Composable
fun AppTypeRadioButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
} 