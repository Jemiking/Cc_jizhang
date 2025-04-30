package com.ccjizhang.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ccjizhang.data.model.Currency

/**
 * 币种选择下拉组件
 *
 * @param selectedCurrency 当前选中的币种
 * @param onCurrencySelected 币种选择回调
 * @param modifier 组件修饰符
 * @param label 输入框标签
 * @param enabled 是否启用
 */
@Composable
fun CurrencyDropdown(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "币种",
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = Currency.values()
    
    Box(modifier = modifier) {
        OutlinedTextField(
            value = "${selectedCurrency.name} (${selectedCurrency.symbol})",
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "展开",
                    modifier = Modifier.clickable(enabled = enabled) { expanded = true }
                )
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (enabled) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(LocalDensity.current) {
                    // 保持下拉菜单宽度与输入框一致
                    LocalDensity.current.run { 280.dp.toPx().toInt().toDp() }
                })
            ) {
                currencies.forEach { currency ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${currency.name} (${currency.code}: ${currency.symbol})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
} 