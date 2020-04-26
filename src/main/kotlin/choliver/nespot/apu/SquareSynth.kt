package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class SquareSynth : Synth {
  private var iSeq = 0
  private var iLength = 0
  var haltLength = false
  var dutyCycle = 0

  override var length by observable(0) { iLength = it }
  override val hasRemainingOutput get() = iLength > 0
  override val output get() = if (hasRemainingOutput) SEQUENCES[dutyCycle][iSeq] else 0

  override fun onTimer() {
    iSeq = (iSeq + 1) % SEQUENCE_LENGTH
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      iLength = max(iLength - 1, 0)
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
