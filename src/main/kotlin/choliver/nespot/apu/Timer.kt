package choliver.nespot.apu

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.Rational
import kotlin.math.max

class Timer(
  private val cyclesPerSample: Rational = CYCLES_PER_SAMPLE
) {
  private var pos = 0
  private var jump = cyclesPerSample.b
  var periodCycles = 1
    set(value) {
      field = value
      jump = value * cyclesPerSample.b
    }

  fun take(): Int {
    pos -= cyclesPerSample.a
    val ticks = max(0, (jump - pos) / jump)
    pos += ticks * jump
    return ticks
  }

  fun restart() {
    pos = jump
  }
}
