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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.FamilyMemberUIModel
import com.ccjizhang.ui.viewmodels.UnifiedSecurityViewModel
import com.ccjizhang.utils.AccessControlHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 统一安全设置界面
 * 整合了SecuritySettingsScreen和EnhancedSecuritySettingsScreen的功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSecurityScreen(
    navController: NavHostController,
    viewModel: UnifiedSecurityViewModel = hiltViewModel()
) {
    val securityState by viewModel.securityState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 对话框状态
    var showResetDialog by remember { mutableStateOf(false) }

    // 收集操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 重置密码确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置数据库密码") },
            text = {
                Text("确定要重置数据库密码吗？这将重新加密数据库，可能需要一些时间。")
            },
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

    RoundedTopBarScaffold(
        title = "安全设置",
        navController = navController,
        showBackButton = true,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (securityState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 安全评分卡片
                item {
                    SecurityScoreCard(
                        score = securityState.securityScore,
                        recommendations = securityState.securityRecommendations
                    )
                }

                // 数据库加密设置卡片
                item {
                    DatabaseEncryptionCard(
                        isEncryptionEnabled = securityState.isEncryptionEnabled,
                        encryptionType = securityState.encryptionType,
                        encryptionStrength = securityState.encryptionStrength,
                        lastPasswordChange = securityState.lastPasswordChange,
                        onEncryptionToggle = { viewModel.setEncryptionEnabled(it) },
                        onResetPassword = { showResetDialog = true }
                    )
                }

                // 访问控制设置卡片
                item {
                    AccessControlCard(
                        isAccessControlEnabled = securityState.isAccessControlEnabled,
                        onAccessControlToggle = { viewModel.setAccessControlEnabled(it) }
                    )
                }

                // 家庭成员管理卡片
                if (securityState.isAccessControlEnabled) {
                    item {
                        FamilyMembersCard(
                            familyMembers = securityState.familyMembers,
                            onRoleChanged = { memberId, newRole ->
                                viewModel.updateMemberRole(memberId, newRole)
                            }
                        )
                    }
                }

                // 安全建议卡片
                item {
                    SecurityRecommendationsCard(
                        recommendations = securityState.securityRecommendations
                    )
                }
            }
        }
    }
}

/**
 * 安全评分卡片
 */
@Composable
fun SecurityScoreCard(
    score: Int,
    recommendations: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "安全评分",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 安全评分圆形进度条
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // 背景圆环
                CircularProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 8.dp
                )

                // 进度圆环
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(120.dp),
                    color = when {
                        score >= 80 -> Color.Green
                        score >= 60 -> Color.Yellow
                        else -> Color.Red
                    },
                    strokeWidth = 8.dp
                )

                // 评分文本
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "分",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 安全状态文本
            Text(
                text = when {
                    score >= 80 -> "安全状态良好"
                    score >= 60 -> "安全状态一般"
                    else -> "安全状态需要改进"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    score >= 80 -> Color.Green
                    score >= 60 -> Color.Yellow
                    else -> Color.Red
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 建议数量
            if (recommendations.isNotEmpty()) {
                Text(
                    text = "有 ${recommendations.size} 条安全建议需要处理",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "没有安全建议，您的设置已经很安全",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 数据库加密设置卡片
 */
@Composable
fun DatabaseEncryptionCard(
    isEncryptionEnabled: Boolean,
    encryptionType: String,
    encryptionStrength: String,
    lastPasswordChange: String,
    onEncryptionToggle: (Boolean) -> Unit,
    onResetPassword: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "数据库加密",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "数据库加密",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 加密开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "启用数据库加密",
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = isEncryptionEnabled,
                    onCheckedChange = onEncryptionToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "加密可以保护您的数据安全，但可能会略微降低性能",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 加密详情
            if (isEncryptionEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))

                // 加密类型
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "加密类型:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )

                    Text(
                        text = encryptionType,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 加密强度
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "加密强度:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )

                    Text(
                        text = encryptionStrength,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 最后密码更改时间
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "密码更新:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(100.dp)
                    )

                    Text(
                        text = lastPasswordChange,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 重置密码按钮
                Button(
                    onClick = onResetPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "重置加密密码",
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("重置加密密码")
                }
            }
        }
    }
}

/**
 * 访问控制设置卡片
 */
@Composable
fun AccessControlCard(
    isAccessControlEnabled: Boolean,
    onAccessControlToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "访问控制",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "访问控制",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 访问控制开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "启用访问控制",
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = isAccessControlEnabled,
                    onCheckedChange = onAccessControlToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "访问控制可以限制不同用户对数据的访问权限",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 家庭成员管理卡片
 */
@Composable
fun FamilyMembersCard(
    familyMembers: List<FamilyMemberUIModel>,
    onRoleChanged: (Long, Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "家庭成员",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "家庭成员管理",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (familyMembers.isEmpty()) {
                Text(
                    text = "暂无家庭成员",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column {
                    for (member in familyMembers) {
                        FamilyMemberItemCard(
                            member = member,
                            onRoleChanged = { newRole ->
                                onRoleChanged(member.id, newRole)
                            }
                        )

                        if (member != familyMembers.last()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
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
private fun FamilyMemberItemCard(
    member: FamilyMemberUIModel,
    onRoleChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 成员头像
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "成员头像",
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 成员信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = member.statusName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 角色选择器
        Column {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(member.roleName)

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "展开角色选择器",
                    modifier = Modifier.size(20.dp)
                )
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

/**
 * 安全建议卡片
 */
@Composable
fun SecurityRecommendationsCard(
    recommendations: List<String>
) {
    if (recommendations.isEmpty()) {
        return
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "安全建议",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "安全建议",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendations.forEach { recommendation ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "警告",
                            tint = Color.Yellow,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
