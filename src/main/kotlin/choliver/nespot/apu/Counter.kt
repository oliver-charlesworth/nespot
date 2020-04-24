package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational

class Counter(
  private val cyclesPerSample: Rational,
  var periodCycles: Rational = rational(1) // TODO - can't be zero
) : Takeable<Int> {
  private var pos = periodCycles

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
