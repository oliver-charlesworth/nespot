package choliver.nespot.apu

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class SquareSynth : Synth {
  private val lc = LengthCounter()
  private var iSeq = 0
  var haltLength = false
  var dutyCycle = 0

  var length
    get() = lc.length
    set(value) {
      lc.length = value
      iSeq = 0  // Restarts phase
    }

  override var enabled
    get() = lc.enabled
    set(value) { lc.enabled = value }

  override val hasRemainingOutput get() = lc.remaining > 0
  override val output get() = if (hasRemainingOutput) SEQUENCES[dutyCycle][iSeq] else 0

  override fun onTimer() {
    iSeq = (iSeq + 1) % SEQUENCE_LENGTH
  }

  override fun onHalfFrame() {
    if (!haltLength) {
     lc.decrement()
    }
  }

  companion object {
    private val SEQUENCES = listOf(
      listOf(0, 1, 0, 0, 0, 0, 0, 0),
      listOf(0, 1, 1, 0, 0, 0, 0, 0),
      listOf(0, 1, 1, 1, 1, 0, 0, 0),
      listOf(1, 0, 0, 1, 1, 1, 1, 1)
    )

    private const val SEQUENCE_LENGTH = 8
  }
}
