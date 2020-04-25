package choliver.nespot.apu

import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseSynth(cyclesPerSample: Rational) : Synth {
  private val timerCounter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iLength = 0

  var dutyCycle = 0
  var volume = 0

  var length: Int = 0
    set(value) {
      field = value
      iLength = value
    }

  var timer: Int = 0
    set(value) {
      field = value
      timerCounter.periodCycles = (value + 1).toRational() * 2 // APU clock rather than CPU clock
    }

  override fun take(ticks: Sequencer.Ticks): Int {
    val ret = if (iLength != 0) (SEQUENCES[dutyCycle][iSeq] * volume) else 0
    updateCounters(ticks)
    updatePhase()
    return ret
  }

  private fun updateCounters(ticks: Sequencer.Ticks) {
    iLength = max(iLength - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = timerCounter.take()
    iSeq = (iSeq + ticks) % SEQUENCE_LENGTH
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
