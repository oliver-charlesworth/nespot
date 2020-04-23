package choliver.nespot.apu

class Counter(
  var periodCpuCycles: Double = 1.0 // TODO - can't be zero
) {
  private var pos = periodCpuCycles - 1

  fun update(): Int {
    pos -= CLOCKS_PER_SAMPLE  // TODO - this approach is bad for things that are definitely clean divisors of the CPU rate

    // TODO - do this with division please
    var ret = 0
    while (pos <= 0) {
      pos += periodCpuCycles
      ret++
    }
    return ret
  }
}
