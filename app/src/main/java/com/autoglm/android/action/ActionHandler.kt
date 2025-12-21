package com.autoglm.android.action

import com.autoglm.android.config.TimingConfig
import com.autoglm.android.device.AppLauncher
import com.autoglm.android.device.CurrentAppDetector
import com.autoglm.android.device.DeviceController
import com.autoglm.android.device.InputService
import kotlinx.coroutines.delay

data class ActionResult(
    val success: Boolean,
    val shouldFinish: Boolean,
    val message: String? = null,
    val requiresConfirmation: Boolean = false
)

class ActionHandler(
    private val onConfirmationRequired: suspend (String) -> Boolean = { true },
    private val onTakeoverRequired: suspend (String) -> Unit = {}
) {
    
    suspend fun execute(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int
    ): ActionResult {
        if (action.isFinish) {
            return ActionResult(
                success = true,
                shouldFinish = true,
                message = action.getString("message")
            )
        }
        
        if (!action.isDo) {
            return ActionResult(
                success = false,
                shouldFinish = true,
                message = "Unknown action type: ${action.metadata}"
            )
        }
        
        return when (action.actionType) {
            "Launch" -> handleLaunch(action)
            "Tap" -> handleTap(action, screenWidth, screenHeight)
            "Type", "Type_Name" -> handleType(action)
            "Swipe" -> handleSwipe(action, screenWidth, screenHeight)
            "Back" -> handleBack()
            "Home" -> handleHome()
            "Double Tap" -> handleDoubleTap(action, screenWidth, screenHeight)
            "Long Press" -> handleLongPress(action, screenWidth, screenHeight)
            "Wait" -> handleWait(action)
            "Take_over" -> handleTakeover(action)
            "Note" -> ActionResult(success = true, shouldFinish = false)
            "Call_API" -> ActionResult(success = true, shouldFinish = false)
            "Interact" -> ActionResult(success = true, shouldFinish = false, message = "User interaction required")
            else -> ActionResult(success = false, shouldFinish = false, message = "Unknown action: ${action.actionType}")
        }
    }
    
    private suspend fun handleLaunch(action: ParsedAction): ActionResult {
        val appName = action.getString("app")
            ?: return ActionResult(false, false, "No app name specified")
        
        val success = AppLauncher.launchApp(appName)
        return if (success) {
            ActionResult(true, false)
        } else {
            ActionResult(false, false, "App not found: $appName")
        }
    }
    
    private suspend fun handleTap(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int
    ): ActionResult {
        val element = action.getIntList("element")
            ?: return ActionResult(false, false, "No element coordinates")
        
        // Check for sensitive operation confirmation
        val message = action.getString("message")
        if (message != null) {
            val confirmed = onConfirmationRequired(message)
            if (!confirmed) {
                return ActionResult(
                    success = false,
                    shouldFinish = true,
                    message = "User cancelled sensitive operation"
                )
            }
        }
        
        val (x, y) = DeviceController.convertRelativeToAbsolute(element, screenWidth, screenHeight)
        DeviceController.tap(x, y)
        return ActionResult(true, false)
    }
    
    private suspend fun handleType(action: ParsedAction): ActionResult {
        val text = action.getString("text") ?: ""
        InputService.typeText(text)
        return ActionResult(true, false)
    }
    
    private suspend fun handleSwipe(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int
    ): ActionResult {
        val start = action.getIntList("start")
            ?: return ActionResult(false, false, "Missing start coordinates")
        val end = action.getIntList("end")
            ?: return ActionResult(false, false, "Missing end coordinates")
        
        val (startX, startY) = DeviceController.convertRelativeToAbsolute(start, screenWidth, screenHeight)
        val (endX, endY) = DeviceController.convertRelativeToAbsolute(end, screenWidth, screenHeight)
        
        DeviceController.swipe(startX, startY, endX, endY)
        return ActionResult(true, false)
    }
    
    private suspend fun handleBack(): ActionResult {
        DeviceController.back()
        return ActionResult(true, false)
    }
    
    private suspend fun handleHome(): ActionResult {
        DeviceController.home()
        return ActionResult(true, false)
    }
    
    private suspend fun handleDoubleTap(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int
    ): ActionResult {
        val element = action.getIntList("element")
            ?: return ActionResult(false, false, "No element coordinates")
        
        val (x, y) = DeviceController.convertRelativeToAbsolute(element, screenWidth, screenHeight)
        DeviceController.doubleTap(x, y)
        return ActionResult(true, false)
    }
    
    private suspend fun handleLongPress(
        action: ParsedAction,
        screenWidth: Int,
        screenHeight: Int
    ): ActionResult {
        val element = action.getIntList("element")
            ?: return ActionResult(false, false, "No element coordinates")
        
        val (x, y) = DeviceController.convertRelativeToAbsolute(element, screenWidth, screenHeight)
        DeviceController.longPress(x, y)
        return ActionResult(true, false)
    }
    
    private suspend fun handleWait(action: ParsedAction): ActionResult {
        val durationStr = action.getString("duration") ?: "1 seconds"
        val duration = try {
            durationStr.replace("seconds", "").trim().toDouble()
        } catch (e: Exception) {
            1.0
        }
        
        delay((duration * 1000).toLong())
        return ActionResult(true, false)
    }
    
    private suspend fun handleTakeover(action: ParsedAction): ActionResult {
        val message = action.getString("message") ?: "User intervention required"
        onTakeoverRequired(message)
        return ActionResult(true, false)
    }
}
