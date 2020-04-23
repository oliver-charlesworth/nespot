package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
class TriangleGenerator(
  linear: Int = 0,   // 7-bit
  length: Int = 0    // 5-bit
) {

  private val quarterFrameCounter = Counter(FRAME_SEQUENCER_PERIOD_CYCLES / 4.0)
  private val timerCounter = Counter()
  private var iSeq = 0
  private var iQuarterFrame = 0
  private var iLinear = linear
  private var iLength = LENGTH_TABLE[length]

  var linear: Int = 0
    set(value) {
      field = value
      iLinear = linear
    }

  var length: Int = 0
    set(value) {
      field = value
      iLength = LENGTH_TABLE[value]
    }

  var timer: Int = 0
    set(value) {
      field = value
      timerCounter.periodCpuCycles = (value + 1).toDouble()
    }


  fun generate(num: Int) = List(num) {
    val quarterTicks = quarterFrameCounter.update()
    updateCounters(quarterTicks)

    val seqTicks = timerCounter.update()
    updatePhase(seqTicks)

    SEQUENCE[iSeq]
  }

  private fun updateCounters(inc: Int) {
    iLinear = max(iLinear - inc, 0)
    iQuarterFrame += inc
    // TODO - handle multiple ticks
    if (iQuarterFrame == 2) {
      iQuarterFrame = 0
      iLength = max(iLength - 1, 0)
    }
  }

  private fun updatePhase(ticks: Int) {
    if ((iLinear != 0) && (iLength != 0)) {
      iSeq = (iSeq + ticks) % SEQUENCE.size
    }
  }

  companion object {
    // TODO - what is the offset here?  Does it matter if there's a DC blocking filter?
    private val SEQUENCE = listOf(
      15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
      0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
    )
  }
}
