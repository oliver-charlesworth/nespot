package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import java.awt.event.KeyEvent.*

sealed class KeyAction {
  object ToggleFullScreen : KeyAction()
  object Snapshot : KeyAction()
  object Restore : KeyAction()
  data class Joypad(val button: Button) : KeyAction()

  companion object {
    fun fromKeyCode(code: Int) = when (code) {
      VK_F -> ToggleFullScreen
      VK_S -> Snapshot
      VK_R -> Restore
      VK_Z -> Joypad(Button.A)
      VK_X -> Joypad(Button.B)
      VK_CLOSE_BRACKET -> Joypad(Button.START)
      VK_OPEN_BRACKET -> Joypad(Button.SELECT)
      VK_LEFT -> Joypad(Button.LEFT)
      VK_RIGHT -> Joypad(Button.RIGHT)
      VK_UP -> Joypad(Button.UP)
      VK_DOWN -> Joypad(Button.DOWN)
      else -> null
    }
  }
}
