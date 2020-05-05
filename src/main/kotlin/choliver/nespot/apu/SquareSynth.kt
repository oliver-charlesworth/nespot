package choliver.nespot.apu

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class SquareSynth : Synth {
  private var duration = Duration()
  private var iSeq = 0
  var haltLength = false
  var dutyCycle = 0

  var length
    get() = duration.length
    set(value) { duration.length = value }

  override var enabled
    get() = duration.enabled
    set(value) { duration.enabled = value }

  override val hasRemainingOutput get() = duration.remaining > 0
  override val output get() = if (hasRemainingOutput) SEQUENCES[dutyCycle][iSeq] else 0

  override fun onTimer() {
    iSeq = (iSeq + 1) % SEQUENCE_LENGTH
  }

  override fun onHalfFrame() {
    if (!haltLength) {
     duration.decrement()
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
