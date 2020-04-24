package choliver.nespot.apu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory

// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcGenerator(
  private val memory: Memory
) : Generator {
  var level = 0
  private val timerCounter = Counter()
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
      timerCounter.periodCpuCycles = RATE_TABLE[value].toDouble()
    }

  override fun generate(ticks: Sequencer.Ticks): Int {
    val counterTicks = timerCounter.update()
    if ((counterTicks != 0) && (offset != _length)) {
      if (numBits == 0) {
        bits = memory.load(addr + offset)
        numBits = 8
        offset++
      }

      val b = bits and 1
      if (b == 1) {
        if (level <= 125) {
          level += 2
        }
      } else {
        if (level >= 2) {
          level -= 2
        }
      }

      bits = bits shr 1
      numBits--
    }
    return level
  }

  companion object {
    private val RATE_TABLE = listOf(
      428, 380, 340, 320, 286, 254, 226, 214, 190, 160, 142, 128, 106,  84,  72,  54
    )
  }
}
