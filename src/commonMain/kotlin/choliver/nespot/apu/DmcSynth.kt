package choliver.nespot.apu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import kotlin.math.max
import kotlin.math.min


// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcSynth(private val memory: Memory) : Synth {
  private var addrCurrent: Address = 0x0000
  private var numBitsRemaining = 0
  private var numBytesRemaining = 0
  private var sample: Data = 0
  private var _irq = false

  var irqEnabled = false
    set(value) {
      field = value
      if (!value) _irq = false
    }
  var loop = false
  var level: Data = 0
  var address: Address = 0x0000
  var length = 0

  override var enabled = false
    set(value) {
      field = value
      when {
        (value && (numBytesRemaining == 0)) -> restart()
        !value -> clear()
      }
      _irq = false
    }
  override val hasRemainingOutput get() = numBytesRemaining > 0
  override val output get() = level

  val irq get() = _irq

  override fun onTimer(num: Int) {
    for (i in 0 until num) {
      maybeLoadSample()
      maybePlaySample()
    }
  }

  private fun maybeLoadSample() {
    if ((numBitsRemaining == 0) && (numBytesRemaining > 0)) {
      sample = memory[addrCurrent]
      numBitsRemaining = 8

      if (addrCurrent == 0xFFFF) {
        addrCurrent = 0x8000
      } else {
        addrCurrent++
      }

      numBytesRemaining--
      if (numBytesRemaining == 0) {
        if (loop) {
          restart()
        } else if (irqEnabled) {
          _irq = true
        }
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

  fun restart() {
    numBytesRemaining = length
    addrCurrent = address
  }

  fun clear() {
    numBytesRemaining = 0
  }
}