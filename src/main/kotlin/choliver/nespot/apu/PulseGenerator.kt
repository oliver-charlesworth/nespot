package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseGenerator(cyclesPerSample: Rational) : Generator {
  private val timerCounter = Counter(cyclesPerSample = cyclesPerSample)
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
      timerCounter.periodCycles = (value + 1).toRational()
    }

  override fun generate(ticks: Sequencer.Ticks): Int {
    updateCounters(ticks)
    updatePhase()
    return SEQUENCES[dutyCycle][iSeq] * volume
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = timerCounter.take()
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
