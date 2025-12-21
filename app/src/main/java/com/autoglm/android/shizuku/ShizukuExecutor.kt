package com.autoglm.android.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

private const val TAG = "ShizukuExecutor"

object ShizukuExecutor {
    
    @Volatile
    private var shellService: IShellService? = null
    
    @Volatile
    private var isBinding = false
    
    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            "com.autoglm.android",
            ShellService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(true)
        .version(1)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "ShellService connected: $name")
            if (service != null && service.pingBinder()) {
                shellService = IShellService.Stub.asInterface(service)
                Log.d(TAG, "ShellService interface obtained")
            }
            isBinding = false
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "ShellService disconnected")
            shellService = null
            isBinding = false
        }
    }
    
    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!ShizukuManager.checkPermission()) {
            Log.e(TAG, "execute: Shizuku permission not granted")
            return@withContext CommandResult(
                success = false,
                output = "",
                error = "Shizuku permission not granted"
            )
        }
        
        // Ensure service is connected with timeout
        if (shellService == null && !isBinding) {
            bindService()
            // Wait for service to connect with timeout
            val connected = withTimeoutOrNull(5000L) {
                while (shellService == null) {
                    delay(100)
                }
                true
            } ?: false
            
            if (!connected) {
                Log.e(TAG, "execute: ShellService connection timeout")
                return@withContext CommandResult(
                    success = false,
                    output = "",
                    error = "ShellService connection timeout"
                )
            }
        }
        
        val service = shellService
        if (service == null) {
            Log.e(TAG, "execute: ShellService not available")
            return@withContext CommandResult(
                success = false,
                output = "",
                error = "ShellService not available"
            )
        }
        
        try {
            Log.d(TAG, "executeCommand: $command")
            val result = service.executeCommand(command)
            val exitCode = service.exitCode
            
            Log.d(TAG, "executeCommand result: exitCode=$exitCode, len=${result.length}")
            
            if (result.startsWith("ERROR:")) {
                CommandResult(
                    success = false,
                    output = "",
                    error = result.removePrefix("ERROR:"),
                    exitCode = exitCode
                )
            } else {
                CommandResult(
                    success = exitCode == 0,
                    output = result,
                    error = "",
                    exitCode = exitCode
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "execute failed", e)
            shellService = null
            CommandResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    private fun bindService() {
        if (isBinding) return
        isBinding = true
        Log.d(TAG, "Binding ShellService...")
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind ShellService", e)
            isBinding = false
        }
    }
    
    suspend fun executeLines(command: String): List<String> {
        val result = execute(command)
        return if (result.success) {
            result.output.lines().filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }
    
    fun unbindService() {
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unbind service", e)
        }
        shellService = null
    }
}

data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String = "",
    val exitCode: Int = -1
)
