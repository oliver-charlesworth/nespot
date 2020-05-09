package choliver.nespot.runner

import javafx.scene.input.KeyCode
import java.nio.IntBuffer

sealed class Event {
  class Audio(val buffer: FloatArray) : Event()
  class Video(val buffer: IntBuffer) : Event()
  data class KeyDown(val code: KeyCode) : Event()
  data class KeyUp(val code: KeyCode) : Event()
  object Close : Event()
}
