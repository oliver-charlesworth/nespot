package choliver.nespot.apu

import observable

class Counter(private val cyclesPerSample: Rational) {
  private var pos = 0.toRational()
  var periodCycles by observable(0.toRational()) { pos = it }

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
}
