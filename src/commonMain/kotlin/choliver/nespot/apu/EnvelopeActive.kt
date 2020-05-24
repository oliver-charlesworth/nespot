package choliver.nespot.apu

class EnvelopeActive : Envelope {
  private var iDivider = 0
  private var iDecay = 0
  private var start = true
  var loop = false
  var param = 0
  var directMode = false

  override val level get() = if (directMode) param else iDecay

  fun restart() {
    start = true
  }

  override fun advance() {
    if (start) {
      start = false
      iDivider = param
      iDecay = DECAY_INIT
    } else {
      when (iDivider) {
        0 -> {
          iDivider = param
          iDecay = when {
            (iDecay > 0) -> iDecay - 1
            loop -> DECAY_INIT
            else -> iDecay
          }
        }
        else -> iDivider--
      }
    }
  }

  companion object {
    private const val DECAY_INIT = 15
  }
}
