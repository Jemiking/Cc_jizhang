package com.ccjizhang.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Category
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.CategoryViewModel
import com.ccjizhang.ui.viewmodels.CategoryWithIcon
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog

@Composable
fun CategoryManagementScreen(
    navController: NavHostController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    // 加载数据
    val categories by viewModel.categories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val topLevelExpenseCategories by viewModel.topLevelExpenseCategories.collectAsState()
    val topLevelIncomeCategories by viewModel.topLevelIncomeCategories.collectAsState()
    val childCategories by viewModel.childCategories.collectAsState()
    val selectedParentId by viewModel.selectedParentCategoryId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hierarchicalViewMode by viewModel.hierarchicalViewMode.collectAsState()

    // 状态
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryWithIcon?>(null) }
    var showAddChildDialog by remember { mutableStateOf(false) }
    var selectedParentCategory by remember { mutableStateOf<CategoryWithIcon?>(null) }

    // 协程作用域
    val coroutineScope = rememberCoroutineScope()

    // 加载数据
    LaunchedEffect(key1 = true) {
        viewModel.loadCategories()
    }

    RoundedTopBarScaffold(
        title = if (selectedParentId == null) "分类管理" else {
            val parentCategory = categories.find { it.category.id == selectedParentId }
            "子分类 - ${parentCategory?.category?.name ?: ""}"
        },
        navController = navController,
        showBackButton = true,
        actions = {
            // 添加视图切换按钮
            IconButton(onClick = { viewModel.toggleViewMode() }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "切换视图模式",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // 返回上级分类按钮，仅在查看子分类时显示
            if (selectedParentId != null) {
                IconButton(onClick = { viewModel.navigateToParent() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回上级分类",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedParentId == null) {
                // 在顶级分类列表显示添加分类按钮
                FloatingActionButton(
                    onClick = { showAddCategoryDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加分类",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                // 在子分类列表显示添加子分类按钮
                ExtendedFloatingActionButton(
                    onClick = {
                        // 获取父分类并显示添加子分类对话框
                        val parentCategory = categories.find { it.category.id == selectedParentId }
                        if (parentCategory != null) {
                            selectedParentCategory = parentCategory
                            showAddChildDialog = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加子分类"
                        )
                    },
                    text = { Text("添加子分类") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 仅在顶级分类视图显示类型标签页（支出/收入分类）
            if (selectedParentId == null) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("支出分类") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("收入分类") }
                    )
                }
            }

            // 内容
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedParentId == null) {
                    // 顶级分类视图
                    if (hierarchicalViewMode) {
                        // 层级模式
                        when (selectedTabIndex) {
                            0 -> HierarchicalCategoryList(
                                categories = topLevelExpenseCategories,
                                onEditCategory = { categoryToEdit = it },
                                onDeleteCategory = {
                                    coroutineScope.launch {
                                        val hasChildren = viewModel.hasChildCategories(it.category.id)
                                        if (hasChildren) {
                                            // 显示提示：该分类包含子分类，删除将同时删除所有子分类
                                            // 这里简化处理，直接删除
                                            viewModel.deleteCategory(it.category.id)
                                        } else {
                                            viewModel.deleteCategory(it.category.id)
                                        }
                                    }
                                },
                                onSelectCategory = { viewModel.selectParentCategory(it.category.id) }
                            )
                            1 -> HierarchicalCategoryList(
                                categories = topLevelIncomeCategories,
                                onEditCategory = { categoryToEdit = it },
                                onDeleteCategory = {
                                    coroutineScope.launch {
                                        val hasChildren = viewModel.hasChildCategories(it.category.id)
                                        if (hasChildren) {
                                            // 显示提示：该分类包含子分类，删除将同时删除所有子分类
                                            // 这里简化处理，直接删除
                                            viewModel.deleteCategory(it.category.id)
                                        } else {
                                            viewModel.deleteCategory(it.category.id)
                                        }
                                    }
                                },
                                onSelectCategory = { viewModel.selectParentCategory(it.category.id) }
                            )
                        }
                    } else {
                        // 平铺模式（原有模式）
                        when (selectedTabIndex) {
                            0 -> CategoryList(
                                categories = expenseCategories,
                                onEditCategory = { categoryToEdit = it },
                                onDeleteCategory = { viewModel.deleteCategory(it.category.id) }
                            )
                            1 -> CategoryList(
                                categories = incomeCategories,
                                onEditCategory = { categoryToEdit = it },
                                onDeleteCategory = { viewModel.deleteCategory(it.category.id) }
                            )
                        }
                    }
                } else {
                    // 子分类视图
                    ChildCategoryList(
                        categories = childCategories,
                        onEditCategory = { categoryToEdit = it },
                        onDeleteCategory = {
                            coroutineScope.launch {
                                val hasChildren = viewModel.hasChildCategories(it.category.id)
                                if (hasChildren) {
                                    // 显示提示：该分类包含子分类，删除将同时删除所有子分类
                                    // 这里简化处理，直接删除
                                    viewModel.deleteCategory(it.category.id)
                                } else {
                                    viewModel.deleteCategory(it.category.id)
                                }
                            }
                        },
                        onSelectCategory = { viewModel.selectParentCategory(it.category.id) }
                    )
                }
            }
        }

        // 添加分类对话框
        if (showAddCategoryDialog) {
            CategoryEditDialog(
                categoryWithIcon = null,
                isIncome = selectedTabIndex == 1,
                availableIcons = viewModel.getAvailableIcons(),
                onDismiss = { showAddCategoryDialog = false },
                onSave = { name, icon, color, isIncome ->
                    val newCategory = Category(
                        name = name,
                        isIncome = isIncome
                    )
                    viewModel.addCategory(newCategory, icon, color)
                    showAddCategoryDialog = false
                }
            )
        }

        // 添加子分类对话框
        if (showAddChildDialog && selectedParentCategory != null) {
            CategoryEditDialog(
                categoryWithIcon = null,
                isIncome = selectedParentCategory!!.category.isIncome,
                availableIcons = viewModel.getAvailableIcons(),
                onDismiss = { showAddChildDialog = false },
                onSave = { name, icon, color, isIncome ->
                    val childCategory = Category(
                        name = name,
                        isIncome = selectedParentCategory!!.category.isIncome
                    )
                    viewModel.addChildCategory(
                        selectedParentCategory!!.category.id,
                        childCategory,
                        icon,
                        color
                    )
                    showAddChildDialog = false
                },
                isChildCategory = true,
                parentCategoryName = selectedParentCategory!!.category.name
            )
        }

        // 编辑分类对话框
        categoryToEdit?.let { category ->
            CategoryEditDialog(
                categoryWithIcon = category,
                isIncome = category.category.isIncome,
                availableIcons = viewModel.getAvailableIcons(),
                onDismiss = { categoryToEdit = null },
                onSave = { name, icon, color, isIncome ->
                    val updatedCategory = category.category.copy(
                        name = name,
                        isIncome = isIncome
                    )
                    viewModel.updateCategory(updatedCategory, icon, color)
                    categoryToEdit = null
                }
            )
        }
    }
}

@Composable
fun CategoryList(
    categories: List<CategoryWithIcon>,
    onEditCategory: (CategoryWithIcon) -> Unit,
    onDeleteCategory: (CategoryWithIcon) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("没有分类，请点击添加按钮创建分类")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { onEditCategory(category) },
                    onDelete = { onDeleteCategory(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 底部空间，避免FAB遮挡
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun HierarchicalCategoryList(
    categories: List<CategoryWithIcon>,
    onEditCategory: (CategoryWithIcon) -> Unit,
    onDeleteCategory: (CategoryWithIcon) -> Unit,
    onSelectCategory: (CategoryWithIcon) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("没有分类，请点击添加按钮创建分类")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(categories) { category ->
                HierarchicalCategoryItem(
                    category = category,
                    onEdit = { onEditCategory(category) },
                    onDelete = { onDeleteCategory(category) },
                    onSelect = { onSelectCategory(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 底部空间，避免FAB遮挡
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ChildCategoryList(
    categories: List<CategoryWithIcon>,
    onEditCategory: (CategoryWithIcon) -> Unit,
    onDeleteCategory: (CategoryWithIcon) -> Unit,
    onSelectCategory: (CategoryWithIcon) -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("没有子分类，请点击添加按钮创建子分类")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(categories) { category ->
                HierarchicalCategoryItem(
                    category = category,
                    onEdit = { onEditCategory(category) },
                    onDelete = { onDeleteCategory(category) },
                    onSelect = { onSelectCategory(category) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 底部空间，避免FAB遮挡
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun CategoryItem(
    category: CategoryWithIcon,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = category.color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.category.name,
                    tint = category.color,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 名称
            Text(
                text = category.category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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

@Composable
fun HierarchicalCategoryItem(
    category: CategoryWithIcon,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = category.color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.category.name,
                    tint = category.color,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 名称
            Text(
                text = category.category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 进入子分类按钮
            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看子分类",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    categoryWithIcon: CategoryWithIcon?,
    isIncome: Boolean,
    availableIcons: List<Pair<ImageVector, Color>>,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: ImageVector, color: Color, isIncome: Boolean) -> Unit,
    isChildCategory: Boolean = false,
    parentCategoryName: String = ""
) {
    val isEditMode = categoryWithIcon != null

    var categoryName by remember { mutableStateOf(categoryWithIcon?.category?.name ?: "") }
    var categoryIsIncome by remember { mutableStateOf(isIncome) }
    var selectedIconIndex by remember {
        mutableStateOf(
            if (isEditMode) {
                availableIcons.indexOfFirst { it.first == categoryWithIcon!!.icon }
            } else {
                0
            }
        )
    }

    if (selectedIconIndex < 0) selectedIconIndex = 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // 标题
                Text(
                    text = when {
                        isEditMode -> "编辑分类"
                        isChildCategory -> "添加子分类"
                        else -> "添加分类"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // 如果是子分类，显示父分类名称
                if (isChildCategory && parentCategoryName.isNotEmpty()) {
                    Text(
                        text = "父分类: $parentCategoryName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 分类名称输入
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 分类类型选择 - 只有在非子分类模式下才允许选择类型
                if (!isChildCategory) {
                    Text("分类类型", style = MaterialTheme.typography.bodyMedium)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !categoryIsIncome,
                            onClick = { categoryIsIncome = false }
                        )
                        Text("支出", modifier = Modifier.clickable { categoryIsIncome = false })

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = categoryIsIncome,
                            onClick = { categoryIsIncome = true }
                        )
                        Text("收入", modifier = Modifier.clickable { categoryIsIncome = true })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 图标选择
                Text("选择图标", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(availableIcons.indices.toList()) { index ->
                        val (icon, color) = availableIcons[index]
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == selectedIconIndex)
                                        color.copy(alpha = 0.2f)
                                    else
                                        Color.Transparent
                                )
                                .border(
                                    width = if (index == selectedIconIndex) 2.dp else 1.dp,
                                    color = if (index == selectedIconIndex) color else Color.Gray,
                                    shape = CircleShape
                                )
                                .selectable(
                                    selected = index == selectedIconIndex,
                                    onClick = { selectedIconIndex = index }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "图标",
                                tint = color
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (categoryName.isNotBlank() && selectedIconIndex in availableIcons.indices) {
                                val (icon, color) = availableIcons[selectedIconIndex]
                                onSave(categoryName, icon, color, categoryIsIncome)
                            }
                        },
                        enabled = categoryName.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}