package com.autoglm.android.shizuku

import android.util.Log
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellService : IShellService.Stub {
    
    companion object {
        private const val TAG = "ShellService"
    }
    
    private var lastExitCode = 0
    
    constructor() : super() {
        Log.d(TAG, "ShellService created")
    }
    
    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: android.content.Context) : super() {
        Log.d(TAG, "ShellService created with context")
    }
    
    override fun destroy() {
        Log.d(TAG, "ShellService destroy")
        System.exit(0)
    }
    
    override fun executeCommand(command: String): String {
        Log.d(TAG, "executeCommand: $command")
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            
            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = stdout.readText()
            val error = stderr.readText()
            lastExitCode = process.waitFor()
            
            stdout.close()
            stderr.close()
            process.destroy()
            
            Log.d(TAG, "executeCommand: exitCode=$lastExitCode, outputLen=${output.length}")
            
            if (lastExitCode == 0) {
                output.trim()
            } else {
                "ERROR:$error"
            }
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand failed", e)
            lastExitCode = -1
            "ERROR:${e.message}"
        }
    }
    
    override fun getExitCode(): Int = lastExitCode
}
