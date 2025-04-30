package com.ccjizhang.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.ccjizhang.ui.navigation.NavAnalyticsReporter
import com.ccjizhang.ui.navigation.NavParametersUnified
import kotlinx.coroutines.launch
import java.io.File

/**
 * 导航分析报告页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavAnalyticsReportScreen(navParameters: NavParametersUnified) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val reportState by NavAnalyticsReporter.reportState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导航分析报告") },
                navigationIcon = {
                    IconButton(onClick = navParameters.onNavigateBack) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (reportState) {
                    is NavAnalyticsReporter.ReportState.Idle -> {
                        Text(
                            text = "生成导航分析报告",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "点击下方按钮生成导航分析报告，报告将包含屏幕停留时间、导航性能等信息。",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        NavAnalyticsReporter.generateReport(context)
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("生成报告失败：${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("生成报告")
                        }
                    }

                    is NavAnalyticsReporter.ReportState.Generating -> {
                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "正在生成报告...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    is NavAnalyticsReporter.ReportState.Success -> {
                        val filePath = (reportState as NavAnalyticsReporter.ReportState.Success).filePath

                        Text(
                            text = "报告生成成功",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "报告已保存到：$filePath",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                val file = File(filePath)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "text/plain")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(Intent.createChooser(intent, "打开报告"))
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("查看报告")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                NavAnalyticsReporter.resetReportState()
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("生成新报告")
                        }
                    }

                    is NavAnalyticsReporter.ReportState.Error -> {
                        val errorMessage = (reportState as NavAnalyticsReporter.ReportState.Error).message

                        Text(
                            text = "生成报告失败",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                NavAnalyticsReporter.resetReportState()
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}
