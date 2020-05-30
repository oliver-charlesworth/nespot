package choliver.nespot.apu

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
class TriangleSynth : Synth {
  private val lc = LengthCounter()
  private var reload = false  // i.e. reload the linear counter
  private var iSeq = 0
  private var linRemaining = 0
  var preventReloadClear = false
  var haltLength = false
  var linLength = 0

  var length
    get() = lc.length
    set(value) {
      lc.length = value
      reload = true  // Reloads both counters
    }

  override var enabled
    get() = lc.enabled
    set(value) { lc.enabled = value }

  override val outputRemaining get() = lc.remaining > 0   // Doesn't account for linear counter
  override val output get() = SEQUENCE[iSeq]

  // Counters gate sequence generation, rather than muting the channel
  override fun onTimer(num: Int) {
    if ((linRemaining > 0) && (lc.remaining > 0)) {
      iSeq = (iSeq + num) % SEQUENCE_LENGTH
    }
  }

  override fun onQuarterFrame() {
    if (reload) {
      linRemaining = linLength
    } else if (linRemaining > 0) {
      linRemaining--
    }
    if (!preventReloadClear) {
      reload = false
    }
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      lc.decrement()
    }
  }

  companion object {
    private val SEQUENCE = listOf(
      15, 14, 13, 12, 11, 10,  9,  8,  7,  6,  5,  4,  3,  2,  1,  0,
      0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15
    )

    private const val SEQUENCE_LENGTH = 32
  }
}
