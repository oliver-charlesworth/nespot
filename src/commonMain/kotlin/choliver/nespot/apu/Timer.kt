package choliver.nespot.apu

import kotlin.math.max

class Timer {
  private var pos = 0
  private var inc = 1
  private var dec = 0
  var periodCycles = MIN_PERIOD
    set(value) {
      field = value
      if (periodCycles >= MIN_PERIOD) {
        inc = periodCycles
        dec = 1
      } else {
        inc = 1
        dec = 0  // Disable if below MIN_PERIOD
      }
    }

  fun advance(numCycles: Int): Int {
    pos -= dec * numCycles
    val ticks = max(0, (inc - pos) / inc)
    pos += inc * ticks
    return ticks
  }

  fun restart() {
    pos = periodCycles
  }

  companion object {
    const val MIN_PERIOD = 3    // Prevents audio aliasing, divide-by-zero, and mitigates perf issues
  }
}
