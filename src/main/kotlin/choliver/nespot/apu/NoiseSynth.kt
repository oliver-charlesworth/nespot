package choliver.nespot.apu

import kotlin.math.max

// http://wiki.nesdev.com/w/index.php/APU_Noise
class NoiseSynth(cyclesPerSample: Rational = CYCLES_PER_SAMPLE) : Synth {
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var iLength = 0
  private var sr = 0x0001

  var volume = 0
  var mode = 0

  var length: Int = 0
    set(value) {
      field = value
      iLength = value
    }

  var periodCycles: Rational = 0.toRational()
    set(value) {
      field = value
      counter.periodCycles = value
    }

  override fun take(ticks: Sequencer.Ticks): Int {
    val ret = if (iLength != 0) ((sr and 1) * volume) else 0
    updateCounters(ticks)
    updatePhase()
    return ret
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = counter.take()
    if (ticks != 0) {
      val fb = (sr and 0x01) xor ((if (mode == 0) (sr shr 1) else (sr shr 6)) and 0x01)
      sr = (sr shr 1) or (fb shl 14)
    }
  }
}
