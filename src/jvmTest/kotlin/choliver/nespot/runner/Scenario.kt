package choliver.nespot.runner

import choliver.nespot.nes.Joypads

data class Scenario(
  val romHash: String,
  val stimuli: List<Stimulus>
) {
  sealed class Stimulus {
    abstract val timestamp: Long

    data class ButtonDown(override val timestamp: Long, val button: Joypads.Button) : Stimulus()
    data class ButtonUp(override val timestamp: Long, val button: Joypads.Button) : Stimulus()
    data class Snapshot(override val timestamp: Long, val pixels: List<Int>) : Stimulus()
    data class Close(override val timestamp: Long) : Stimulus()
  }
}
