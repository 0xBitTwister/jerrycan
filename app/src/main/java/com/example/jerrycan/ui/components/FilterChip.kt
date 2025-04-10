package com.example.jerrycan.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 自定义的FilterChip组件
 */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(end = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = if (selected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surface,
        contentColor = if (selected) 
            MaterialTheme.colorScheme.onPrimaryContainer 
        else 
            MaterialTheme.colorScheme.onSurface,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        androidx.compose.material3.Surface(
            color = Color.Transparent
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
            ) {
                label()
            }
        }
    }
} 