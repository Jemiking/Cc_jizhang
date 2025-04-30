package com.ccjizhang.ui.screens.transaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold

/**
 * 交易详情屏幕
 */
@Composable
fun TransactionDetailScreen(
    navController: NavHostController,
    transactionId: Long
) {
    RoundedTopBarScaffold(
        title = "交易详情",
        navController = navController,
        showBackButton = true
    ) { paddingValues ->
        // 占位内容，后续实现
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "交易详情 ID: $transactionId",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
} 