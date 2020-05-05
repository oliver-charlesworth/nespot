package choliver.nespot.apu

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.Rational
import choliver.nespot.observable
import choliver.nespot.toRational

class Counter(
  private val cyclesPerSample: Rational = CYCLES_PER_SAMPLE
) {
  private var pos = 0.toRational()
  var periodCycles = 1.toRational() // TODO - can this be Int ?

  fun take(): Int {
    pos -= cyclesPerSample
    // TODO - replace this crap with some kind of division
    var ret = 0
    while (pos <= 0) {
      pos += periodCycles
      ret++
    }
    return ret
  }

  fun restart() {
    pos = periodCycles
  }
}
