package choliver.nespot.apu

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.Rational
import choliver.nespot.observable

class Timer(
  private val cyclesPerSample: Rational = CYCLES_PER_SAMPLE
) {
  private var pos = 0
  private var jump = cyclesPerSample.b
  var periodCycles by observable(1) { jump = it * cyclesPerSample.b }

  fun take(): Int {
    pos -= cyclesPerSample.a
    // TODO - replace this crap with some kind of division
    var ret = 0
    while (pos <= 0) {
      pos += jump
      ret++
    }
    return ret
  }

  fun restart() {
    pos = jump
  }
}
