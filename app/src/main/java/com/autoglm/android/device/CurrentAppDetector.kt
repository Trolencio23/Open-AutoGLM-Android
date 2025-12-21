package com.autoglm.android.device

import com.autoglm.android.config.AppPackages
import com.autoglm.android.shizuku.ShizukuExecutor

object CurrentAppDetector {
    
    suspend fun getCurrentApp(): String {
        val result = ShizukuExecutor.execute("dumpsys window")
        if (!result.success) {
            return "System Home"
        }
        
        val output = result.output
        for (line in output.lines()) {
            if ("mCurrentFocus" in line || "mFocusedApp" in line) {
                for ((appName, packageName) in AppPackages.APP_PACKAGES) {
                    if (packageName in line) {
                        return appName
                    }
                }
            }
        }
        
        return "System Home"
    }
    
    suspend fun getCurrentPackage(): String? {
        val result = ShizukuExecutor.execute("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'")
        if (!result.success) {
            return null
        }
        
        val regex = """([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z][a-zA-Z0-9_]*)+)/""".toRegex()
        val match = regex.find(result.output)
        return match?.groupValues?.get(1)
    }
    
    suspend fun getScreenSize(): Pair<Int, Int>? {
        val result = ShizukuExecutor.execute("wm size")
        if (!result.success) {
            return null
        }
        
        val regex = """(\d+)x(\d+)""".toRegex()
        val match = regex.find(result.output) ?: return null
        val (width, height) = match.destructured
        return width.toInt() to height.toInt()
    }
    
    suspend fun getScreenDensity(): Int? {
        val result = ShizukuExecutor.execute("wm density")
        if (!result.success) {
            return null
        }
        
        val regex = """(\d+)""".toRegex()
        val match = regex.find(result.output) ?: return null
        return match.value.toInt()
    }
}
