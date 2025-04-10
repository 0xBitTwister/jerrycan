package com.example.jerrycan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * 文件工具类
 */
object FileUtils {
    
    private const val TAG = "FileUtils"
    
    /**
     * 分享文件给其他应用
     * @param context 上下文
     * @param file 要分享的文件
     * @param title 分享对话框标题
     * @param mimeType 文件MIME类型
     */
    fun shareFile(context: Context, file: File, title: String, mimeType: String = "text/plain") {
        try {
            // 使用FileProvider获取文件Uri（兼容Android 7.0+）
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            shareFile(context, fileUri, title, mimeType)
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 分享文件给其他应用
     * @param context 上下文
     * @param fileUri 文件Uri
     * @param title 分享对话框标题
     * @param mimeType 文件MIME类型
     */
    fun shareFile(context: Context, fileUri: Uri, title: String, mimeType: String = "text/plain") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Log.e(TAG, "分享文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 打开文件
     * @param context 上下文
     * @param file 要打开的文件
     * @param mimeType 文件MIME类型
     */
    fun openFile(context: Context, file: File, mimeType: String = "*/*") {
        try {
            // 使用FileProvider获取文件Uri（兼容Android 7.0+）
            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 获取文件大小的友好显示字符串
     * @param size 文件大小（字节）
     * @return 友好显示字符串，如"1.2 MB"
     */
    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
} 