package com.droiddeck.launcher.input

import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class TouchpadGesturesTest {
    private val buttons = mutableListOf<String>()
    private val pad = TouchpadGestures(10f, object : PointerGestures.Listener {
        override fun onMove(x: Float, y: Float) {}
        override fun onButton(button: Int, pressed: Boolean, x: Float, y: Float) {
            buttons += "${if (button == PointerGestures.BTN_RIGHT) "right" else "left"} ${if (pressed) "down" else "up"}"
        }
        override fun onWheel(steps: Int) { buttons += "wheel $steps" }
        override fun onLongPress() {}
    }).apply { bounds = android.graphics.RectF(0f, 0f, 1000f, 600f); place(500f, 300f) }

    private val rightClick = listOf("right down", "right up")

    @Test fun twoFingerTapSecondFingerLiftingFirstIsARightClick() {
        down(1000, 0 to (400f to 300f))
        pointerDown(1040, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        pointerUp(1120, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        up(1150, 0 to (400f to 300f))
        assertEquals(rightClick, buttons)
    }

    @Test fun twoFingerTapFirstFingerLiftingFirstIsARightClick() {
        down(1000, 0 to (400f to 300f))
        pointerDown(1030, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        pointerUp(1110, 0, 0 to (400f to 300f), 1 to (520f to 310f))
        up(1140, 1 to (520f to 310f))
        assertEquals(rightClick, buttons)
    }

    @Test fun twoFingerTapWithSmallJitterIsStillARightClick() {
        down(1000, 0 to (400f to 300f))
        pointerDown(1030, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        move(1060, 0 to (403f to 304f), 1 to (523f to 316f))
        pointerUp(1110, 1, 0 to (403f to 304f), 1 to (523f to 316f))
        up(1140, 0 to (403f to 304f))
        assertEquals(rightClick, buttons)
    }

    @Test fun aSlowSecondFingerIsNotARightClick() {
        down(1000, 0 to (400f to 300f))
        pointerDown(1030, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        pointerUp(1500, 1, 0 to (400f to 300f), 1 to (520f to 310f))
        up(1700, 0 to (400f to 300f))
        assertEquals(emptyList<String>(), buttons)
    }

    @Test fun twoFingersScrollingIsNotARightClick() {
        down(1000, 0 to (400f to 300f))
        pointerDown(1030, 1, 0 to (400f to 300f), 1 to (520f to 300f))
        move(1060, 0 to (400f to 380f), 1 to (520f to 380f))
        pointerUp(1110, 1, 0 to (400f to 380f), 1 to (520f to 380f))
        up(1140, 0 to (400f to 380f))
        assertFalse(buttons.contains("right down"))
        assertTrue(buttons.any { it.startsWith("wheel") })
    }

    @Test fun oneFingerTapIsALeftClick() {
        down(1000, 0 to (400f to 300f))
        up(1080, 0 to (400f to 300f))
        assertEquals(listOf("left down", "left up"), buttons)
    }

    private fun down(t: Long, vararg p: Pair<Int, Pair<Float, Float>>) = send(t, MotionEvent.ACTION_DOWN, *p)
    private fun up(t: Long, vararg p: Pair<Int, Pair<Float, Float>>) = send(t, MotionEvent.ACTION_UP, *p)
    private fun move(t: Long, vararg p: Pair<Int, Pair<Float, Float>>) = send(t, MotionEvent.ACTION_MOVE, *p)
    private fun pointerDown(t: Long, index: Int, vararg p: Pair<Int, Pair<Float, Float>>) =
        send(t, MotionEvent.ACTION_POINTER_DOWN or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), *p)
    private fun pointerUp(t: Long, index: Int, vararg p: Pair<Int, Pair<Float, Float>>) =
        send(t, MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), *p)

    private fun send(t: Long, action: Int, vararg p: Pair<Int, Pair<Float, Float>>) {
        val props = p.map { (id, _) -> MotionEvent.PointerProperties().apply { this.id = id; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = p.map { (_, xy) -> MotionEvent.PointerCoords().apply { x = xy.first; y = xy.second } }
        val event = MotionEvent.obtain(1000L, t, action, p.size, props.toTypedArray(), coords.toTypedArray(),
            0, 0, 1f, 1f, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0)
        pad.onTouch(event)
        event.recycle()
    }
}
