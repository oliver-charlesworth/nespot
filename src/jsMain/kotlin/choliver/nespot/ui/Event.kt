package choliver.nespot.ui

import choliver.nespot.nes.Joypads

sealed class Event {
  data class GamepadButtonDown(val button: Joypads.Button) : Event()
  data class GamepadButtonUp(val button: Joypads.Button) : Event()
}
