package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button

sealed class BrowserKeyAction {
  object ToggleFullScreen : BrowserKeyAction()
  data class Joypad(val button: Button) : BrowserKeyAction()

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
