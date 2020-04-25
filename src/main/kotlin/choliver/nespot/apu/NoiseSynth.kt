package choliver.nespot.apu

import kotlin.math.max

// http://wiki.nesdev.com/w/index.php/APU_Noise
class NoiseSynth : Synth {
  private var iLength = 0
  private var sr = 0x0001
  var mode = 0
  override var length by observable(0) { iLength = it }

  override fun take(counterTicks: Int, seqTicks: Sequencer.Ticks): Int {
    val ret = if (iLength != 0) (sr and 1) else 0
    updateCounters(seqTicks)
    updatePhase(counterTicks)
    return ret
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    if (ticks.half) {
      iLength = max(iLength - 1, 0)
    }
  }

  private fun updatePhase(counterTicks: Int) {
    if (counterTicks != 0) {
      val fb = (sr and 0x01) xor ((if (mode == 0) (sr shr 1) else (sr shr 6)) and 0x01)
      sr = (sr shr 1) or (fb shl 14)
    }
  }
}
