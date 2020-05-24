package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.*

sealed class KeyAction {
  object ToggleFullScreen : KeyAction()
  object Snapshot : KeyAction()
  object Restore : KeyAction()
  data class Joypad(val button: Button) : KeyAction()

  companion object {
    fun fromKeyCode(code: KeyCode) = when (code) {
      F -> ToggleFullScreen
      S -> Snapshot
      R -> Restore
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