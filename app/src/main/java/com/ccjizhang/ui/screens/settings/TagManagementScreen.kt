package com.ccjizhang.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.ccjizhang.ui.components.RoundedTopBarScaffold
import com.ccjizhang.ui.components.UnifiedScaffold
import com.ccjizhang.ui.components.PrimaryCard
import com.ccjizhang.ui.components.SecondaryCard
import com.ccjizhang.ui.viewmodels.TagViewModel
import com.ccjizhang.ui.navigation.NavRoutes
import com.ccjizhang.ui.navigation.NavParametersUnified
import com.ccjizhang.ui.navigation.createUnifiedNavParameters
import kotlinx.coroutines.launch

/**
 * 标签管理界面
 */
@Composable
fun TagManagementScreen(
    navController: NavHostController,
    viewModel: TagViewModel = hiltViewModel()
) {
    // 本地UI状态
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }

    // Snackbar状态
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 加载标签使用统计
    LaunchedEffect(Unit) {
        viewModel.loadTagTransactionCounts()
    }

    // 使用collectAsState()方法收集状态
    // 使用as强制类型转换
    val allTagsState = viewModel.allTags.collectAsState()
    val allTags = remember(allTagsState.value) {
        allTagsState.value as? List<String> ?: emptyList()
    }

    val searchQueryState = viewModel.searchQuery.collectAsState()
    val searchQuery = remember(searchQueryState.value) {
        searchQueryState.value as String
    }

    val searchResultsState = viewModel.searchResults.collectAsState()
    val searchResults = remember(searchResultsState.value) {
        searchResultsState.value as? List<String> ?: emptyList()
    }

    val isProcessingState = viewModel.isProcessing.collectAsState()
    val isProcessing = remember(isProcessingState.value) {
        isProcessingState.value as Boolean
    }

    val resultMessageState = viewModel.resultMessage.collectAsState()
    val resultMessage = remember(resultMessageState.value) {
        resultMessageState.value as? String
    }

    val editingTagState = viewModel.editingTag.collectAsState()
    val editingTag = remember(editingTagState.value) {
        editingTagState.value as? String
    }

    val selectedTagsState = viewModel.selectedTags.collectAsState()
    val selectedTags = remember(selectedTagsState.value) {
        selectedTagsState.value as? Set<String> ?: emptySet()
    }

    val tagTransactionCountsState = viewModel.tagTransactionCounts.collectAsState()
    val tagTransactionCounts = remember(tagTransactionCountsState.value) {
        tagTransactionCountsState.value as? Map<String, Int> ?: emptyMap()
    }

    // 显示结果消息
    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearResultMessage()
            }
        }
    }

    UnifiedScaffold(
        title = "标签管理",
        navController = navController,
        showBackButton = true,
        showFloatingActionButton = !selectionMode,
        floatingActionButtonContent = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加标签",
                tint = Color.White
            )
        },
        onFloatingActionButtonClick = { showAddTagDialog = true },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = {
                    isSearchActive = false
                    viewModel.updateSearchQuery("")
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭搜索"
                    )
                }
            } else {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索标签"
                    )
                }
            }

            if (selectionMode && selectedTags.isNotEmpty()) {
                IconButton(onClick = {
                    viewModel.deleteSelectedTags()
                    selectionMode = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除选中标签"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("搜索标签...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索图标"
                        )
                    },
                    singleLine = true
                )
            }

            // 多选模式提示
            AnimatedVisibility(visible = selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选择 ${selectedTags.size} 个标签",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    TextButton(onClick = {
                        selectionMode = false
                        viewModel.clearSelection()
                    }) {
                        Text("取消选择")
                    }
                }
            }

            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                // 加载中状态
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // 根据当前模式选择显示的列表
                    val displayTags = if (isSearchActive) searchResults else allTags

                    // 空列表状态
                    if (displayTags.isEmpty()) {
                        Text(
                            text = if (isSearchActive) "未找到匹配的标签" else "还没有标签，请添加标签",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        // 显示标签列表
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(items = displayTags, key = { it }) { tag ->
                                val count = tagTransactionCounts[tag] ?: 0
                                val isSelected = selectedTags.contains(tag)

                                TagItemView(
                                    tag = tag,
                                    count = count,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    onTagClick = {
                                        if (selectionMode) {
                                            viewModel.toggleTagSelection(tag)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            viewModel.toggleTagSelection(tag)
                                        }
                                    },
                                    onEditClick = {
                                        if (!selectionMode) {
                                            viewModel.startEditTag(tag)
                                        }
                                    },
                                    onDeleteClick = {
                                        if (!selectionMode) {
                                            viewModel.deleteTag(tag)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 底部空间，避免FAB遮挡
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // 添加标签对话框
    if (showAddTagDialog) {
        AddTagDialog(
            value = newTagName,
            onValueChange = { newTagName = it },
            onDismiss = {
                showAddTagDialog = false
                newTagName = ""
            },
            onConfirm = {
                if (newTagName.isNotBlank()) {
                    // 创建模拟交易ID来添加标签
                    val tempTransactionId = System.currentTimeMillis()
                    viewModel.setTagsForTransaction(tempTransactionId, listOf(newTagName))
                    newTagName = ""
                    showAddTagDialog = false

                    // 刷新标签列表和统计
                    viewModel.loadTagTransactionCounts()
                }
            }
        )
    }

    // 编辑标签对话框
    editingTag?.let { tag ->
        var editedTagName by remember(tag) { mutableStateOf(tag) }

        EditTagDialog(
            value = editedTagName,
            onValueChange = { editedTagName = it },
            onDismiss = { viewModel.cancelEdit() },
            onConfirm = {
                if (editedTagName.isNotBlank() && editedTagName != tag) {
                    viewModel.renameTag(editedTagName)
                    viewModel.loadTagTransactionCounts()
                } else {
                    viewModel.cancelEdit()
                }
            }
        )
    }
}

@Composable
fun TagItemView(
    tag: String,
    count: Int,
    isSelected: Boolean,
    selectionMode: Boolean,
    onTagClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    SecondaryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTagClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTagClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // 标签内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "使用次数: $count",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            if (!selectionMode) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun AddTagDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新标签") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("标签名称") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.isNotBlank()
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
}

@Composable
fun EditTagDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑标签") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("标签名称") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.isNotBlank()
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
}