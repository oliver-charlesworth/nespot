package choliver.nespot.ui

import choliver.nespot.nes.Joypads.Button

sealed class KeyAction {
  object ToggleFullScreen : KeyAction()
  data class Joypad(val button: Button) : KeyAction()

  companion object {
    fun fromKeyCode(code: String) = when (code) {
      "KeyF" -> ToggleFullScreen
      "KeyZ" -> Joypad(Button.A)
      "KeyX" -> Joypad(Button.B)
      "BracketRight" -> Joypad(Button.START)
      "BracketLeft" -> Joypad(Button.SELECT)
      "ArrowLeft" -> Joypad(Button.LEFT)
      "ArrowRight" -> Joypad(Button.RIGHT)
      "ArrowUp" -> Joypad(Button.UP)
      "ArrowDown" -> Joypad(Button.DOWN)
      else -> null
    }
  }
}
