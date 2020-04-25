package choliver.nespot.apu

class Envelope {
  private var iDivider = 0
  private var iDecay = 0
  private var start = false
  var loop = false
  var param = 0
  var directMode = false

  val level get() = if (directMode) param else iDecay

  fun advance() {
    if (start) {
      start = false
      iDivider = param
      iDecay = DECAY_INIT
    } else {
      when (iDivider) {
        0 -> {
          iDivider = param
          iDecay = if (loop) DECAY_INIT else (iDecay - 1)
        }
        else -> iDivider--
      }
    }
  }

  fun reset() {
    start = true
  }

  companion object {
    private const val DECAY_INIT = 15
  }
}
