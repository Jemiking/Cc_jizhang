package com.ccjizhang.ui.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import java.util.Date

/**
 * 日期选择对话框
 * 
 * @param initialDate 初始日期
 * @param minDate 最小可选日期（可选）
 * @param maxDate 最大可选日期（可选）
 * @param onDateSelected 日期选择回调
 * @param onDismiss 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Date,
    minDate: Date? = null,
    maxDate: Date? = null,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    // 将毫秒时间戳转换为UTC毫秒时间戳
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )
    
    val confirmEnabled = remember {
        derivedStateOf {
            val selectedDate = datePickerState.selectedDateMillis?.let { Date(it) }
            if (selectedDate == null) {
                false
            } else {
                (minDate == null || !selectedDate.before(minDate)) &&
                        (maxDate == null || !selectedDate.after(maxDate))
            }
        }
    }
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(Date(it))
                    }
                    onDismiss()
                },
                enabled = confirmEnabled.value
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
} 