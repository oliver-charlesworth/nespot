package choliver.nespot.apu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import java.lang.Integer.max
import java.lang.Integer.min

// TODO - timing of memory loads is probably important - what if content changes?
// TODO - interrupts

// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcSynth(private val memory: Memory) : Synth {
  private var numBitsRemaining = 0
  private var numBytesRemaining = 0
  private var sample: Data = 0
  var loop: Boolean = false
  var level: Data = 0
  var address: Address by observable(0x0000) { resetPattern() }

  override var length by observable(0) { resetPattern() }
  override val hasRemainingOutput get() = numBytesRemaining > 0
  override val output get() = level

  override fun onTimer() {
    maybeLoadSample()
    maybePlaySample()
  }

  private fun maybeLoadSample() {
    if ((numBitsRemaining == 0) && (numBytesRemaining > 0)) {
      sample = memory.load(address + length - numBytesRemaining)
      numBitsRemaining = 8
      numBytesRemaining--
      if (loop && numBytesRemaining == 0) {
        resetPattern()
      }
    }
  }

  private fun maybePlaySample() {
    if (numBitsRemaining != 0) {
      when (sample and 1) {
        1 -> level = min(level + 2, 125)
        0 -> level = max(level - 2, 2)
      }
      sample = sample shr 1
      numBitsRemaining--
    }
  }

  private fun resetPattern() {
    numBytesRemaining = length
  }
}
