package com.ccjizhang.ui.screens.savinggoal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ccjizhang.data.model.SavingGoal
import com.ccjizhang.ui.viewmodels.SavingGoalViewModel
import com.ccjizhang.util.DateUtils
import com.ccjizhang.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * 储蓄目标详情界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalDetailScreen(
    goalId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: SavingGoalViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    
    // 加载目标详情
    LaunchedEffect(goalId) {
        viewModel.selectGoal(goalId)
    }
    
    val goalDetails by viewModel.selectedGoalDetails.collectAsState(initial = null)
    
    // 对话框状态
    val showDeleteConfirmDialog = remember { mutableStateOf(false) }
    val showDepositDialog = remember { mutableStateOf(false) }
    val showWithdrawDialog = remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("储蓄目标详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(goalId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog.value = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        },
        floatingActionButton = {
            goalDetails?.let { details ->
                if (!details.isCompleted && !details.isExpired) {
                    ExtendedFloatingActionButton(
                        onClick = { showDepositDialog.value = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("存入资金") },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        goalDetails?.let { details ->
            val goal = details.goal
            val account = details.account
            val progress = details.progress
            val timeProgress = details.timeProgress
            val isCompleted = details.isCompleted
            val isExpired = details.isExpired
            
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
            val remainingDays = if (!isCompleted && !isExpired) {
                DateUtils.daysBetween(Date(), goal.targetDate)
            } else {
                0
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 头部卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 图标或颜色圆圈
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(goal.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (goal.iconUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(goal.iconUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = goal.name,
                                    modifier = Modifier.size(48.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 目标名称
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 状态标签
                        val (statusText, statusColor) = when {
                            isCompleted -> Pair("已完成", MaterialTheme.colorScheme.primary)
                            isExpired -> Pair("已过期", MaterialTheme.colorScheme.error)
                            DateUtils.isDatesClose(Date(), goal.targetDate, 7) -> Pair("即将到期", MaterialTheme.colorScheme.tertiary)
                            else -> Pair("进行中", MaterialTheme.colorScheme.secondary)
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = statusColor.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 进度显示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "当前金额",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = goal.currentAmount.formatCurrency(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "目标金额",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = goal.targetAmount.formatCurrency(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 进度条和百分比
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                color = when {
                                    isCompleted -> MaterialTheme.colorScheme.primary
                                    isExpired -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.tertiary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "完成度: ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 详细信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "详细信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 关联账户
                        InfoItem(
                            icon = Icons.Default.AccountBalance,
                            label = "关联账户",
                            value = account?.name ?: "未关联账户"
                        )
                        
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                        
                        // 开始日期和目标日期
                        InfoItem(
                            icon = Icons.Default.CalendarToday,
                            label = "开始日期",
                            value = dateFormat.format(goal.startDate)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        InfoItem(
                            icon = Icons.Default.Event,
                            label = "目标日期",
                            value = dateFormat.format(goal.targetDate)
                        )
                        
                        if (!isCompleted && !isExpired) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            InfoItem(
                                icon = Icons.Default.HourglassBottom,
                                label = "剩余时间",
                                value = "$remainingDays 天"
                            )
                        }
                        
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                        
                        // 自动存款信息
                        if (goal.autoSaveAmount != null && goal.autoSaveFrequencyDays != null) {
                            InfoItem(
                                icon = Icons.Default.Autorenew,
                                label = "自动存款",
                                value = "${goal.autoSaveAmount.formatCurrency()} / ${goal.autoSaveFrequencyDays}天"
                            )
                            
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                        
                        // 备注
                        if (!goal.note.isNullOrBlank()) {
                            Text(
                                text = "备注",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = goal.note,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (!isCompleted) {
                        Button(
                            onClick = { showDepositDialog.value = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("存入资金")
                        }
                    }
                    
                    if (goal.currentAmount > 0) {
                        Button(
                            onClick = { showWithdrawDialog.value = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCompleted) "取出资金" else "提前取出")
                        }
                    }
                }
                
                // 底部空间，避免FAB遮挡
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            // 删除确认对话框
            if (showDeleteConfirmDialog.value) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog.value = false },
                    title = { Text("删除目标") },
                    text = { Text("确定要删除\"${goal.name}\"这个储蓄目标吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteSavingGoal(goal)
                                    showDeleteConfirmDialog.value = false
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog.value = false }) {
                            Text("取消")
                        }
                    }
                )
            }
            
            // 存入资金对话框
            if (showDepositDialog.value) {
                TransactionDialog(
                    title = "存入资金",
                    maxAmount = Double.MAX_VALUE,
                    onConfirm = { amount ->
                        coroutineScope.launch {
                            viewModel.depositToGoal(goalId, amount)
                            showDepositDialog.value = false
                        }
                    },
                    onDismiss = { showDepositDialog.value = false }
                )
            }
            
            // 取出资金对话框
            if (showWithdrawDialog.value) {
                TransactionDialog(
                    title = "取出资金",
                    maxAmount = goal.currentAmount,
                    onConfirm = { amount ->
                        coroutineScope.launch {
                            viewModel.withdrawFromGoal(goalId, amount)
                            showWithdrawDialog.value = false
                        }
                    },
                    onDismiss = { showWithdrawDialog.value = false }
                )
            }
        } ?: run {
            // 加载中或找不到目标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * 信息项组件
 */
@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 交易对话框
 */
@Composable
fun TransactionDialog(
    title: String,
    maxAmount: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    fun validateAmount(): Boolean {
        val amount = amountText.toDoubleOrNull()
        return when {
            amountText.isBlank() -> {
                isError = true
                errorMessage = "请输入金额"
                false
            }
            amount == null -> {
                isError = true
                errorMessage = "请输入有效金额"
                false
            }
            amount <= 0 -> {
                isError = true
                errorMessage = "金额必须大于零"
                false
            }
            amount > maxAmount -> {
                isError = true
                errorMessage = "金额不能超过${maxAmount.formatCurrency()}"
                false
            }
            else -> {
                isError = false
                errorMessage = ""
                true
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("金额") },
                    isError = isError,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("¥", style = MaterialTheme.typography.bodyLarge) }
                )
                
                if (isError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                
                if (maxAmount < Double.MAX_VALUE) {
                    Text(
                        text = "最大可用金额: ${maxAmount.formatCurrency()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validateAmount()) {
                        onConfirm(amountText.toDoubleOrNull() ?: 0.0)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 