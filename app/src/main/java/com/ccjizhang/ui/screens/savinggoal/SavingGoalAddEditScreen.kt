package com.ccjizhang.ui.screens.savinggoal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ccjizhang.R
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.SavingGoal
import com.ccjizhang.ui.common.ColorPickerDialog
import com.ccjizhang.ui.common.DatePickerDialog
import com.ccjizhang.ui.common.IconPickerDialog
import com.ccjizhang.ui.common.TextFieldWithErrorState
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.viewmodels.SavingGoalViewModel
import com.ccjizhang.util.DateUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavHostController

/**
 * 储蓄目标添加和编辑界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingGoalAddEditScreen(
    navController: NavHostController,
    goalId: Long? = null,
    viewModel: SavingGoalViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 账户列表
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    
    // 编辑模式下加载现有目标
    val existingGoal = remember { mutableStateOf<SavingGoal?>(null) }
    
    LaunchedEffect(goalId) {
        if (goalId != null) {
            viewModel.selectGoal(goalId)
            viewModel.selectedGoalDetails.collect { details ->
                if (details != null) {
                    existingGoal.value = details.goal
                }
            }
        }
    }
    
    // 表单状态
    val name = remember { mutableStateOf(existingGoal.value?.name ?: "") }
    val nameError = remember { mutableStateOf<String?>(null) }
    val targetAmount = remember { mutableStateOf(existingGoal.value?.targetAmount?.toString() ?: "") }
    val targetAmountError = remember { mutableStateOf<String?>(null) }
    val currentAmount = remember { mutableStateOf(existingGoal.value?.currentAmount?.toString() ?: "0.0") }
    val selectedAccount = remember { mutableStateOf(existingGoal.value?.accountId) }
    val startDate = remember { mutableStateOf(existingGoal.value?.startDate ?: Date()) }
    val targetDate = remember { mutableStateOf(existingGoal.value?.targetDate ?: Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) }
    val targetDateError = remember { mutableStateOf<String?>(null) }
    val priority = remember { mutableStateOf(existingGoal.value?.priority ?: 2) }
    val iconUri = remember { mutableStateOf(existingGoal.value?.iconUri) }
    val color = remember { mutableStateOf(existingGoal.value?.color ?: Color.Blue.toArgb()) }
    val note = remember { mutableStateOf(existingGoal.value?.note ?: "") }
    val autoSaveAmount = remember { mutableStateOf(existingGoal.value?.autoSaveAmount?.toString() ?: "") }
    val autoSaveFrequencyDays = remember { mutableStateOf(existingGoal.value?.autoSaveFrequencyDays?.toString() ?: "") }
    
    // 对话框状态
    val showStartDatePicker = remember { mutableStateOf(false) }
    val showTargetDatePicker = remember { mutableStateOf(false) }
    val showAccountPicker = remember { mutableStateOf(false) }
    val showColorPicker = remember { mutableStateOf(false) }
    val showIconPicker = remember { mutableStateOf(false) }
    
    // 表单验证
    fun validateForm(): Boolean {
        var isValid = true
        
        if (name.value.isBlank()) {
            nameError.value = "目标名称不能为空"
            isValid = false
        } else {
            nameError.value = null
        }
        
        try {
            val amount = targetAmount.value.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                targetAmountError.value = "请输入有效的目标金额"
                isValid = false
            } else {
                targetAmountError.value = null
            }
        } catch (e: Exception) {
            targetAmountError.value = "请输入有效的目标金额"
            isValid = false
        }
        
        if (startDate.value.after(targetDate.value)) {
            targetDateError.value = "目标日期必须晚于开始日期"
            isValid = false
        } else {
            targetDateError.value = null
        }
        
        return isValid
    }
    
    // 保存目标
    fun saveGoal() {
        if (!validateForm()) return
        
        coroutineScope.launch {
            try {
                val targetAmountValue = targetAmount.value.toDoubleOrNull() ?: 0.0
                val currentAmountValue = currentAmount.value.toDoubleOrNull() ?: 0.0
                val autoSaveAmountValue = autoSaveAmount.value.toDoubleOrNull()
                val autoSaveFrequencyDaysValue = autoSaveFrequencyDays.value.toIntOrNull()
                
                if (existingGoal.value != null) {
                    // 更新现有目标
                    val updatedGoal = existingGoal.value!!.copy(
                        name = name.value,
                        targetAmount = targetAmountValue,
                        currentAmount = currentAmountValue,
                        accountId = selectedAccount.value,
                        startDate = startDate.value,
                        targetDate = targetDate.value,
                        priority = priority.value,
                        iconUri = iconUri.value,
                        color = color.value,
                        note = note.value.takeIf { it.isNotBlank() },
                        autoSaveAmount = autoSaveAmountValue,
                        autoSaveFrequencyDays = autoSaveFrequencyDaysValue
                    )
                    viewModel.updateSavingGoal(updatedGoal)
                } else {
                    // 添加新目标
                    viewModel.addSavingGoal(
                        name = name.value,
                        targetAmount = targetAmountValue,
                        accountId = selectedAccount.value,
                        startDate = startDate.value,
                        targetDate = targetDate.value,
                        priority = priority.value,
                        iconUri = iconUri.value,
                        color = color.value,
                        note = note.value.takeIf { it.isNotBlank() },
                        autoSaveAmount = autoSaveAmountValue,
                        autoSaveFrequencyDays = autoSaveFrequencyDaysValue
                    )
                }
                
                navController.popBackStack()
            } catch (e: Exception) {
                // 处理可能的错误
            }
        }
    }
    
    RoundedTopBarScaffold(
        title = if (goalId == null) "新建储蓄目标" else "编辑储蓄目标",
        navController = navController,
        showBackButton = true,
        actions = {
             IconButton(onClick = { saveGoal() }) {
                 Icon(Icons.Default.Check, contentDescription = "保存")
             }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 图标和颜色选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(color.value))
                        .clickable { showColorPicker.value = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (iconUri.value != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(iconUri.value)
                                .crossfade(true)
                                .build(),
                            contentDescription = "目标图标",
                            modifier = Modifier
                                .size(50.dp)
                                .clickable { showIconPicker.value = true },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        IconButton(onClick = { showIconPicker.value = true }) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = "选择图标",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 目标名称
            TextFieldWithErrorState(
                value = name.value,
                onValueChange = { name.value = it },
                label = "目标名称",
                error = nameError.value,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 目标金额
            TextFieldWithErrorState(
                value = targetAmount.value,
                onValueChange = { targetAmount.value = it },
                label = "目标金额",
                error = targetAmountError.value,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("¥", style = MaterialTheme.typography.bodyLarge) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 当前金额 (编辑模式下)
            if (existingGoal.value != null) {
                OutlinedTextField(
                    value = currentAmount.value,
                    onValueChange = { currentAmount.value = it },
                    label = { Text("当前金额") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("¥", style = MaterialTheme.typography.bodyLarge) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 关联账户
            OutlinedTextField(
                value = accounts.find { it.id == selectedAccount.value }?.name ?: "未选择账户",
                onValueChange = { },
                label = { Text("关联账户") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAccountPicker.value = true },
                enabled = false,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 开始日期和目标日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.value),
                    onValueChange = { },
                    label = { Text("开始日期") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .clickable { showStartDatePicker.value = true },
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )
                
                TextFieldWithErrorState(
                    value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(targetDate.value),
                    onValueChange = { },
                    label = "目标日期",
                    error = targetDateError.value,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .clickable { showTargetDatePicker.value = true },
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 优先级
            Text("优先级", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = priority.value.toFloat(),
                onValueChange = { priority.value = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("低", style = MaterialTheme.typography.bodySmall)
                Text("高", style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 备注
            OutlinedTextField(
                value = note.value,
                onValueChange = { note.value = it },
                label = { Text("备注 (可选)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 自动存款设置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("自动存款设置 (可选)", style = MaterialTheme.typography.titleMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = autoSaveAmount.value,
                        onValueChange = { autoSaveAmount.value = it },
                        label = { Text("定期存款金额") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("¥", style = MaterialTheme.typography.bodyLarge) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = autoSaveFrequencyDays.value,
                        onValueChange = { autoSaveFrequencyDays.value = it },
                        label = { Text("存款频率 (天)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // 日期选择对话框
    if (showStartDatePicker.value) {
        DatePickerDialog(
            initialDate = startDate.value,
            onDateSelected = { selectedDate ->
                startDate.value = selectedDate
                showStartDatePicker.value = false
            },
            onDismiss = { showStartDatePicker.value = false }
        )
    }
    
    if (showTargetDatePicker.value) {
        DatePickerDialog(
            initialDate = targetDate.value,
            minDate = startDate.value,
            onDateSelected = { selectedDate ->
                targetDate.value = selectedDate
                showTargetDatePicker.value = false
            },
            onDismiss = { showTargetDatePicker.value = false }
        )
    }
    
    // 账户选择对话框
    if (showAccountPicker.value) {
        AlertDialog(
            onDismissRequest = { showAccountPicker.value = false },
            title = { Text("选择关联账户") },
            text = {
                Column {
                    if (accounts.isEmpty()) {
                        Text("没有可用账户")
                    } else {
                        accounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAccount.value = account.id
                                        showAccountPicker.value = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (account.id == selectedAccount.value) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountPicker.value = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 颜色选择对话框
    if (showColorPicker.value) {
        ColorPickerDialog(
            initialColor = Color(color.value),
            onColorSelected = { selectedColor ->
                color.value = selectedColor.toArgb()
                showColorPicker.value = false
            },
            onDismiss = { showColorPicker.value = false }
        )
    }
    
    // 图标选择对话框
    if (showIconPicker.value) {
        IconPickerDialog(
            onIconSelected = { uri ->
                iconUri.value = uri
                showIconPicker.value = false
            },
            onDismiss = { showIconPicker.value = false }
        )
    }
} 