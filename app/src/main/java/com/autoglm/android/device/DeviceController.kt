package com.autoglm.android.device

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.autoglm.android.shizuku.ShizukuExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object DeviceController {
    
    suspend fun tap(x: Int, y: Int, delayMs: Long = 500) {
        ShizukuExecutor.execute("input tap $x $y")
        delay(delayMs)
    }
    
    suspend fun doubleTap(x: Int, y: Int, delayMs: Long = 500) {
        ShizukuExecutor.execute("input tap $x $y")
        delay(100)
        ShizukuExecutor.execute("input tap $x $y")
        delay(delayMs)
    }
    
    suspend fun longPress(x: Int, y: Int, durationMs: Int = 3000, delayMs: Long = 500) {
        ShizukuExecutor.execute("input swipe $x $y $x $y $durationMs")
        delay(delayMs)
    }
    
    suspend fun swipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMs: Int? = null,
        delayMs: Long = 1000
    ) {
        val duration = durationMs ?: run {
            val distSq = (startX - endX) * (startX - endX) + (startY - endY) * (startY - endY)
            (distSq / 1000).coerceIn(1000, 2000)
        }
        ShizukuExecutor.execute("input swipe $startX $startY $endX $endY $duration")
        delay(delayMs)
    }
    
    suspend fun back(delayMs: Long = 500) {
        ShizukuExecutor.execute("input keyevent 4")
        delay(delayMs)
    }
    
    suspend fun home(delayMs: Long = 500) {
        ShizukuExecutor.execute("input keyevent KEYCODE_HOME")
        delay(delayMs)
    }
    
    suspend fun pressEnter() {
        ShizukuExecutor.execute("input keyevent 66")
    }
    
    fun convertRelativeToAbsolute(
        element: List<Int>,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Int, Int> {
        val x = (element[0] / 1000.0 * screenWidth).toInt()
        val y = (element[1] / 1000.0 * screenHeight).toInt()
        return x to y
    }
}
