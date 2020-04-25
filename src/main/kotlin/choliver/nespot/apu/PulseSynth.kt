package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import kotlin.math.max

// TODO - mute output if period < 8 or > 7FF

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseSynth(cyclesPerSample: Rational = CYCLES_PER_SAMPLE) : Synth {
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iSweep = 0
  private var iLength = 0
  var sweepEnabled: Boolean = false
  var sweepDivider by observable(0) { iSweep = it }
  var sweepNegate: Boolean = false
  var sweepShift: Int = 0
  var dutyCycle = 0
  var volume = 0
  var periodCycles by observable(1.toRational()) { counter.periodCycles = it }
  override var length by observable(0) { iLength = it }

  override fun take(ticks: Ticks): Int {
    val ret = if (iLength != 0) (SEQUENCES[dutyCycle][iSeq] * volume) else 0
    updateCounters(ticks)
    updateSweep()
    updatePhase()
    return ret
  }

  private fun updateSweep() {
    if (iSweep == 0 && sweepEnabled) {
//      val delta = periodCycles / (1 shl sweepShift)
//      periodCycles += if (sweepNegate) -delta else delta
//      iSweep = sweepDivider
    }
  }

  private fun updateCounters(ticks: Ticks) {
    iLength = max(iLength - ticks.half, 0)
    iSweep = max(iSweep - ticks.half, 0)
  }

  private fun updatePhase() {
    val ticks = counter.take()
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
