package com.ccjizhang.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 日期范围选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startDate: LocalDate, endDate: LocalDate) -> Unit,
    initialStartDate: LocalDate = LocalDate.now().minusDays(30),
    initialEndDate: LocalDate = LocalDate.now()
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 开始日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "开始日期:",
                        modifier = Modifier.weight(0.3f)
                    )
                    
                    Button(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text(startDate.format(dateFormatter))
                    }
                }
                
                // 结束日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "结束日期:",
                        modifier = Modifier.weight(0.3f)
                    )
                    
                    Button(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text(endDate.format(dateFormatter))
                    }
                }
                
                // 快捷选择按钮
                Text(
                    text = "快捷选择:",
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickSelectButton(
                        text = "最近7天",
                        onClick = {
                            val now = LocalDate.now()
                            startDate = now.minusDays(6)
                            endDate = now
                        }
                    )
                    
                    QuickSelectButton(
                        text = "最近30天",
                        onClick = {
                            val now = LocalDate.now()
                            startDate = now.minusDays(29)
                            endDate = now
                        }
                    )
                    
                    QuickSelectButton(
                        text = "最近90天",
                        onClick = {
                            val now = LocalDate.now()
                            startDate = now.minusDays(89)
                            endDate = now
                        }
                    )
                }
                
                // 日期验证错误提示
                if (startDate.isAfter(endDate)) {
                    Text(
                        text = "开始日期不能晚于结束日期",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!startDate.isAfter(endDate)) {
                        onConfirm(startDate, endDate)
                    }
                },
                enabled = !startDate.isAfter(endDate)
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
    
    // 开始日期选择器
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            onDateSelected = { 
                startDate = it
                showStartDatePicker = false
                
                // 如果开始日期晚于结束日期，自动调整结束日期
                if (startDate.isAfter(endDate)) {
                    endDate = startDate
                }
            },
            initialDate = startDate
        )
    }
    
    // 结束日期选择器
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            onDateSelected = { 
                endDate = it
                showEndDatePicker = false
                
                // 如果结束日期早于开始日期，自动调整开始日期
                if (endDate.isBefore(startDate)) {
                    startDate = endDate
                }
            },
            initialDate = endDate,
            maxDate = LocalDate.now() // 结束日期不能超过今天
        )
    }
}

@Composable
fun QuickSelectButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * 日期选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now(),
    minDate: LocalDate = LocalDate.of(2000, 1, 1),
    maxDate: LocalDate = LocalDate.now().plusYears(10)
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择日期") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 这里应该使用Material3的DatePicker，但由于它的API可能不稳定，
                // 我们使用简化版的日期选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 年份选择
                    OutlinedButton(onClick = {
                        if (selectedDate.minusYears(1).isAfter(minDate) || selectedDate.minusYears(1).isEqual(minDate)) {
                            selectedDate = selectedDate.minusYears(1)
                        }
                    }) {
                        Text("-1年")
                    }
                    
                    Text(
                        text = "${selectedDate.year}年",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    OutlinedButton(onClick = {
                        if (selectedDate.plusYears(1).isBefore(maxDate) || selectedDate.plusYears(1).isEqual(maxDate)) {
                            selectedDate = selectedDate.plusYears(1)
                        }
                    }) {
                        Text("+1年")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 月份选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = {
                        if (selectedDate.minusMonths(1).isAfter(minDate) || selectedDate.minusMonths(1).isEqual(minDate)) {
                            selectedDate = selectedDate.minusMonths(1)
                        }
                    }) {
                        Text("-1月")
                    }
                    
                    Text(
                        text = "${selectedDate.monthValue}月",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    OutlinedButton(onClick = {
                        if (selectedDate.plusMonths(1).isBefore(maxDate) || selectedDate.plusMonths(1).isEqual(maxDate)) {
                            selectedDate = selectedDate.plusMonths(1)
                        }
                    }) {
                        Text("+1月")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = {
                        if (selectedDate.minusDays(1).isAfter(minDate) || selectedDate.minusDays(1).isEqual(minDate)) {
                            selectedDate = selectedDate.minusDays(1)
                        }
                    }) {
                        Text("-1日")
                    }
                    
                    Text(
                        text = "${selectedDate.dayOfMonth}日",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    OutlinedButton(onClick = {
                        if (selectedDate.plusDays(1).isBefore(maxDate) || selectedDate.plusDays(1).isEqual(maxDate)) {
                            selectedDate = selectedDate.plusDays(1)
                        }
                    }) {
                        Text("+1日")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDateSelected(selectedDate) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
