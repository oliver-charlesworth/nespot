package choliver.nespot.runner

import choliver.nespot.nes.Joypads
import java.nio.IntBuffer


sealed class Event {
  class Audio(val buffer: FloatArray) : Event()
  class Video(val buffer: IntBuffer) : Event()
  data class ControllerButtonDown(val button: Joypads.Button) : Event()
  data class ControllerButtonUp(val button: Joypads.Button) : Event()
  data class KeyDown(val code: Int) : Event()
  data class KeyUp(val code: Int) : Event()
  object Close : Event()
  data class Error(val cause: Exception) : Event()
}
