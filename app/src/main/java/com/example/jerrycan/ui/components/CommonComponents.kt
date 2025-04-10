package com.example.jerrycan.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jerrycan.R

/**
 * 全屏加载指示器
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = stringResource(id = R.string.loading)
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = message)
            }
        }
    }
}

/**
 * 错误提示对话框
 */
@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = stringResource(id = R.string.error_title)) },
        text = { Text(text = errorMessage) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    )
}

/**
 * 信息提示对话框
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    )
}

/**
 * 确认对话框
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(id = R.string.confirm),
    cancelText: String = stringResource(id = R.string.cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = cancelText)
            }
        }
    )
}

/**
 * 空状态提示
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAction,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

/**
 * 状态指示器（已连接/未连接等）
 */
@Composable
fun StatusIndicator(
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 8.dp else 12.dp)
                .background(
                    color = if (isActive) MaterialTheme.colorScheme.tertiary else Color.Gray,
                    shape = RoundedCornerShape(if (compact) 4.dp else 6.dp)
                )
        )
        
        Spacer(modifier = Modifier.width(if (compact) 2.dp else 4.dp))
        
        Text(
            text = if (isActive) activeLabel else inactiveLabel,
            style = if (compact) 
                MaterialTheme.typography.labelSmall
            else 
                MaterialTheme.typography.bodySmall,
            color = if (isActive) 
                MaterialTheme.colorScheme.tertiary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
} 