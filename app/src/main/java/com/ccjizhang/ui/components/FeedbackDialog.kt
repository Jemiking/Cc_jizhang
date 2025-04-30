package com.ccjizhang.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 反馈对话框
 * 
 * @param onSubmit 提交反馈的回调
 * @param onDismiss 关闭对话框的回调
 */
@Composable
fun FeedbackDialog(
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var feedbackContent by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Feedback,
                contentDescription = "反馈",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "提供反馈",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "您的反馈对我们非常重要，请告诉我们您的想法或遇到的问题。",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 反馈内容输入框
                OutlinedTextField(
                    value = feedbackContent,
                    onValueChange = { feedbackContent = it },
                    label = { Text("反馈内容") },
                    placeholder = { Text("请描述您的建议或遇到的问题...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 联系方式输入框
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("联系方式（可选）") },
                    placeholder = { Text("邮箱或其他联系方式") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "注意：提交反馈后，我们会尽快处理并回复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (feedbackContent.isNotBlank()) {
                        onSubmit(feedbackContent, contactInfo)
                    }
                },
                enabled = feedbackContent.isNotBlank()
            ) {
                Text("提交")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("取消")
            }
        }
    )
}
