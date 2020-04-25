package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import kotlin.math.max

// See http://wiki.nesdev.com/w/index.php/APU_Pulse
class PulseSynth(cyclesPerSample: Rational = CYCLES_PER_SAMPLE) : Synth {
  private val counter = Counter(cyclesPerSample = cyclesPerSample)
  private var iSeq = 0
  private var iSweep = 0
  private var iLength = 0
  private var iDivider = 0
  private var iDecay = 0
  var sweepEnabled = false
  var sweepDivider by observable(0) { iSweep = it }
  var sweepNegate = false
  var sweepShift = 0
  var dutyCycle = 0
  var envLoop = false
  var envParam = 0    // TODO - should this reload iDivider?
  var directEnvMode = false
  var periodCycles by observable(1) {
    counter.periodCycles = it.toRational()
  }
  override var length by observable(0) {
    iLength = it
    iDecay = 15
    iDivider = envParam + 1
  }

  override fun take(ticks: Ticks): Int {
    val ret = SEQUENCES[dutyCycle][iSeq] * calcOutputLevel()
    updateEnvelope(ticks.quarter)
    updateSweep()
    updatePhase()
    updateCounters(ticks)
    return ret
  }

  private fun calcOutputLevel() = when {
    (iLength == 0) -> 0
    (periodCycles < 8 || periodCycles > 0x7FF) -> 0
    directEnvMode -> envParam
    else -> iDecay
  }

  private fun updateEnvelope(quarter: Boolean) {
    if (quarter) {
      when (iDivider) {
        0 -> {

        }
        else -> iDivider--

        }
      }

//    if (iDivider == 0) {
//      iDecay = max(iDecay - 1, 0)
//      if (iDecay == 0) {
//        iDecay = 15
//      }
//      iDivider = envParam + 1
//    }
  }

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

  private fun updateCounters(ticks: Ticks) {
    if (ticks.half) {
      iLength = max(iLength - 1, 0)
      iSweep = max(iSweep - 1, 0)
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
