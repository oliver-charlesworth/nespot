package choliver.nespot.apu

class Counter(private val cyclesPerSample: Rational = CYCLES_PER_SAMPLE) {
  private var pos = 0.toRational()
  var periodCycles by observable(1.toRational()) { pos = it } // TODO - can this be Int ?

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
