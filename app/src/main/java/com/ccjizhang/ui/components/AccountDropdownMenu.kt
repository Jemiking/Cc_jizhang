package com.ccjizhang.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.ccjizhang.data.model.Account

/**
 * 账户下拉菜单组件
 * 用于显示账户列表的下拉菜单
 *
 * @param expanded 下拉菜单是否展开
 * @param accounts 要显示的账户列表
 * @param onAccountSelected 当用户选择账户时的回调
 * @param onDismissRequest 当用户关闭下拉菜单时的回调
 */
@Composable
fun AccountDropdownMenu(
    expanded: Boolean,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    onDismissRequest: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { 
                    Text("${account.name} (¥${String.format("%.2f", account.balance)})") 
                },
                onClick = {
                    onAccountSelected(account)
                }
            )
        }
    }
} 