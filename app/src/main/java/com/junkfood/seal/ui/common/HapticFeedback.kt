package com.junkfood.seal.ui.common

import android.view.HapticFeedbackConstants
import android.view.View
import com.junkfood.seal.util.NOTIFICATION_VIBRATE
import com.junkfood.seal.util.PreferenceUtil.getBoolean

object HapticFeedback {
    fun View.slightHapticFeedback() {
        if (NOTIFICATION_VIBRATE.getBoolean()) {
            this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun View.longPressHapticFeedback() {
        if (NOTIFICATION_VIBRATE.getBoolean()) {
            this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
