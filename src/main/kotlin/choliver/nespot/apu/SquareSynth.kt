package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class SquareSynth : Synth {
  private var iSeq = 0
  var haltLength = false
  var dutyCycle = 0
  override var length = 0

  override val output get() = if (length > 0) SEQUENCES[dutyCycle][iSeq] else 0

  override fun onTimer() {
    iSeq = (iSeq + 1) % SEQUENCE_LENGTH
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      length = max(length - 1, 0)
    }
  }

  companion object {
    private val SEQUENCES = listOf(
      listOf(0, 1, 0, 0, 0, 0, 0, 0),
      listOf(0, 1, 1, 0, 0, 0, 0, 0),
      listOf(0, 1, 1, 1, 1, 0, 0, 0),
      listOf(1, 0, 0, 1, 1, 1, 1, 1)
    )

    private const val SEQUENCE_LENGTH = 8
  }
}
