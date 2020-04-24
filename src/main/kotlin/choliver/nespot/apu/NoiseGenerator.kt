package choliver.nespot.apu

import kotlin.math.max

// http://wiki.nesdev.com/w/index.php/APU_Noise
class NoiseGenerator(cyclesPerSample: Rational) : Generator {
  private val timerCounter = Counter(cyclesPerSample = cyclesPerSample)
  private var iLength = 0
  private var sr = 0x0001

  var volume = 0
  var mode = 0

  var length: Int = 0
    set(value) {
      field = value
      iLength = LENGTH_TABLE[value]
    }

  var period: Int = 0
    set(value) {
      field = value
      timerCounter.periodCycles = PERIOD_TABLE[value].toRational()
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
    val ticks = timerCounter.take()
    if (ticks != 0) {
      val fb = (sr and 0x01) xor ((if (mode == 0) (sr shr 1) else (sr shr 6)) and 0x01)
      sr = (sr shr 1) or (fb shl 14)
    }
  }

  companion object {
    private val PERIOD_TABLE = listOf(
      4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
    )
  }
}
