package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseGenerator {
  private val sequencer = Sequencer()
  private val timerCounter = Counter()
  private var iSeq = 0
  private var iLength = 0

  var dutyCycle = 0
  var volume = 0

  var length: Int = 0
    set(value) {
      field = value
      iLength = LENGTH_TABLE[value]
    }

  var timer: Int = 0
    set(value) {
      field = value
      timerCounter.periodCpuCycles = (value + 1).toDouble()
    }

  fun generate(num: Int) = List(num) {
    updateCounters()
    updatePhase()
    SEQUENCES[dutyCycle][iSeq] * volume
  }

  private fun updateCounters() {
    val ticks = sequencer.update()
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = timerCounter.update()
    if (iLength != 0) {
      iSeq = (iSeq + ticks) % SEQUENCE_LENGTH
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
