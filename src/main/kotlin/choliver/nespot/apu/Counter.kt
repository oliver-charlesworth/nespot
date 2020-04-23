package choliver.nespot.apu

class Counter {
  var periodCpuCycles: Int = 1
  private var pos = periodCpuCycles - 1
  private var residual = 0.0


  fun update(): Int {
//    println("period = ${periodCpuCycles}, pos = ${pos}")

    pos -= INT_CLOCKS_PER_SAMPLE
    residual += RESIDUAL_CLOCKS_PER_SAMPLE
    if (residual >= 1.0) {
      pos--
      residual -= 1.0
    }

    // TODO - do this with division please
    var ret = 0
    while (pos <= 0) {
      pos += periodCpuCycles
      ret++
    }
    return ret
  }
}
