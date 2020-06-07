package choliver.nespot.ui

import choliver.nespot.nes.Joypads
import javafx.scene.input.KeyCode

sealed class Event {
  data class GamepadButtonDown(val button: Joypads.Button) : Event()
  data class GamepadButtonUp(val button: Joypads.Button) : Event()
  data class KeyDown(val code: KeyCode) : Event()
  data class KeyUp(val code: KeyCode) : Event()
  object Close : Event()
  data class Error(val cause: Exception) : Event()
}
