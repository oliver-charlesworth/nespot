package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational

class Counter(
  private val cyclesPerSample: Rational,
  var periodCycles: Rational = rational(1) // TODO - can't be zero
) {
  private var pos = periodCycles

  fun take(): Int {
    pos -= cyclesPerSample
    return if (pos <= 0) {
      val ret = (-pos / periodCycles).toInt()
      pos += periodCycles * ret
      ret
    } else 0
  }
}
