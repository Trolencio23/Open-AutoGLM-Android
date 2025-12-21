package com.autoglm.android.config

object TimingConfig {
    
    object Device {
        const val DEFAULT_TAP_DELAY = 500L
        const val DEFAULT_DOUBLE_TAP_DELAY = 500L
        const val DEFAULT_DOUBLE_TAP_INTERVAL = 100L
        const val DEFAULT_LONG_PRESS_DELAY = 500L
        const val DEFAULT_SWIPE_DELAY = 1000L
        const val DEFAULT_BACK_DELAY = 500L
        const val DEFAULT_HOME_DELAY = 500L
        const val DEFAULT_LAUNCH_DELAY = 2000L
    }
    
    object Action {
        const val KEYBOARD_SWITCH_DELAY = 200L
        const val TEXT_CLEAR_DELAY = 100L
        const val TEXT_INPUT_DELAY = 500L
        const val KEYBOARD_RESTORE_DELAY = 200L
    }
    
    object Agent {
        const val SCREENSHOT_DELAY = 300L
        const val STEP_DELAY = 500L
    }
}
