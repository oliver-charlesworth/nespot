package choliver.nespot.playtest

import choliver.nespot.nes.Joypads

data class Scenario(
  val romHash: String,
  val stimuli: List<Stimulus>
) {
  sealed class Stimulus {
    abstract val timestamp: Long

    data class ButtonDown(override val timestamp: Long, val button: Joypads.Button) : Stimulus()
    data class ButtonUp(override val timestamp: Long, val button: Joypads.Button) : Stimulus()
    data class Snapshot(override val timestamp: Long, val bytes: List<Byte>) : Stimulus()
    data class Close(override val timestamp: Long) : Stimulus()
  }
}
