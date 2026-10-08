package com.droiddeck.launcher.input

import android.view.InputDevice
import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class StylusInputTest {
    private val calls = mutableListOf<String>()
    private var hoverEnded = 0
    private val stylus = StylusInput(object : PointerGestures.Listener {
        override fun onMove(x: Float, y: Float) { calls += "move ${x.toInt()},${y.toInt()}" }
        override fun onButton(button: Int, pressed: Boolean, x: Float, y: Float) {
            calls += "${if (button == PointerGestures.BTN_RIGHT) "right" else "button $button"} ${if (pressed) "down" else "up"} ${x.toInt()},${y.toInt()}"
        }
        override fun onWheel(steps: Int) { calls += "wheel $steps" }
        override fun onLongPress() {}
    }) { hoverEnded++ }

    @Test fun penHoverMovesThePointerAndExitEndsIt() {
        assertTrue(stylus.onHover(event(MotionEvent.ACTION_HOVER_ENTER, 10f, 20f)))
        assertTrue(stylus.onHover(event(MotionEvent.ACTION_HOVER_MOVE, 30f, 40f)))
        assertTrue(stylus.onHover(event(MotionEvent.ACTION_HOVER_EXIT, 30f, 40f)))
        assertEquals(listOf("move 10,20", "move 30,40"), calls)
        assertEquals(1, hoverEnded)
    }

    @Test fun hoverFromAFingerOrMouseIsNotThePens() {
        assertFalse(stylus.onHover(event(MotionEvent.ACTION_HOVER_MOVE, 5f, 5f, tool = MotionEvent.TOOL_TYPE_MOUSE)))
        assertFalse(stylus.onHover(event(MotionEvent.ACTION_HOVER_MOVE, 5f, 5f, tool = MotionEvent.TOOL_TYPE_FINGER)))
        assertTrue(calls.isEmpty())
    }

    @Test fun aPlainPenTapIsLeftToTheTouchPath() {
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 50f, 60f)))
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_MOVE, 55f, 65f)))
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_UP, 55f, 65f)))
        assertTrue(calls.isEmpty())
    }

    @Test fun sideButtonTapIsARightClickOnly() {
        val side = MotionEvent.BUTTON_STYLUS_PRIMARY
        assertTrue(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 50f, 60f, buttons = side)))
        assertTrue(stylus.onTouch(event(MotionEvent.ACTION_MOVE, 52f, 61f, buttons = side)))
        // The button may come up before the tip; the press still ends with the tip.
        assertTrue(stylus.onTouch(event(MotionEvent.ACTION_UP, 53f, 62f)))
        assertEquals(listOf("right down 50,60", "move 52,61", "right up 53,62"), calls)
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 70f, 80f)))
    }

    @Test fun theSecondBarrelButtonAlsoRightClicks() {
        assertTrue(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 1f, 2f, buttons = MotionEvent.BUTTON_STYLUS_SECONDARY)))
        assertEquals(listOf("right down 1,2"), calls)
    }

    @Test fun aCancelledSideButtonPressReleasesTheButton() {
        stylus.onTouch(event(MotionEvent.ACTION_DOWN, 50f, 60f, buttons = MotionEvent.BUTTON_STYLUS_PRIMARY))
        assertTrue(stylus.onTouch(event(MotionEvent.ACTION_CANCEL, 0f, 0f)))
        assertEquals(listOf("right down 50,60", "right up 50,60"), calls)
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_MOVE, 5f, 5f)))
    }

    @Test fun aPressWhoseUpWasLostIsReleasedByTheNextTouch() {
        stylus.onTouch(event(MotionEvent.ACTION_DOWN, 50f, 60f, buttons = MotionEvent.BUTTON_STYLUS_PRIMARY))
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 10f, 10f, tool = MotionEvent.TOOL_TYPE_FINGER)))
        assertEquals(listOf("right down 50,60", "right up 50,60"), calls)
    }

    @Test fun aFingerWithMouseButtonFlagsIsNotAPen() {
        assertFalse(stylus.onTouch(event(MotionEvent.ACTION_DOWN, 1f, 2f, tool = MotionEvent.TOOL_TYPE_FINGER,
            buttons = MotionEvent.BUTTON_STYLUS_PRIMARY)))
        assertTrue(calls.isEmpty())
    }

    private fun event(action: Int, x: Float, y: Float, tool: Int = MotionEvent.TOOL_TYPE_STYLUS, buttons: Int = 0): MotionEvent {
        val props = MotionEvent.PointerProperties().apply { id = 0; toolType = tool }
        val coords = MotionEvent.PointerCoords().apply { this.x = x; this.y = y }
        return MotionEvent.obtain(0L, 0L, action, 1, arrayOf(props), arrayOf(coords), 0, buttons, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN or InputDevice.SOURCE_STYLUS, 0)
    }
}
