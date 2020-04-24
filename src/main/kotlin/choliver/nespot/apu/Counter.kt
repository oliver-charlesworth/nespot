package choliver.nespot.apu

class Counter(
  private val cyclesPerSample: Double,
  var periodCycles: Double = 1.0 // TODO - can't be zero
) {
  private var pos = periodCycles

  fun take(): Int {
    pos -= cyclesPerSample  // TODO - this approach is bad for things that are definitely clean divisors of the CPU rate

    // TODO - do this with division please
    var ret = 0
    while (pos <= 0) {
      pos += periodCycles
      ret++
    }
    return ret
  }
}
