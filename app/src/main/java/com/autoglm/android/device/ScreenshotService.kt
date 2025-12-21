package com.autoglm.android.device

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.autoglm.android.shizuku.ShizukuExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val TAG = "ScreenshotService"

data class Screenshot(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val base64Data: String
)

object ScreenshotService {
    
    suspend fun capture(): Screenshot? = withContext(Dispatchers.IO) {
        try {
            val result = ShizukuExecutor.execute("screencap -p")
            if (!result.success) {
                return@withContext null
            }
            
            val bytes = result.output.toByteArray(Charsets.ISO_8859_1)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null
            
            val base64 = bitmapToBase64(bitmap)
            
            Screenshot(
                bitmap = bitmap,
                width = bitmap.width,
                height = bitmap.height,
                base64Data = base64
            )
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun captureToFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val result = ShizukuExecutor.execute("screencap -p $path")
        result.success
    }
    
    suspend fun captureBase64(): String? = withContext(Dispatchers.IO) {
        try {
            val tempFile = "/data/local/tmp/autoglm_screenshot.png"
            val captureResult = ShizukuExecutor.execute("screencap -p $tempFile")
            if (!captureResult.success) {
                return@withContext null
            }
            
            val base64Result = ShizukuExecutor.execute("base64 -w 0 $tempFile")
            ShizukuExecutor.execute("rm $tempFile")
            
            if (base64Result.success) {
                base64Result.output
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun captureWithDimensions(): Triple<String, Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val tempFile = "/data/local/tmp/autoglm_screenshot.png"
            val base64File = "/data/local/tmp/autoglm_screenshot.b64"
            
            Log.d(TAG, "captureWithDimensions: Taking screenshot...")
            val captureResult = ShizukuExecutor.execute("screencap -p $tempFile")
            if (!captureResult.success) {
                Log.e(TAG, "captureWithDimensions: screencap failed: ${captureResult.error}")
                return@withContext null
            }
            
            val sizeResult = ShizukuExecutor.execute("wm size")
            val dimensions = parseDimensions(sizeResult.output)
            Log.d(TAG, "captureWithDimensions: dimensions=$dimensions")
            
            // Write base64 to file instead of returning through binder
            val base64WriteResult = ShizukuExecutor.execute("base64 -w 0 $tempFile > $base64File")
            if (!base64WriteResult.success) {
                Log.e(TAG, "captureWithDimensions: base64 write failed: ${base64WriteResult.error}")
                ShizukuExecutor.execute("rm $tempFile")
                return@withContext null
            }
            
            // Read base64 file and make it readable
            ShizukuExecutor.execute("chmod 644 $base64File")
            
            // Read the base64 content from the file
            val base64Content = try {
                java.io.File(base64File).readText()
            } catch (e: Exception) {
                Log.e(TAG, "captureWithDimensions: Failed to read base64 file", e)
                null
            }
            
            // Cleanup
            ShizukuExecutor.execute("rm $tempFile $base64File")
            
            if (base64Content != null && dimensions != null) {
                Log.d(TAG, "captureWithDimensions: Success, base64 length=${base64Content.length}")
                Triple(base64Content, dimensions.first, dimensions.second)
            } else {
                Log.e(TAG, "captureWithDimensions: Failed, base64=${base64Content != null}, dimensions=$dimensions")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "captureWithDimensions: Exception", e)
            null
        }
    }
    
    private fun parseDimensions(wmOutput: String): Pair<Int, Int>? {
        val regex = """(\d+)x(\d+)""".toRegex()
        val match = regex.find(wmOutput) ?: return null
        val (width, height) = match.destructured
        return width.toInt() to height.toInt()
    }
    
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
