package com.ccjizhang.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.NavPerformanceMonitor
import com.ccjizhang.ui.navigation.PerformanceMetrics
import com.ccjizhang.ui.navigation.PerformanceReport
import com.ccjizhang.ui.navigation.PerformanceWarning
import com.ccjizhang.ui.navigation.PerformanceWarningType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导航性能报告页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavPerformanceReportScreen(navParameters: NavParametersUnified) {
    // 获取性能数据
    val performanceData by NavPerformanceMonitor.performanceData.collectAsState()
    
    // 获取性能警告
    val performanceWarnings by NavPerformanceMonitor.performanceWarnings.collectAsState()
    
    // 生成性能报告
    val performanceReport = remember(performanceData, performanceWarnings) {
        NavPerformanceMonitor.getPerformanceReport()
    }
    
    // 选择的标签
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导航性能报告") },
                navigationIcon = {
                    IconButton(onClick = navParameters.onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { NavPerformanceMonitor.reset() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 性能报告摘要
            PerformanceReportSummary(performanceReport)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标签栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton(
                    text = "性能指标",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                
                TabButton(
                    text = "性能警告",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内容
            when (selectedTab) {
                0 -> PerformanceMetricsList(performanceData)
                1 -> PerformanceWarningsList(performanceWarnings)
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PerformanceReportSummary(report: PerformanceReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "性能报告摘要",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 平均导航时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "平均导航时间",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "${report.avgNavigationTime.toInt()} ms",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 平均页面加载时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "平均页面加载时间",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "${report.avgPageLoadTime.toInt()} ms",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 最慢导航
            report.slowestNavigation?.let { metrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "最慢导航",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = "${metrics.route} (${metrics.navigationTime} ms)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 最慢页面加载
            report.slowestPageLoad?.let { metrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "最慢页面加载",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Text(
                        text = "${metrics.route} (${metrics.pageLoadTime} ms)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 性能警告数量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "性能警告数量",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = "${report.warningCount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (report.warningCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 报告时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "报告时间",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Text(
                    text = formatTimestamp(report.timestamp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsList(performanceData: Map<String, PerformanceMetrics>) {
    if (performanceData.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无性能数据",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn {
            items(performanceData.values.toList()) { metrics ->
                PerformanceMetricsItem(metrics)
                
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricsItem(metrics: PerformanceMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = metrics.route,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 导航时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "导航时间",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${metrics.navigationTime} ms",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 页面加载时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "页面加载时间",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${metrics.pageLoadTime} ms",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 导航次数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "导航次数",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${metrics.navigationCount}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 平均导航时间
        if (metrics.navigationCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "平均导航时间",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "${metrics.totalNavigationTime / metrics.navigationCount} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // 平均页面加载时间
        if (metrics.pageLoadCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "平均页面加载时间",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "${metrics.totalPageLoadTime / metrics.pageLoadCount} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PerformanceWarningsList(performanceWarnings: List<PerformanceWarning>) {
    if (performanceWarnings.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无性能警告",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn {
            items(performanceWarnings) { warning ->
                PerformanceWarningItem(warning)
                
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerformanceWarningItem(warning: PerformanceWarning) {
    val warningTypeText = when (warning.warningType) {
        PerformanceWarningType.NAVIGATION_TIME -> "导航时间过长"
        PerformanceWarningType.PAGE_LOAD_TIME -> "页面加载时间过长"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = warningTypeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "路由: ${warning.route}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "值: ${warning.value} ms (阈值: ${warning.threshold} ms)",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "时间: ${formatTimestamp(warning.timestamp)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
