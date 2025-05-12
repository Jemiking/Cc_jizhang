package com.ccjizhang.ui.screens.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.components.AppInfoDialog
import com.ccjizhang.ui.components.ClearDataConfirmDialog
import com.ccjizhang.ui.components.FeedbackDialog
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.SettingsItem
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.theme.CCJiZhangTheme
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * 设置页面
 *
 * @param navController 导航控制器
 * @param onNavigateBack 返回上一页的回调
 * @param viewModel 设置页面的ViewModel
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // 获取状态和上下文
    val settingsState by viewModel.settingsState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // 对话框状态
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    // 收集操作结果
    LaunchedEffect(Unit) {
        viewModel.operationResult.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        ClearDataConfirmDialog(
            onConfirm = {
                viewModel.clearAllData()
                showClearDataDialog = false
            },
            onDismiss = { showClearDataDialog = false }
        )
    }

    // 应用信息对话框
    if (showAppInfoDialog) {
        AppInfoDialog(
            appVersion = settingsState.appVersion,
            buildNumber = settingsState.appBuildNumber,
            databaseVersion = settingsState.databaseVersion,
            onDismiss = { showAppInfoDialog = false }
        )
    }

    // 反馈对话框
    if (showFeedbackDialog) {
        FeedbackDialog(
            onSubmit = { content, contactInfo ->
                // 发送反馈邮件
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@ccjizhang.com")
                        putExtra(Intent.EXTRA_SUBJECT, "CC记账 - 用户反馈")
                        putExtra(Intent.EXTRA_TEXT, "反馈内容：\n$content\n\n联系方式：\n$contactInfo")
                    }
                    context.startActivity(intent)
                    scope.launch {
                        snackbarHostState.showSnackbar("感谢您的反馈！")
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar("无法发送反馈，请检查您的邮件应用设置")
                    }
                }
                showFeedbackDialog = false
            },
            onDismiss = { showFeedbackDialog = false }
        )
    }

    UnifiedScaffold(
        title = "设置",
        navController = navController,
        showBackButton = true,
        onBackClick = onNavigateBack,
        showFloatingActionButton = false
    ) { paddingValues ->
        // 显示加载状态
        if (settingsState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                // 用户资料卡片
                UserProfileCard(navController = navController)


                    // 数据管理部分
                    Text(
                        text = "数据管理",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 16.dp),
                        // 支持动态字体大小
                        fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                        // 添加字体粗细以提高可读性
                        fontWeight = FontWeight.SemiBold
                    )

                // 数据管理设置项
                // 使用缓存的数据管理设置项
                val dataManagementItems = remember { viewModel.getSettingItemsByCategory("数据管理") }

                // 显示数据管理设置项
                dataManagementItems.forEach { item ->
                    SettingsItem(
                        icon = com.ccjizhang.ui.utils.IconUtils.getIconForName(item.icon),
                        title = item.title,
                        subtitle = item.subtitle ?: "",
                        onClick = {
                            if (item.route != null) {
                                navController.navigate(item.route)
                            } else if (item.id == "clear_data") {
                                showClearDataDialog = true
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 外观设置部分
                Text(
                    text = "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp),
                    // 支持动态字体大小
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    // 添加字体粗细以提高可读性
                    fontWeight = FontWeight.SemiBold
                )

                // 使用缓存的外观设置项
                val appearanceItems = remember { viewModel.getSettingItemsByCategory("外观设置") }

                // 显示外观设置项
                appearanceItems.forEach { item ->
                    SettingsItem(
                        icon = com.ccjizhang.ui.utils.IconUtils.getIconForName(item.icon),
                        title = item.title,
                        subtitle = item.subtitle ?: "",
                        onClick = {
                            if (item.route != null) {
                                navController.navigate(item.route)
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                // 高级功能部分
                Text(
                    text = "高级功能",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.SemiBold
                )

                // 使用缓存的高级功能设置项
                val advancedItems = remember { viewModel.getSettingItemsByCategory("高级功能") }

                // 显示高级功能设置项
                advancedItems.forEach { item ->
                    SettingsItem(
                        icon = com.ccjizhang.ui.utils.IconUtils.getIconForName(item.icon),
                        title = item.title,
                        subtitle = item.subtitle ?: "",
                        onClick = {
                            if (item.route != null) {
                                navController.navigate(item.route)
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // 开发者工具部分
                Text(
                    text = "开发者工具",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    fontWeight = FontWeight.SemiBold
                )

                // 使用缓存的开发者工具设置项
                val developerItems = remember { viewModel.getSettingItemsByCategory("开发者工具") }

                // 显示开发者工具设置项
                developerItems.forEach { item ->
                    SettingsItem(
                        icon = com.ccjizhang.ui.utils.IconUtils.getIconForName(item.icon),
                        title = item.title,
                        subtitle = item.subtitle ?: "",
                        onClick = {
                            if (item.route != null) {
                                navController.navigate(item.route)
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                // 关于部分
                Text(
                    text = "关于",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp),
                    // 支持动态字体大小
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                    // 添加字体粗细以提高可读性
                    fontWeight = FontWeight.SemiBold
                )

                // 使用缓存的关于设置项
                val aboutItems = remember { viewModel.getSettingItemsByCategory("关于") }

                // 显示关于设置项
                aboutItems.forEach { item ->
                    SettingsItem(
                        icon = com.ccjizhang.ui.utils.IconUtils.getIconForName(item.icon),
                        title = item.title,
                        subtitle = item.subtitle ?: "",
                        onClick = {
                            when (item.id) {
                                "app_info" -> showAppInfoDialog = true
                                "feedback" -> showFeedbackDialog = true
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 用户资料卡片
 */
@Composable
fun UserProfileCard(navController: NavHostController) {
    com.ccjizhang.ui.components.PrimaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClick = { navController.navigate(NavRoutes.UserProfile) },
                // 添加语义属性，提高屏幕阅读器可用性
                onClickLabel = "编辑个人资料"
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = PrimaryDark,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "张小明的用户头像",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "张小明",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    // 支持动态字体大小
                    fontFamily = MaterialTheme.typography.titleMedium.fontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "xiaoming@example.com",
                    style = MaterialTheme.typography.bodyMedium,
                    // 提高文字对比度
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    // 支持动态字体大小
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily
                )
            }

            // 编辑按钮
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "编辑个人资料",
                // 提高图标对比度
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    CCJiZhangTheme {
        SettingsScreen(rememberNavController())
    }
}