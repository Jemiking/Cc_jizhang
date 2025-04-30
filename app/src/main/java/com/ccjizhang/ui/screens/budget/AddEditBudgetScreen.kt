package com.ccjizhang.ui.screens.budget

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.data.model.Budget
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.BudgetViewModel
import com.ccjizhang.ui.viewmodels.CategoryViewModel
import com.ccjizhang.ui.viewmodels.CategoryWithIcon
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import androidx.compose.material.ExperimentalMaterialApi
import java.util.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    navController: NavHostController,
    budgetId: Long? = null,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    // 加载数据
    val expenseCategories by categoryViewModel.expenseCategories.collectAsState()
    val selectedBudget by budgetViewModel.selectedBudget.collectAsState()
    val isLoading by budgetViewModel.isLoading.collectAsState()

    // 状态
    var budgetName by remember { mutableStateOf("") }
    var budgetAmount by remember { mutableStateOf("") }
    var budgetPeriod by remember { mutableStateOf("月") }
    var isCategoryBudget by remember { mutableStateOf(false) }
    val selectedCategories = remember { mutableStateListOf<Long>() }

    // 下拉框状态
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // 初始化数据
    LaunchedEffect(key1 = budgetId) {
        categoryViewModel.loadCategories()
        if (budgetId != null) {
            budgetViewModel.loadBudgetDetails(budgetId)
        }
    }

    // 如果是编辑模式，填充现有数据
    LaunchedEffect(selectedBudget) {
        selectedBudget?.let { budget ->
            budgetName = budget.name
            budgetAmount = budget.amount.toString()
            budgetPeriod = budget.period
            isCategoryBudget = budget.categories.isNotEmpty()
            selectedCategories.clear()
            selectedCategories.addAll(budget.categories)
        }
    }

    RoundedTopBarScaffold(
        title = if (budgetId == null) "添加预算" else "编辑预算",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 预算名称
            item {
                OutlinedTextField(
                    value = budgetName,
                    onValueChange = { budgetName = it },
                    label = { Text("预算名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 预算金额
            item {
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("预算金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 预算周期
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = budgetPeriod,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "选择周期"
                                )
                            },
                            label = { Text("预算周期") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            listOf("日", "周", "月", "季", "年").forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period) },
                                    onClick = {
                                        budgetPeriod = period
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 预算类型选择
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "分类预算",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isCategoryBudget,
                        onCheckedChange = { isCategoryBudget = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 分类选择（仅在分类预算模式下显示）
            if (isCategoryBudget) {
                item {
                    Text(
                        text = "选择分类",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 显示分类列表
                items(expenseCategories) { category ->
                    CategorySelectionItem(
                        category = category,
                        isSelected = selectedCategories.contains(category.category.id),
                        onSelectionChanged = { isSelected ->
                            if (isSelected) {
                                if (!selectedCategories.contains(category.category.id)) {
                                    selectedCategories.add(category.category.id)
                                }
                            } else {
                                selectedCategories.remove(category.category.id)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 保存按钮
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // 验证输入
                        val amount = budgetAmount.toDoubleOrNull() ?: 0.0
                        if (budgetName.isBlank() || amount <= 0) {
                            return@Button
                        }

                        // 创建预算对象
                        val budget = Budget(
                            id = budgetId ?: System.currentTimeMillis(),
                            name = budgetName,
                            amount = amount,
                            period = budgetPeriod,
                            categories = if (isCategoryBudget) selectedCategories else listOf(),
                            startDate = Date(), // 使用当前日期作为开始日期
                            endDate = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // 结束日期为30天后
                        )

                        // 保存预算
                        if (budgetId == null) {
                            budgetViewModel.addBudget(budget)
                        } else {
                            budgetViewModel.updateBudget(budget)
                        }

                        // 返回上一页
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = budgetName.isNotBlank() && budgetAmount.toDoubleOrNull() != null &&
                              (budgetAmount.toDoubleOrNull() ?: 0.0) > 0 &&
                              (!isCategoryBudget || selectedCategories.isNotEmpty())
                ) {
                    Text("保存预算")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CategorySelectionItem(
    category: CategoryWithIcon,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

            Spacer(modifier = Modifier.width(12.dp))

            // 分类名称
            Text(
                text = category.category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // 选择框
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelectionChanged(it) }
            )
        }
    }
}