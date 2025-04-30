package com.ccjizhang.ui.screens.savinggoal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ccjizhang.data.model.SavingGoal
import com.ccjizhang.ui.viewmodels.SavingGoalTab
import com.ccjizhang.ui.viewmodels.SavingGoalViewModel
import com.ccjizhang.util.DateUtils
import com.ccjizhang.util.formatCurrency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavHostController

/**
 * 储蓄目标主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onAddSavingGoal: () -> Unit,
    onNavigateToSavingGoalDetail: (Long) -> Unit,
    viewModel: SavingGoalViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val savingGoals by viewModel.currentTabGoals.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("储蓄目标") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSavingGoal,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加储蓄目标")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp
            ) {
                SavingGoalTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { 
                            coroutineScope.launch {
                                viewModel.selectTab(tab)
                            }
                        },
                        text = {
                            Text(
                                when (tab) {
                                    SavingGoalTab.ACTIVE -> "进行中"
                                    SavingGoalTab.COMPLETED -> "已完成"
                                    SavingGoalTab.EXPIRED -> "已过期"
                                    SavingGoalTab.ALL -> "全部"
                                }
                            )
                        }
                    )
                }
            }
            
            if (savingGoals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "暂无储蓄目标",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "点击右下角的加号按钮创建您的第一个储蓄目标",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(onClick = onAddSavingGoal) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加储蓄目标")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(savingGoals) { goal ->
                        SavingGoalItem(
                            goal = goal,
                            onClick = { onNavigateToSavingGoalDetail(goal.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * 单个储蓄目标项
 */
@Composable
fun SavingGoalItem(
    goal: SavingGoal,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val progress = goal.currentAmount / goal.targetAmount
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val isCompleted = goal.currentAmount >= goal.targetAmount
    val isExpired = !isCompleted && goal.targetDate.before(Date())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 图标或颜色圆形
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(goal.color)),
                    contentAlignment = Alignment.Center
                ) {
                    if (goal.iconUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(goal.iconUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = goal.name,
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 状态图标
                        when {
                            isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = "已完成",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            isExpired -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "已过期",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.WatchLater,
                                    contentDescription = "进行中",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 进度百分比显示
                    Text(
                        text = "进度: ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = progress.toFloat().coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isExpired -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.tertiary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 当前金额/目标金额
                Text(
                    text = "${goal.currentAmount.formatCurrency()} / ${goal.targetAmount.formatCurrency()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 目标日期
                Text(
                    text = dateFormat.format(goal.targetDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val statusText = when {
                    isCompleted -> "已完成"
                    isExpired -> "已过期"
                    DateUtils.isDatesClose(Date(), goal.targetDate, 7) -> "即将到期"
                    else -> "进行中"
                }
                
                val statusColor = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isExpired -> MaterialTheme.colorScheme.error
                    DateUtils.isDatesClose(Date(), goal.targetDate, 7) -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
} 