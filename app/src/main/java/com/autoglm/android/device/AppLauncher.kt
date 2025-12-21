package com.autoglm.android.device

import com.autoglm.android.config.AppPackages
import com.autoglm.android.shizuku.ShizukuExecutor
import kotlinx.coroutines.delay

object AppLauncher {
    
    suspend fun launchApp(appName: String, delayMs: Long = 2000): Boolean {
        val packageName = AppPackages.getPackageName(appName)
        if (packageName == null) {
            return false
        }
        
        val result = ShizukuExecutor.execute(
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        )
        
        delay(delayMs)
        return result.success || result.output.contains("Events injected")
    }
    
    suspend fun launchByPackage(packageName: String, delayMs: Long = 2000): Boolean {
        val result = ShizukuExecutor.execute(
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        )
        
        delay(delayMs)
        return result.success || result.output.contains("Events injected")
    }
    
    suspend fun launchByIntent(packageName: String, activityName: String, delayMs: Long = 2000): Boolean {
        val result = ShizukuExecutor.execute(
            "am start -n $packageName/$activityName"
        )
        
        delay(delayMs)
        return result.success
    }
}
