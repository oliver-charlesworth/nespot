package choliver.nespot.apu

// TODO - model negate differently for pulse1 and pulse2
class Sweep(private val timer: Counter) {
  private var iDivider = 0
  private var reload = true
  var enabled = false
  var divider = 0
  var negate = false
  var shift = 0
  var inhibitMute = false

  val mute get() = !inhibitMute && ((currentPeriod() < MIN_PERIOD) || (targetPeriod() > MAX_PERIOD))

  fun restart() {
    reload = true
  }

  fun advance() {
    if ((iDivider == 0) || reload) {
      iDivider = divider
      reload = false
    } else {
      iDivider--
    }
    if ((iDivider == 0) && enabled && !mute) {
      timer.periodCycles = targetPeriod()
    }
  }

  private fun targetPeriod(): Int {
    val period = currentPeriod()
    val delta = period shr shift
    return period + (if (negate) -delta else delta)
  }

  private fun currentPeriod() = timer.periodCycles

  companion object {
    // Inclusive, and * 2 because normalised to CPU (rather than APU) cycles
    private const val MIN_PERIOD = 8 * 2
    private const val MAX_PERIOD = 0x7FF * 2
  }
}
