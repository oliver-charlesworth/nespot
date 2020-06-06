package choliver.nespot.apu

import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.memory.Memory
import kotlin.math.max
import kotlin.math.min

// See http://wiki.nesdev.com/w/index.php/APU_DMC
class DmcSynth(private val memory: Memory) : Synth {
  private val memoryUnit = MemoryUnit()
  private val outputUnit = OutputUnit()
  private var sample: Data? = null
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
        (value && (memoryUnit.numRemaining == 0)) -> memoryUnit.restart()
        !value -> memoryUnit.clear()
      }
      _irq = false
    }
  override val outputRemaining get() = memoryUnit.numRemaining > 0
  override val output get() = level

  val irq get() = _irq

  override fun onTimer(num: Int) {
    for (i in 0 until num) {
      memoryUnit.update()
      outputUnit.update()
    }
  }

  private inner class MemoryUnit {
    var numRemaining = 0
    private var addrCurrent: Address = 0x0000

    fun update() {
      if ((sample == null) && (numRemaining > 0)) {
        sample = memory[addrCurrent]

        addrCurrent = when (addrCurrent) {
          0xFFFF -> 0x8000
          else -> (addrCurrent + 1)
        }

        if (--numRemaining == 0) {
          when {
            loop -> restart()
            irqEnabled -> _irq = true
          }
        }
      }
    }

    fun restart() {
      numRemaining = length
      addrCurrent = address
    }

    fun clear() {
      numRemaining = 0
    }
  }

  private inner class OutputUnit {
    private var silence = true
    private var sr: Data = 0
    private var numRemaining = 8

    fun update() {
      if (!silence) {
        when (sr and 1) {
          1 -> level = min(level + 2, 125)
          0 -> level = max(level - 2, 2)
        }
        sr = sr shr 1
      }

      if (--numRemaining == 0) {
        numRemaining = 8
        if (sample != null) {
          silence = false
          sr = sample!!
          sample = null
        } else {
          silence = true
        }
      }
    }
  }
}
