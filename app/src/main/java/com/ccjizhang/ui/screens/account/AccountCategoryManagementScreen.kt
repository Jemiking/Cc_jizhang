package com.ccjizhang.ui.screens.account

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.ui.common.ColorPickerDialog
import com.ccjizhang.ui.common.IconPickerDialog
import com.ccjizhang.ui.common.LoadingIndicator
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.AccountCategoryViewModel

/**
 * 账户分类管理页面
 */
@Composable
fun AccountCategoryManagementScreen(
    navController: NavHostController,
    viewModel: AccountCategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 对话框状态
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<AccountCategory?>(null) }

    RoundedTopBarScaffold(
        title = "账户分类管理",
        navController = navController,
        actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加分类")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        CategoryItem(
                            category = category,
                            onEdit = {
                                selectedCategory = category
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedCategory = category
                                showDeleteDialog = true
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // 错误消息
            errorMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearErrorMessage() },
                    title = { Text("错误") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("确定")
                        }
                    }
                )
            }

            // 添加分类对话框
            if (showAddDialog) {
                CategoryDialog(
                    category = null,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, icon, color, isDefault ->
                        viewModel.addCategory(name, icon, color, isDefault)
                        showAddDialog = false
                    }
                )
            }

            // 编辑分类对话框
            if (showEditDialog && selectedCategory != null) {
                CategoryDialog(
                    category = selectedCategory,
                    onDismiss = { showEditDialog = false },
                    onConfirm = { name, icon, color, isDefault ->
                        selectedCategory?.let { category ->
                            viewModel.updateCategory(
                                category.copy(
                                    name = name,
                                    icon = icon,
                                    color = color,
                                    isDefault = isDefault
                                )
                            )
                        }
                        showEditDialog = false
                    }
                )
            }

            // 删除确认对话框
            if (showDeleteDialog && selectedCategory != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("删除分类") },
                    text = { Text("确定要删除分类'${selectedCategory?.name}'吗？该分类下的账户将变为未分类。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                selectedCategory?.let { viewModel.deleteCategory(it) }
                                showDeleteDialog = false
                            }
                        ) {
                            Text("删除")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

/**
 * 分类项目
 */
@Composable
fun CategoryItem(
    category: AccountCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(category.color))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.icon),
                    contentDescription = category.name,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 分类名称
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (category.isDefault) {
                    Text(
                        text = "默认分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 编辑按钮
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 分类编辑对话框
 */
@Composable
fun CategoryDialog(
    category: AccountCategory?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: Int, isDefault: Boolean) -> Unit
) {
    val isEdit = category != null

    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "account_balance_wallet") }
    val primaryColor = MaterialTheme.colorScheme.primary.value.toInt()
    var color by remember { mutableStateOf(category?.color ?: primaryColor) }
    var isDefault by remember { mutableStateOf(category?.isDefault ?: false) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑分类" else "添加分类") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 分类名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 图标选择
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIconPicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("图标：", modifier = Modifier.width(60.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconByName(icon),
                            contentDescription = "分类图标",
                            tint = Color.White
                        )
                    }
                }

                // 颜色选择
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorPicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("颜色：", modifier = Modifier.width(60.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                    )
                }

                // 默认分类选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )

                    Text("设为默认分类")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, icon, color, isDefault)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (isEdit) "保存" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 图标选择器对话框
    if (showIconPicker) {
        IconPickerDialog(
            onDismiss = { showIconPicker = false },
            onIconSelected = {
                icon = it
                showIconPicker = false
            }
        )
    }

    // 颜色选择器对话框
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = Color(color),
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                color = it.value.toInt()
                showColorPicker = false
            }
        )
    }
}

/**
 * 根据图标名称获取图标
 */
@Composable
fun getIconByName(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "account_balance" -> Icons.Default.AccountBalance
        "credit_card" -> Icons.Default.CreditCard
        "payment" -> Icons.Default.Payment
        else -> Icons.Default.AccountBalanceWallet
    }
}
