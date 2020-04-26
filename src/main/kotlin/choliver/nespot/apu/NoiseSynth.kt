package choliver.nespot.apu

import kotlin.math.max

// http://wiki.nesdev.com/w/index.php/APU_Noise
class NoiseSynth : Synth {
  private var iLength = 0
  private var sr = 0x0001
  var haltLength = false
  var mode = 0

  override var length by observable(0) { iLength = it }
  override val hasRemainingOutput get() = iLength > 0
  override val output get() = if (hasRemainingOutput) (sr and 1) else 0

  override fun onTimer() {
    val fb = (sr and 0x01) xor ((if (mode == 0) (sr shr 1) else (sr shr 6)) and 0x01)
    sr = (sr shr 1) or (fb shl 14)
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      iLength = max(iLength - 1, 0)
    }
  }
}
