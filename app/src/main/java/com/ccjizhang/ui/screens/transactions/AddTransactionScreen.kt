package com.ccjizhang.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.TransactionType
import com.ccjizhang.ui.components.MainTopAppBar
import com.ccjizhang.ui.theme.ExpenseRed
import com.ccjizhang.ui.theme.IncomeGreen
import com.ccjizhang.ui.theme.PrimaryDark
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.ccjizhang.ui.theme.CCJiZhangTheme

@Composable
fun AddTransactionScreen(
    navController: NavHostController,
    transactionId: Long = 0L,
    isEditing: Boolean = false,
    onTransactionAdded: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = if (isEditing) "编辑交易" else "添加交易",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 保存交易记录
                    onTransactionAdded()
                    navController.popBackStack()
                },
                containerColor = PrimaryDark,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "保存"
                )
            }
        }
    ) { paddingValues ->
        TransactionForm(
            modifier = Modifier.padding(paddingValues),
            transactionId = transactionId,
            isEditing = isEditing
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    modifier: Modifier = Modifier,
    transactionId: Long = 0L,
    isEditing: Boolean = false
) {
    var transactionType by remember { mutableStateOf("expense") }
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2023年5月15日") }
    var account by remember { mutableStateOf("现金") }
    var description by remember { mutableStateOf("") }
    
    // 如果是编辑模式，尝试加载现有交易数据
    LaunchedEffect(transactionId) {
        if (isEditing && transactionId > 0) {
            // 这里应该从ViewModel加载交易数据
            // 示例数据
            transactionType = "expense"
            amount = "55.0"
            title = "午餐"
            category = "餐饮"
            date = "2023年5月15日"
            account = "工商银行"
            description = "公司附近的快餐店"
        }
    }
    
    // 下拉菜单状态
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    
    // 模拟分类列表
    val categories = listOf("餐饮", "购物", "交通", "住房", "娱乐", "医疗", "教育", "其他")
    // 模拟账户列表
    val accounts = listOf("现金", "支付宝", "微信", "工商银行", "招商银行", "信用卡")
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 交易类型选择
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 支出选项
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(
                            selected = transactionType == "expense",
                            onClick = { transactionType = "expense" }
                        )
                        .background(
                            if (transactionType == "expense") ExpenseRed.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = transactionType == "expense",
                        onClick = { transactionType = "expense" },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "支出",
                        color = if (transactionType == "expense") ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 收入选项
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(
                            selected = transactionType == "income",
                            onClick = { transactionType = "income" }
                        )
                        .background(
                            if (transactionType == "income") IncomeGreen.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = transactionType == "income",
                        onClick = { transactionType = "income" },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "收入",
                        color = if (transactionType == "income") IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // 金额输入
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("金额") },
            prefix = { Text("¥") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = if (transactionType == "expense") ExpenseRed else IncomeGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        
        // 标题输入
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("标题") },
            shape = RoundedCornerShape(16.dp)
        )
        
        // 分类选择
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("分类") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "选择分类",
                        modifier = Modifier.clickable { showCategoryDropdown = true }
                    )
                },
                readOnly = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            
            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                categories.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            category = item
                            showCategoryDropdown = false
                        }
                    )
                }
            }
        }
        
        // 日期选择
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("日期") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "选择日期"
                )
            },
            readOnly = true,
            shape = RoundedCornerShape(16.dp)
        )
        
        // 账户选择
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账户") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "选择账户",
                        modifier = Modifier.clickable { showAccountDropdown = true }
                    )
                },
                readOnly = true,
                shape = RoundedCornerShape(16.dp)
            )
            
            DropdownMenu(
                expanded = showAccountDropdown,
                onDismissRequest = { showAccountDropdown = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                accounts.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            account = item
                            showAccountDropdown = false
                        }
                    )
                }
            }
        }
        
        // 备注输入
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备注") },
            shape = RoundedCornerShape(16.dp),
            minLines = 3
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 快速分类选择
        Text(
            text = "常用分类",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CategoryIcon(
                icon = Icons.Default.LocalDining,
                title = "餐饮",
                isSelected = category == "餐饮",
                onClick = { category = "餐饮" }
            )
            
            CategoryIcon(
                icon = Icons.Default.ShoppingCart,
                title = "购物",
                isSelected = category == "购物",
                onClick = { category = "购物" }
            )
            
            CategoryIcon(
                icon = Icons.Default.Home,
                title = "住房",
                isSelected = category == "住房",
                onClick = { category = "住房" }
            )
            
            CategoryIcon(
                icon = Icons.Default.Wallet,
                title = "交通",
                isSelected = category == "交通",
                onClick = { category = "交通" }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // 为FloatingActionButton留出空间
    }
}

@Composable
fun CategoryIcon(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) PrimaryDark else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) PrimaryDark else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) PrimaryDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

// 添加预览
@Preview(
    name = "添加交易页面预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewAddTransactionScreen() {
    CCJiZhangTheme {
        AddTransactionScreen(navController = rememberNavController())
    }
}

@Preview(
    name = "编辑交易页面预览",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun PreviewEditTransactionScreen() {
    CCJiZhangTheme {
        AddTransactionScreen(
            navController = rememberNavController(),
            transactionId = 1L,
            isEditing = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTransactionForm() {
    CCJiZhangTheme {
        TransactionForm()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCategorySelector() {
    // 注释掉或删除这个预览函数，因为CategorySelector可能不存在
} 