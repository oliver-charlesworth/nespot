package choliver.nespot.apu

import kotlin.math.max

class Timer {
  private var pos = 0
  private var dec = 0
  var periodCycles = MIN_PERIOD
    set(value) {
      field = value
      dec = if (periodCycles >= MIN_PERIOD) 1 else 0
    }

  fun advance(numCycles: Int): Int {
    pos -= dec * numCycles
    val ticks = max(0, (periodCycles - pos) / periodCycles)
    pos += ticks * periodCycles
    return ticks
  }

  fun restart() {
    pos = periodCycles
  }

  companion object {
    const val MIN_PERIOD = 3    // Prevents audio aliasing, divide-by-zero, and mitigates perf issues
  }
}
