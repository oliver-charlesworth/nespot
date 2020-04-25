package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
class TriangleSynth(cyclesPerSample: Rational) : Synth {
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iLinear = 0
  private var iLength = 0

  var linear: Int = 0
    set(value) {
      field = value
      iLinear = value
    }

  var length: Int = 0
    set(value) {
      field = value
      iLength = value
      iLinear = linear   // Resets the linear counter too
    }

  var timer: Int = 0
    set(value) {
      field = value
      counter.periodCycles = (value + 1).toRational()
    }

  override fun take(ticks: Sequencer.Ticks): Int {
    val ret = SEQUENCE[iSeq]
    updateCounters(ticks)
    updatePhase()
    return ret
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    iLinear = max(iLinear - ticks.quarter, 0)
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = counter.take()
    // Counters gate sequence generation, rather than muting the channel
    if ((iLinear != 0) && (iLength != 0)) {
      iSeq = (iSeq + ticks) % SEQUENCE_LENGTH
    }
  }

  companion object {
    private val SEQUENCE = listOf(
      15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
      0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
    )

    private const val SEQUENCE_LENGTH = 32
  }
}
