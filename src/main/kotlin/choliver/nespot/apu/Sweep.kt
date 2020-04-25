package choliver.nespot.apu

class Sweep(
  private val get: () -> Int,
  private val set: (Int) -> Unit
) {
  private var iDivider = 0
  var enabled = false
  var divider by observable(0) { iDivider = it }
  var negate = false
  var shift = 0

  fun advance() {
    if (iDivider == 0 && enabled) {
      val period = get()
      val delta = period shr shift
      val newPeriod = period + (if (negate) -delta else delta)
      // TODO - unify this between here and PulseSynth
      if (newPeriod in 8..0x7FF) {
        set(newPeriod)
        iDivider = divider
      }
    }
  }
}
