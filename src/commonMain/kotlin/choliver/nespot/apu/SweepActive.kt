package choliver.nespot.apu

class SweepActive(
  private val timer: Timer,
  private val negateWithOnesComplement: Boolean
) : Sweep {
  private var iDivider = 0
  private var reload = true
  var enabled = false
  var divider = 0
  var negate = false
  var shift = 0

  override val mute get() = (currentPeriod() < MIN_PERIOD) || (targetPeriod() > MAX_PERIOD)

  fun restart() {
    reload = true
  }

  override fun advance() {
    if ((iDivider == 0) || reload) {
      iDivider = divider
      reload = false
    } else {
      iDivider--
    }
    if ((iDivider == 0) && enabled && !mute && (shift != 0)) {
      timer.periodCycles = targetPeriod()
    }
  }

  private fun targetPeriod(): Int {
    val period = currentPeriod()
    val delta = period shr shift
    return period + when {
      !negate -> delta
      negateWithOnesComplement -> -delta - 1
      else -> -delta
    }
  }

  private fun currentPeriod() = timer.periodCycles

  companion object {
    // Inclusive, and * 2 because normalised to CPU (rather than APU) cycles
    private const val MIN_PERIOD = 8 * 2
    private const val MAX_PERIOD = 0x7FF * 2
  }
}
