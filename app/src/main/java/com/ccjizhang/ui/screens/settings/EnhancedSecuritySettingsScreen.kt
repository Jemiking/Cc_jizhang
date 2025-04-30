package com.ccjizhang.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
// 使用 remember 和 collectAsState 替代 livedata
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ccjizhang.ui.viewmodels.EnhancedSecuritySettingsViewModel
import com.ccjizhang.ui.viewmodels.FamilyMemberUIModel
import com.ccjizhang.utils.AccessControlHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 增强的安全设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EnhancedSecuritySettingsViewModel = hiltViewModel()
) {
    val securityStatus = remember { mutableStateOf(emptyMap<String, Any>()) }
    val familyMembers = remember { mutableStateOf(emptyList<FamilyMemberUIModel>()) }

    // 收集安全状态
    LaunchedEffect(key1 = true) {
        viewModel.loadSecurityStatus()
        viewModel.securityStatus.collect { status ->
            securityStatus.value = status
        }
    }

    // 收集家庭成员数据
    LaunchedEffect(key1 = true) {
        viewModel.loadFamilyMembers()
        viewModel.familyMembers.collect { members ->
            familyMembers.value = members
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }

    // 收集操作结果
    LaunchedEffect(key1 = true) {
        viewModel.operationResult.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 重置密码确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置数据库密码") },
            text = { Text("确定要重置数据库密码吗？这将重新加密数据库，可能需要一些时间。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDatabasePassword()
                        showResetDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安全设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 数据库加密设置
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "数据库加密",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "数据库加密",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("启用数据库加密")
                            Switch(
                                checked = securityStatus.value["encryption_enabled"] as? Boolean ?: false,
                                onCheckedChange = { viewModel.setEncryptionEnabled(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "加密可以保护您的数据安全，但可能会略微降低性能",
                            style = MaterialTheme.typography.bodySmall
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
            }

            // 访问控制设置
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "访问控制",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "访问控制",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("启用访问控制")
                            Switch(
                                checked = securityStatus.value["access_control_enabled"] as? Boolean ?: false,
                                onCheckedChange = { viewModel.setAccessControlEnabled(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "访问控制可以限制不同用户对数据的访问权限",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 密钥健康状态
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "密钥健康状态",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("上次密钥轮换")
                            Text(
                                text = securityStatus.value["key_last_rotated"] as? String ?: "未知",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("密钥健康状态")
                            val keyHealth = securityStatus.value["key_health"] as? String ?: "未知"
                            val color = when (keyHealth) {
                                "良好" -> MaterialTheme.colorScheme.primary
                                "即将过期" -> MaterialTheme.colorScheme.tertiary
                                "需要轮换" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = keyHealth,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 家庭成员权限管理
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "家庭成员",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "家庭成员权限管理",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (familyMembers.value.isEmpty()) {
                            Text(
                                text = "暂无家庭成员",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Column {
                                for (member in familyMembers.value) {
                                    FamilyMemberItem(
                                        member = member,
                                        onRoleChanged = { newRole ->
                                            viewModel.updateMemberRole(member.id, newRole)
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 家庭成员项
 */
@Composable
fun FamilyMemberItem(
    member: FamilyMemberUIModel,
    onRoleChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "状态: ${member.statusName}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column {
            Button(onClick = { expanded = true }) {
                Text(member.roleName)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("拥有者") },
                    onClick = {
                        onRoleChanged(AccessControlHelper.ROLE_OWNER)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("管理员") },
                    onClick = {
                        onRoleChanged(AccessControlHelper.ROLE_ADMIN)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("编辑者") },
                    onClick = {
                        onRoleChanged(AccessControlHelper.ROLE_EDITOR)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("查看者") },
                    onClick = {
                        onRoleChanged(AccessControlHelper.ROLE_VIEWER)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("访客") },
                    onClick = {
                        onRoleChanged(AccessControlHelper.ROLE_GUEST)
                        expanded = false
                    }
                )
            }
        }
    }
}
