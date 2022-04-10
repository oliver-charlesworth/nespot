package choliver.nespot.ui

import choliver.nespot.nes.Joypads.Button
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*

sealed class KeyAction {
  object SetController1 : KeyAction()
  object SetController2 : KeyAction()
  object ToggleFullScreen : KeyAction()
  object Reset : KeyAction()
  data class Joypad(val button: Button) : KeyAction()

  companion object {
    fun fromKeyCode(code: KeyCode) = when (code) {
      DIGIT1 -> SetController1
      DIGIT2 -> SetController2
      F -> ToggleFullScreen
      R -> Reset
      Z -> Joypad(Button.A)
      X -> Joypad(Button.B)
      CLOSE_BRACKET -> Joypad(Button.START)
      OPEN_BRACKET -> Joypad(Button.SELECT)
      LEFT -> Joypad(Button.LEFT)
      RIGHT -> Joypad(Button.RIGHT)
      UP -> Joypad(Button.UP)
      DOWN -> Joypad(Button.DOWN)
      else -> null
    }
  }
}
