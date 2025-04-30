package com.ccjizhang.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.ui.viewmodels.CreditCardViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 信用卡账单信息组件
 * 显示信用卡的账单周期、下次账单日、还款日等信息
 * @param creditCard 信用卡账户
 * @param viewModel 信用卡视图模型
 */
@Composable
fun CreditCardBillingInfo(
    creditCard: Account,
    viewModel: CreditCardViewModel
) {
    if (creditCard.type != AccountType.CREDIT_CARD) {
        return
    }
    
    val dateFormat = remember { SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()) }
    
    val daysUntilBillingDate = remember(creditCard) {
        viewModel.getDaysUntilNextBillingDate(creditCard)
    }
    
    val daysUntilDueDate = remember(creditCard) {
        viewModel.getDaysUntilNextDueDate(creditCard)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "账单周期信息",
                style = MaterialTheme.typography.titleMedium
            )
            
            Divider()
            
            // 账单日信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("账单日")
                Text("每月${creditCard.billingDay}日")
            }
            
            // 还款日信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("还款日")
                Text("每月${creditCard.dueDay}日")
            }
            
            Divider()
            
            // 下次账单日
            if (creditCard.nextBillingDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("下次账单日")
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(dateFormat.format(creditCard.nextBillingDate!!))
                        Text(
                            text = "还有 $daysUntilBillingDate 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                daysUntilBillingDate <= 3 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            // 下次还款日
            if (creditCard.nextDueDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("下次还款日")
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(dateFormat.format(creditCard.nextDueDate!!))
                        Text(
                            text = "还有 $daysUntilDueDate 天",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                daysUntilDueDate <= 3 -> MaterialTheme.colorScheme.error
                                daysUntilDueDate <= 7 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (daysUntilDueDate <= 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            // 信用额度使用情况
            if (creditCard.creditLimit > 0) {
                Divider()
                
                val usedAmount = if (creditCard.balance < 0) -creditCard.balance else 0.0
                val usedPercentage = (usedAmount / creditCard.creditLimit * 100).toInt()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("信用额度")
                    Text("¥${creditCard.creditLimit}")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已用额度")
                    Text("¥$usedAmount ($usedPercentage%)")
                }
                
                // 信用额度使用进度条
                LinearProgressIndicator(
                    progress = (usedAmount / creditCard.creditLimit).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = when {
                        usedPercentage > 80 -> MaterialTheme.colorScheme.error
                        usedPercentage > 50 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
 