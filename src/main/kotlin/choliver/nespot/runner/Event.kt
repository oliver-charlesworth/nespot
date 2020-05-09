package choliver.nespot.runner

import javafx.scene.input.KeyCode

sealed class Event {
  class Audio(val buffer: FloatArray) : Event()
  data class KeyDown(val code: KeyCode) : Event()
  data class KeyUp(val code: KeyCode) : Event()
  object Close : Event()
}
