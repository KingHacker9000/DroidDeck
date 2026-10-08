package com.droiddeck.launcher.input

import android.view.MotionEvent

/**
 * A stylus (an S-Pen) on the direct touchscreen, on top of the plain touch path:
 *
 *  - hover .......................... moves the pointer, no button (tooltips, highlights)
 *  - side button held, then touch ... right button, held until the pen lifts
 *  - touch without the button ....... not handled here: a plain touch, like a finger
 *
 * Android reports a pen's barrel buttons (evdev BTN_STYLUS, BTN_STYLUS2) in the button state as
 * BUTTON_STYLUS_PRIMARY and BUTTON_STYLUS_SECONDARY; the S-Pen has the first only.
 */
class StylusInput(
    private val listener: PointerGestures.Listener,
    private val onHoverEnd: () -> Unit,
) {
    private var rightHeld = false
    private var lastX = 0f
    private var lastY = 0f

    /** Hover from a stylus; false for anything else, which keeps its usual path. */
    fun onHover(event: MotionEvent): Boolean {
        if (!isStylus(event)) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> listener.onMove(event.x, event.y)
            MotionEvent.ACTION_HOVER_EXIT -> onHoverEnd()
            else -> return false
        }
        return true
    }

    /** A touch; true while it is a side-button press, which then never reaches the touch path. */
    fun onTouch(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            release() // a press whose up never came
            if (!isStylus(event) || event.buttonState and SIDE_BUTTONS == 0) return false
            rightHeld = true
            track(event)
            listener.onButton(PointerGestures.BTN_RIGHT, true, lastX, lastY)
            return true
        }
        if (!rightHeld) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> { track(event); listener.onMove(lastX, lastY) }
            MotionEvent.ACTION_UP -> { track(event); release() }
            MotionEvent.ACTION_CANCEL -> release()
        }
        return true
    }

    private fun track(event: MotionEvent) { lastX = event.x; lastY = event.y }

    private fun release() {
        if (!rightHeld) return
        rightHeld = false
        listener.onButton(PointerGestures.BTN_RIGHT, false, lastX, lastY)
    }

    companion object {
        const val SIDE_BUTTONS = MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_STYLUS_SECONDARY

        fun isStylus(event: MotionEvent) = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
    }
}
