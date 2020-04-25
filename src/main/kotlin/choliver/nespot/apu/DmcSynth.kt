package choliver.nespot.apu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import java.lang.Integer.max
import java.lang.Integer.min

// TODO - timing of memory loads is probably important - what if content changes?
// TODO - interrupts
// TODO - what happens if values change mid-way?

// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcSynth(
  cyclesPerSample: Rational,
  private val memory: Memory
) : Synth {
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var numBits = 0
  private var offset = 0
  private var sample: Data = 0
  var loop: Boolean = false
  var address: Address = 0
  var length: Int = 0
  var level: Data = 0

  // TODO - update other synths to take this value directly
  var periodCycles: Rational = 0.toRational()
    set(value) {
      field = value
      counter.periodCycles = value
    }

  override fun take(ticks: Sequencer.Ticks): Int {
    val ret = level
    updateLevel()
    return ret
  }

  private fun updateLevel() {
    val counterTicks = counter.take()
    if (counterTicks != 0) {
      maybeLoadSample()
      maybePlaySample()
    }
  }

  private fun maybeLoadSample() {
    if ((numBits == 0) && (offset < length)) {
      sample = memory.load(address + offset)
      numBits = 8
      offset++
      if (loop && offset == length) {
        offset = 0
      }
    }
  }

  private fun maybePlaySample() {
    if (numBits != 0) {
      when (sample and 1) {
        1 -> level = min(level + 2, 125)
        0 -> level = max(level - 2, 2)
      }
      sample = sample shr 1
      numBits--
    }
  }

  companion object {
    val DMC_RATE_TABLE = listOf(
      428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    )
  }
}
