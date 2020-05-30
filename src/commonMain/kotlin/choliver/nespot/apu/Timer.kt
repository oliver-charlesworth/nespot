package choliver.nespot.apu

import choliver.nespot.Rational
import kotlin.math.max

class Timer(
  private val cyclesPerSample: Rational
) {
  private var pos = 0
  private var dec = cyclesPerSample.a
  private var inc = cyclesPerSample.b
  var periodCycles = MIN_PERIOD
    set(value) {
      field = value
      if (periodCycles >= MIN_PERIOD) {
        inc = value * cyclesPerSample.b
        dec = cyclesPerSample.a
      } else {
        inc = 1
        dec = 0
      }
    }

  fun advance(numCycles: Int): Int {
    pos -= numCycles
    val ticks = max(0, (periodCycles - pos) / periodCycles)
    pos += ticks * periodCycles
    return ticks
  }

  fun take(): Int {
    pos -= dec
    val ticks = max(0, (inc - pos) / inc)
    pos += ticks * inc
    return ticks
  }

  fun restart() {
    pos = inc
  }

  companion object {
    const val MIN_PERIOD = 3    // Prevents audio aliasing, divide-by-zero, and mitigates perf issues
  }
}
