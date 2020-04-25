package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseSynth(cyclesPerSample: Rational = CYCLES_PER_SAMPLE) : Synth {
  // Sweep state and params
  private var iSweep = 0
  var sweepEnabled = false
  var sweepDivider by observable(0) { iSweep = it }
  var sweepNegate = false
  var sweepShift = 0

  // Core state and params
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iLength = 0
  var dutyCycle = 0
  var periodCycles by observable(1) { counter.periodCycles = it.toRational() }
  override var length by observable(0) { iLength = it }

  override fun take(ticks: Ticks): Int {
    val ret = if (outputEnabled()) SEQUENCES[dutyCycle][iSeq] else 0

    updateSweep()
    updatePhase()

    if (ticks.half) {
      iLength = max(iLength - 1, 0)
      iSweep = max(iSweep - 1, 0)
    }

    return ret
  }

  private fun outputEnabled() = (iLength > 0) && (periodCycles in 8..0x7FF)

  private fun updateSweep() {
    if (iSweep == 0 && sweepEnabled) {
      val delta = periodCycles shr sweepShift
      val newPeriod = periodCycles + (if (sweepNegate) -delta else delta)
      if (newPeriod in 8..0x7FF) {
        periodCycles = newPeriod
        iSweep = sweepDivider
      }
    }
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
