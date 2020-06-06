package choliver.nespot.apu

import choliver.nespot.common.isBitSet

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

  override val outputRemaining get() = lc.remaining > 0
  override val output get() = if (outputRemaining) (sr and 1) else 0

  var haltLength = false
  var mode = 0

  override fun onTimer(num: Int) {
    // No point running this faster than 1 tick per sample, so run at most once
    if (num > 0) {
      val tap = if (mode == 0) 1 else 6
      val fb = (sr xor (sr shr tap)).isBitSet(0)
      sr = (sr shr 1) or (if (fb) (1 shl 14) else 0)
    }
  }

  override fun onHalfFrame() {
    if (!haltLength) {
      lc.decrement()
    }
  }
}
