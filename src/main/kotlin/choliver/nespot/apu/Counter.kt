package choliver.nespot.apu

class Counter(private val cyclesPerSample: Rational) {
  private var pos = 0.toRational()

  var periodCycles: Rational = Rational(1, 1)
    set(value) {
      field = value
      pos = value
    }

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
