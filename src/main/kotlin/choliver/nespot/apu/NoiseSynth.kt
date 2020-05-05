package choliver.nespot.apu

// http://wiki.nesdev.com/w/index.php/APU_Noise
class NoiseSynth : Synth {
  private val lc = LengthCounter()
  private var sr = 0x0001

  var length
    get() = lc.length
    set(value) { lc.length = value }

  override var enabled
    get() = lc.enabled
    set(value) { lc.enabled = value }

  override val hasRemainingOutput get() = lc.remaining > 0
  override val output get() = if (hasRemainingOutput) (sr and 1) else 0

  var haltLength = false
  var mode = 0

  override fun onTimer() {
    val fb = (sr and 0x01) xor ((if (mode == 0) (sr shr 1) else (sr shr 6)) and 0x01)
    sr = (sr shr 1) or (fb shl 14)
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      lc.decrement()
    }
  }
}
