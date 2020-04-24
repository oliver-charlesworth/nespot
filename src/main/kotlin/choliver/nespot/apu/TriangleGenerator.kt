package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
class TriangleGenerator(cyclesPerSample: Double) : Generator {
  private val timerCounter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iLinear = 0
  private var iLength = 0
  private var linearY = 0   // TODO - needs a better name

  var linear: Int = 0
    set(value) {
      field = value
      linearY = value
      iLinear = value
    }

  var length: Int = 0
    set(value) {
      field = value
      iLength = LENGTH_TABLE[value]
      iLinear = linearY   // Resets the linear counter too
    }

  var timer: Int = 0
    set(value) {
      field = value
      timerCounter.periodCycles = (value + 1).toDouble()
    }


  override fun generate(ticks: Sequencer.Ticks): Int {
    updateCounters(ticks)
    updatePhase()
    return SEQUENCE[iSeq]
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    iLinear = max(iLinear - ticks.quarter, 0)
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = timerCounter.take()
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
