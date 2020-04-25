package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseSynth : Synth {
  // Core state and params
  private var iSeq = 0
  private var iLength = 0
  var dutyCycle = 0
  override var length by observable(0) { iLength = it }

  override fun take(counterTicks: Int, seqTicks: Ticks): Int {
    val ret = if (outputEnabled()) SEQUENCES[dutyCycle][iSeq] else 0

    updatePhase(counterTicks)

    if (seqTicks.half) {
      iLength = max(iLength - 1, 0)
    }

    return ret
  }

  private fun outputEnabled() = (iLength > 0)

  private fun updatePhase(counterTicks: Int) {
    iSeq = (iSeq + counterTicks) % SEQUENCE_LENGTH
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
