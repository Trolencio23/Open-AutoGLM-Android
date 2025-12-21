package com.autoglm.android.shizuku

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {
    
    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()
    
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateState()
    }
    
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        updateState()
    }
    
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _state.value = _state.value.copy(
            hasPermission = grantResult == PackageManager.PERMISSION_GRANTED
        )
    }
    
    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        updateState()
    }
    
    fun destroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }
    
    fun requestPermission(requestCode: Int = 1) {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        }
    }
    
    fun checkPermission(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }
    
    private fun updateState() {
        val isRunning = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        
        val hasPermission = if (isRunning) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
        
        _state.value = ShizukuState(
            isRunning = isRunning,
            hasPermission = hasPermission
        )
    }
}

data class ShizukuState(
    val isRunning: Boolean = false,
    val hasPermission: Boolean = false
) {
    val isReady: Boolean get() = isRunning && hasPermission
}
