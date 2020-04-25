package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Triangle
internal class TriangleSynth : Synth {
  private var reload = false  // i.e. reload the linear counter
  private var iSeq = 0
  private var iLinear = 0
  private var iLength = 0
  var preventReloadClear = false
  var haltLength = false
  var linear = 0
  override var length by observable(0) { iLength = it; reload = true } // Reloads both counters

  override val output get() = SEQUENCE[iSeq]

  // Counters gate sequence generation, rather than muting the channel
  override fun onTimer() {
    if ((iLinear != 0) && (iLength != 0)) {
      iSeq = (iSeq + 1) % SEQUENCE_LENGTH
    }
  }

  override fun onQuarterFrame() {
    if (reload) {
      iLinear = linear

    } else if (iLinear > 0) {
      iLinear--
    }
    if (!preventReloadClear) {
      reload = false
    }
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      iLength = max(iLength - 1, 0)
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
