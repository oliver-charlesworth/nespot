package choliver.nespot.apu

// TODO - model negate differently for pulse1 and pulse2
// TODO - channel muting
internal class Sweep(private val timer: Counter) {
  private var iDivider = 0
  private var reload = true
  var enabled = false
  var divider = 0
  var negate = false
  var shift = 0

  fun reset() {
    reload = true
  }

  fun advance() {
    if ((iDivider == 0) || reload) {
      iDivider = divider
      reload = false
    } else {
      iDivider--
    }
    if ((iDivider == 0) && enabled) {
      adjustPeriod()
    }
  }

  private fun adjustPeriod() {
    val period = timer.periodCycles.toInt()
    val delta = period shr shift
    val newPeriod = period + (if (negate) -delta else delta)
    if (newPeriod in 8..0x7FF) {
      timer.periodCycles = newPeriod.toRational()
    }
  }
}
