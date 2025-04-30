package com.ccjizhang.ui.screens.transactions

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.ui.components.MainTopAppBar
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.theme.ExpenseRed
import com.ccjizhang.ui.theme.IncomeGreen
import com.ccjizhang.ui.theme.PrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.components.CategoryIcon
import com.ccjizhang.ui.theme.CCJiZhangTheme
import com.ccjizhang.ui.viewmodels.TransactionViewModel

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    navController: NavHostController,
    onNavigateBack: () -> Unit = { navController.popBackStack() },
    onNavigateToEdit: (Long) -> Unit = { id -> navController.navigate(NavRoutes.transactionEdit(id)) },
    onDeleteSuccess: () -> Unit = { navController.popBackStack() }, 
    viewModel: TransactionViewModel = hiltViewModel()
) {
    // 加载交易数据
    LaunchedEffect(transactionId) {
        viewModel.loadTransactionDetails(transactionId)
    }
    
    // 收集状态
    val transaction = viewModel.selectedTransaction.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    
    // 删除确认对话框状态
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "交易详情",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (transaction != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showDeleteConfirmDialog = true },
                        containerColor = ExpenseRed,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除"
                        )
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            onNavigateToEdit(transactionId)
                        },
                        containerColor = PrimaryDark,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (transaction == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到交易记录")
            }
        } else {
            TransactionDetailContent(
                transaction = transaction,
                paddingValues = paddingValues,
                onBackClick = onNavigateBack
            )
        }
        
        // 删除确认对话框
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除这条交易记录吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            transaction?.id?.let { id ->
                                viewModel.deleteTransaction(id)
                                showDeleteConfirmDialog = false
                                onDeleteSuccess()
                            }
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onBackClick: () -> Unit
) {
    // 格式化日期
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(transaction.date)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 金额和类别卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (!transaction.isIncome) ExpenseRed else IncomeGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 分类图标和名称行将在后续实现
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryIcon(
                        categoryId = transaction.categoryId ?: 0L,
                        tint = Color.White,
                        backgroundColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "交易类别", // 在实际实现中应该从分类仓库获取名称
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (!transaction.isIncome) "-¥${transaction.amount}" else "+¥${transaction.amount}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = transaction.note.ifEmpty { "无备注" },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // 详细信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DetailItem(label = "日期和时间", value = formattedDate)
                DetailItem(label = "账户", value = "账户名称") // 从账户仓库获取名称
                DetailItem(label = "交易类型", value = if (transaction.isIncome) "收入" else "支出")
                
                if (transaction.note.isNotEmpty()) {
                    DetailItem(label = "备注", value = transaction.note)
                }
                
                // 位置信息（如果有）
                if (transaction.location.isNotEmpty()) {
                    DetailItem(
                        label = "位置信息",
                        value = transaction.location,
                        icon = Icons.Default.LocationOn
                    )
                }
                
                // 图片附件（如果有）
                if (transaction.imageUri.isNotEmpty()) {
                    DetailItem(
                        label = "附件",
                        value = "",
                        isLast = true,
                        icon = Icons.Default.Image,
                        content = {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(transaction.imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "交易附件",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "返回", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    isLast: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (content != null) {
            content()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        if (!isLast) {
            Divider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// 预览
@Preview(showBackground = true)
@Composable
fun DetailItemPreview() {
    CCJiZhangTheme {
        DetailItem(
            label = "日期和时间",
            value = "2023年5月15日 12:30"
        )
    }
} 