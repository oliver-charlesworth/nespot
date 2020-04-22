package choliver.nespot.apu

class Counter(private val periodCpuCycles: Double) {
  private var pos = periodCpuCycles - 1
  private var residual = 0.0

  // TODO - handle case where multiple ticks per sample
  fun update(): Int {
    pos -= INT_CLOCKS_PER_SAMPLE
    residual += RESIDUAL_CLOCKS_PER_SAMPLE
    if (residual >= 1.0) {
      pos--
      residual -= 1.0
    }
    return if (pos <= 0) {
      pos += periodCpuCycles
      1
    } else {
      0
    }
  }
}
