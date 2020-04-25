package choliver.nespot.apu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory

// TODO - tests
// TODO - timing of memory loads is probably important - what if content changes?
// TODO - looping
// TODO - interrupt
// TODO - stop playing at end of sample (and reset?)
// TODO - what happens if values change mid-way?

// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcSynth(
  cyclesPerSample: Rational,
  private val memory: Memory
) : Synth {
  var level = 0
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var addr: Address = 0
  private var _length = 0
  private var numBits = 0
  private var offset = 0
  private var bits: Data = 0

  var address: Int = 0
    set(value) {
      field = value
      addr = 0xC000 + (value shl 6)
    }

  var length: Int = 0
    set(value) {
      field = value
      _length = (value * 16) + 1
    }

  var rate: Int = 0
    set(value) {
      field = value
      counter.periodCycles = RATE_TABLE[value].toRational()
    }

  override fun take(ticks: Sequencer.Ticks): Int {
    val ret = level
    updateLevel()
    return ret
  }

  private fun updateLevel() {
    val counterTicks = counter.take()
    if ((counterTicks != 0) && (offset != length)) {
      if (numBits == 0) {
        bits = memory.load(address + offset)
        numBits = 8
        offset++
      }

      val b = bits and 1
      when {
        (b == 1) && (level <= 125) -> level += 2
        (b == 0) && (level >= 2) -> level -= 2
      }
      bits = bits shr 1
      numBits--
    }
  }

  companion object {
    private val RATE_TABLE = listOf(
      428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    )
  }
}
