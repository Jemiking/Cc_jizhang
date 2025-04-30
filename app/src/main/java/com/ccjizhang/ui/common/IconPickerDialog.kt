package com.ccjizhang.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 图标选择对话框
 */
@Composable
fun IconPickerDialog(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 预设图标列表
    val icons = listOf(
        Pair("savings", Icons.Default.Savings),
        Pair("home", Icons.Default.Home),
        Pair("car", Icons.Default.DirectionsCar),
        Pair("school", Icons.Default.School),
        Pair("flight", Icons.Default.Flight),
        Pair("restaurant", Icons.Default.Restaurant),
        Pair("shopping", Icons.Default.ShoppingCart),
        Pair("vacation", Icons.Default.BeachAccess),
        Pair("health", Icons.Default.HealthAndSafety),
        Pair("tech", Icons.Default.Devices),
        Pair("gift", Icons.Default.CardGiftcard),
        Pair("business", Icons.Default.Business),
        Pair("family", Icons.Default.People),
        Pair("hobby", Icons.Default.SportsBasketball),
        Pair("music", Icons.Default.MusicNote),
        Pair("book", Icons.Default.MenuBook),
        Pair("movie", Icons.Default.Movie),
        Pair("pet", Icons.Default.Pets),
        Pair("celebration", Icons.Default.Celebration),
        Pair("fitness", Icons.Default.FitnessCenter)
    )
    
    var selectedIconId by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(icons) { (id, icon) ->
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 2.dp,
                                color = if (selectedIconId == id) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { 
                                selectedIconId = id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = id,
                            tint = if (selectedIconId == id) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedIconId?.let { onIconSelected(it) }
                    onDismiss()
                },
                enabled = selectedIconId != null
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