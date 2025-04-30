package com.ccjizhang.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.CategorySuggestionList
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.CategoryViewModel

/**
 * 添加/编辑交易屏幕
 */
@Composable
fun AddEditTransactionScreen(
    navController: NavHostController,
    transactionId: Long? = null,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    // 分类相关
    val suggestedCategories by categoryViewModel.suggestedCategories.collectAsState()
    
    // 表单状态
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    
    // 加载数据
    LaunchedEffect(Unit) {
        // 加载分类
        categoryViewModel.loadCategories()
    }
    
    // 监听笔记变化，触发分类建议
    LaunchedEffect(note, isIncome) {
        if (note.isNotBlank()) {
            categoryViewModel.getSuggestedCategoriesForDescription(note, isIncome)
        } else {
            categoryViewModel.clearSuggestions()
        }
    }
    
    // 监听金额变化，触发分类建议
    LaunchedEffect(amount, isIncome) {
        val amountValue = amount.toDoubleOrNull()
        if (amountValue != null && amountValue > 0) {
            categoryViewModel.getSuggestedCategoriesForAmount(amountValue, isIncome)
        }
    }

    RoundedTopBarScaffold(
        title = if (transactionId == null) "添加交易" else "编辑交易",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 交易类型切换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { isIncome = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isIncome) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("支出")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { isIncome = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isIncome) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("收入")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 金额输入
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 备注输入
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类建议
            if (suggestedCategories.isNotEmpty()) {
                CategorySuggestionList(
                    suggestedCategories = suggestedCategories,
                    mapCategoryToIcon = { category ->
                        val icon = Icons.Default.Category
                        val color = Color(category.color)
                        Pair(icon, color)
                    },
                    onCategorySelected = { category ->
                        // 选择分类的回调
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
} 