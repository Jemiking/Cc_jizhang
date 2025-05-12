package com.ccjizhang.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import com.ccjizhang.ui.viewmodels.NotificationSettingsViewModel

/**
 * 通知设置页面
 */
@Composable
fun NotificationSettingsScreen(
    navController: NavHostController,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val notificationState by viewModel.notificationState.collectAsState()

    UnifiedScaffold(
        title = "通知设置",
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = false
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 主开关
            NotificationMasterSwitch(
                enabled = notificationState.masterNotificationsEnabled,
                onEnabledChange = { viewModel.setMasterNotificationsEnabled(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 预算通知设置
            if (notificationState.masterNotificationsEnabled) {
                NotificationCategoryCard(
                    title = "预算通知",
                    icon = Icons.Default.Warning,
                    enabled = notificationState.budgetNotificationsEnabled,
                    onEnabledChange = { viewModel.setBudgetNotificationsEnabled(it) }
                ) {
                    // 预算警告阈值设置
                    if (notificationState.budgetNotificationsEnabled) {
                        ThresholdSetting(
                            title = "预算提醒阈值",
                            value = notificationState.budgetWarningThreshold,
                            onValueChange = { viewModel.setBudgetWarningThreshold(it) },
                            valueText = "${notificationState.budgetWarningThreshold}%"
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 是否显示超支通知
                        SwitchSetting(
                            title = "预算超支提醒",
                            description = "当预算超支时发送通知",
                            checked = notificationState.budgetExceededNotificationsEnabled,
                            onCheckedChange = { viewModel.setBudgetExceededNotificationsEnabled(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 信用卡还款通知设置
                NotificationCategoryCard(
                    title = "信用卡提醒",
                    icon = Icons.Default.CreditCard,
                    enabled = notificationState.creditCardNotificationsEnabled,
                    onEnabledChange = { viewModel.setCreditCardNotificationsEnabled(it) }
                ) {
                    if (notificationState.creditCardNotificationsEnabled) {
                        // 还款提前天数设置
                        ThresholdSetting(
                            title = "提前提醒天数",
                            value = notificationState.creditCardReminderDays.toFloat(),
                            onValueChange = { viewModel.setCreditCardReminderDays(it.toInt()) },
                            valueText = "${notificationState.creditCardReminderDays}天",
                            valueRange = 1f..10f,
                            steps = 9
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 定期交易通知设置
                NotificationCategoryCard(
                    title = "定期交易提醒",
                    icon = Icons.Default.Wallet,
                    enabled = notificationState.recurringTransactionNotificationsEnabled,
                    onEnabledChange = { viewModel.setRecurringTransactionNotificationsEnabled(it) }
                ) {
                    if (notificationState.recurringTransactionNotificationsEnabled) {
                        // 提前提醒天数
                        ThresholdSetting(
                            title = "提前提醒天数",
                            value = notificationState.recurringTransactionReminderDays.toFloat(),
                            onValueChange = { viewModel.setRecurringTransactionReminderDays(it.toInt()) },
                            valueText = "${notificationState.recurringTransactionReminderDays}天",
                            valueRange = 0f..7f,
                            steps = 7
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 系统通知设置
                NotificationCategoryCard(
                    title = "系统通知",
                    icon = Icons.Default.Notifications,
                    enabled = notificationState.systemNotificationsEnabled,
                    onEnabledChange = { viewModel.setSystemNotificationsEnabled(it) }
                ) {
                    if (notificationState.systemNotificationsEnabled) {
                        // 备份通知
                        SwitchSetting(
                            title = "备份通知",
                            description = "显示备份成功或失败的通知",
                            checked = notificationState.backupNotificationsEnabled,
                            onCheckedChange = { viewModel.setBackupNotificationsEnabled(it) }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // 同步通知
                        SwitchSetting(
                            title = "同步通知",
                            description = "显示数据同步的相关通知",
                            checked = notificationState.syncNotificationsEnabled,
                            onCheckedChange = { viewModel.setSyncNotificationsEnabled(it) }
                        )
                    }
                }
            }

            // 底部间距
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 通知主开关
 */
@Composable
fun NotificationMasterSwitch(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    PrimaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "通知",
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "启用通知",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = if (enabled) "通知已启用" else "通知已禁用",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

/**
 * 通知分类卡片
 */
@Composable
fun NotificationCategoryCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    SecondaryCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            // 分割线
            if (enabled) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // 内容
                content()
            }
        }
    }
}

/**
 * 阈值设置组件
 */
@Composable
fun ThresholdSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float> = 1f..100f,
    steps: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * 开关设置组件
 */
@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}