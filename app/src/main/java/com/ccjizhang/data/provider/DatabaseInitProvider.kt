package com.ccjizhang.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.ccjizhang.utils.DatabaseRepairTool
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 数据库初始化提供者
 * 在应用启动的最早阶段运行，用于初始化和检查数据库
 */
class DatabaseInitProvider : ContentProvider() {
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DatabaseInitProviderEntryPoint {
        fun databaseRepairTool(): DatabaseRepairTool
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return false
        
        scope.launch {
            try {
                // 获取DatabaseRepairTool实例
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    DatabaseInitProviderEntryPoint::class.java
                )
                val databaseRepairTool = entryPoint.databaseRepairTool()
                
                // 检查数据库是否损坏
                if (databaseRepairTool.isDatabaseCorrupted()) {
                    Log.w("DatabaseInitProvider", "数据库损坏，尝试修复")
                    // 修复数据库
                    val repaired = databaseRepairTool.repairDatabase()
                    if (repaired) {
                        Log.i("DatabaseInitProvider", "数据库修复成功")
                    } else {
                        Log.e("DatabaseInitProvider", "数据库修复失败")
                    }
                }
            } catch (e: Exception) {
                Log.e("DatabaseInitProvider", "数据库初始化错误", e)
                try {
                    // 如果出现任何错误，尝试强制修复数据库
                    val entryPoint = EntryPointAccessors.fromApplication(
                        appContext,
                        DatabaseInitProviderEntryPoint::class.java
                    )
                    val databaseRepairTool = entryPoint.databaseRepairTool()
                    databaseRepairTool.forceRepairDatabase()
                    Log.i("DatabaseInitProvider", "强制数据库修复完成")
                } catch (e2: Exception) {
                    Log.e("DatabaseInitProvider", "强制数据库修复失败", e2)
                }
            }
        }
        
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null
    
    override fun getType(uri: Uri): String? = null
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
