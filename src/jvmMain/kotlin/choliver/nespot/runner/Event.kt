package choliver.nespot.runner

import choliver.nespot.nes.Joypads
import javafx.scene.input.KeyCode

sealed class Event {
  data class ControllerButtonDown(val button: Joypads.Button) : Event()
  data class ControllerButtonUp(val button: Joypads.Button) : Event()
  data class KeyDown(val code: KeyCode) : Event()
  data class KeyUp(val code: KeyCode) : Event()
  object Close : Event()
  data class Error(val cause: Exception) : Event()
}
