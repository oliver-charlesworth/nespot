package choliver.nespot.ui

import choliver.nespot.dom.Gamepad
import choliver.nespot.dom.GamepadEvent
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Joypads.Button.*
import choliver.nespot.ui.Event.GamepadButtonDown
import choliver.nespot.ui.Event.GamepadButtonUp
import kotlin.browser.window

class GamepadManager(
  private val onEvent: (e: Event) -> Unit = {}
) {
  private val gamepads = mutableMapOf<Int, Gamepad>()

  private val buttons = mapOf(
    0 to ButtonManager(B),
    1 to ButtonManager(A),
    8 to ButtonManager(SELECT),
    9 to ButtonManager(START),
    12 to ButtonManager(UP),
    13 to ButtonManager(DOWN),
    14 to ButtonManager(LEFT),
    15 to ButtonManager(RIGHT)
  )

  init {
    window.addEventListener("gamepadconnected", { e ->
      e as GamepadEvent
      gamepads[e.gamepad.index] = e.gamepad
    })
    window.addEventListener("gamepaddisconnected", { e ->
      e as GamepadEvent
      gamepads.remove(e.gamepad.index)
    })
    window.setInterval(::poll, 1)
  }

  private fun poll() {
    gamepads.values.forEach { gamepad ->
      buttons.forEach { (k, v) ->
        v.update(gamepad.buttons[k].pressed)
      }
    }
  }

  inner class ButtonManager(private val button: Button) {
    private var prev = false

    fun update(current: Boolean) {
      if (prev != current) {
        when (current) {
          true -> onEvent(GamepadButtonDown(button))
          false -> onEvent(GamepadButtonUp(button))
        }
        prev = current
      }
    }
  }
}
