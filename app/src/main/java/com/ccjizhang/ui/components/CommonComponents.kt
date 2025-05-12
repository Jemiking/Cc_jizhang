package com.ccjizhang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.theme.SurfaceLight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.theme.CCJiZhangTheme
import com.ccjizhang.R

/**
 * 圆角顶部栏容器
 */
@Composable
fun RoundedTopBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 获取状态栏高度
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            // 确保背景色完全延伸到状态栏
            .height(56.dp + statusBarHeight)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(PrimaryDark)
    ) {
        // 在内容容器上添加padding等于状态栏高度，确保内容不被状态栏遮挡
        Box(modifier = Modifier.padding(top = statusBarHeight)) {
            content()
        }
    }
}

/**
 * 主顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    customNavigationIcon: @Composable (() -> Unit)? = null
) {
    RoundedTopBar {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SurfaceLight,
                        fontSize = 22.sp // 增大字体大小
                    ),
                    modifier = Modifier.padding(top = 4.dp) // 微调垂直位置
                )
            },
            navigationIcon = {
                if (customNavigationIcon != null) {
                    customNavigationIcon()
                } else if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = SurfaceLight
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = SurfaceLight,
                actionIconContentColor = SurfaceLight
            ),
            modifier = Modifier.height(52.dp) // 减小顶部栏高度
        )
    }
}

/**
 * 卡片容器
 */
@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        content()
    }
}

/**
 * 分类图标项
 */
@Composable
fun CategoryIconItem(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
            color = backgroundColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 预算进度条项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetProgressItem(
    title: String,
    amount: Double,
    maxAmount: Double,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (amount / maxAmount).coerceIn(0.0, 1.0).toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "¥ $amount / ¥ $maxAmount",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "已使用 ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "剩余 ¥ ${maxAmount - amount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 交易记录项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    category: String,
    amount: Double,
    isExpense: Boolean,
    time: String,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBackground.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconBackground,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 交易信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Row {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 金额
            Text(
                text = if (isExpense) "-¥ $amount" else "+¥ $amount",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpense) Color(0xFFE53935) else Color(0xFF43A047)
                )
            )
        }
    }
}

/**
 * 带圆角顶栏的Scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundedTopBarScaffold(
    title: String,
    navController: NavHostController? = null,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    backgroundColor: Color = PrimaryDark,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit) = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    // 确保系统状态栏被考虑在内
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        topBar = { /* 空的顶栏，实际顶栏在内容中处理 */ },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 圆角顶栏
            MainTopAppBar(
                title = title,
                showBackButton = showBackButton,
                onBackClick = {
                    if (onBackClick != null) {
                        onBackClick()
                    } else {
                        navController?.popBackStack()
                    }
                },
                actions = actions,
                customNavigationIcon = navigationIcon
            )

            // 主要内容，考虑状态栏高度
            content(paddingValues)
        }
    }
}

// 添加预览
@Preview(showBackground = true)
@Composable
fun PreviewRoundedTopBar() {
    CCJiZhangTheme {
        RoundedTopBar {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "标题示例",
                    style = MaterialTheme.typography.titleLarge,
                    color = SurfaceLight
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainTopAppBar() {
    CCJiZhangTheme {
        MainTopAppBar(
            title = "应用标题",
            showBackButton = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCardContainer() {
    CCJiZhangTheme {
        CardContainer {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "卡片标题",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "这是卡片的内容示例，展示了CardContainer的外观。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBudgetProgressItem() {
    CCJiZhangTheme {
        BudgetProgressItem(
            title = "食品杂货",
            amount = 1200.0,
            maxAmount = 2000.0,
            progressColor = PrimaryDark
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTransactionItem() {
    CCJiZhangTheme {
        TransactionItem(
            icon = Icons.Default.MoreVert,
            iconBackground = PrimaryDark,
            title = "超市购物",
            category = "食品杂货",
            amount = 326.5,
            isExpense = true,
            time = "昨天 18:15"
        )
    }
}

@Preview(
    name = "圆角顶栏Scaffold预览",
    showBackground = true
)
@Composable
fun PreviewRoundedTopBarScaffold() {
    CCJiZhangTheme {
        RoundedTopBarScaffold(
            title = "示例页面",
            navController = rememberNavController(),
            showBackButton = true
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("这是页面内容示例")
            }
        }
    }
}

/**
 * CC记账应用的顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCJiZhangTopAppBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = actions
    )
}

/**
 * 删除确认对话框
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 加载中内容组件
 */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 空内容组件
 */
@Composable
fun EmptyContent(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    icon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    actionText: String = ""
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (onActionClick != null && actionText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionText)
            }
        }
    }
}

/**
 * 信息行组件
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

/**
 * 日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}