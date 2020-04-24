package choliver.nespot.apu

class Counter(
  private val cyclesPerSample: Rational,
  var periodCycles: Rational = Rational(1) // TODO - can't be zero
) {
  private var pos = periodCycles

  fun take(): Int {
    pos -= cyclesPerSample

    // TODO - do this with division please
    var ret = 0
    while (pos <= 0) {
      pos += periodCycles
      ret++
    }
    return ret
  }
}
