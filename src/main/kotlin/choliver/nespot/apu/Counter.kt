package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational

class Counter(private val cyclesPerSample: Rational) : Takeable<Int> {
  private var pos = 0.toRational()

  var periodCycles: Rational = rational(1)
    set(value) {
      field = value
      pos = value
    }

  override fun take(): Int {
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
