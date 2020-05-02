package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.runner.KeyAction.*
import javafx.scene.input.KeyCode.*

sealed class KeyAction {
  object ToggleFullScreen : KeyAction()
  object Snapshot : KeyAction()
  data class Joypad(val button: Button) : KeyAction()
}

val KEY_MAPPINGS = mapOf(
  F to ToggleFullScreen,
  S to Snapshot,
  Z to Joypad(Button.A),
  X to Joypad(Button.B),
  CLOSE_BRACKET to Joypad(Button.START),
  OPEN_BRACKET to Joypad(Button.SELECT),
  LEFT to Joypad(Button.LEFT),
  RIGHT to Joypad(Button.RIGHT),
  UP to Joypad(Button.UP),
  DOWN to Joypad(Button.DOWN)
)
