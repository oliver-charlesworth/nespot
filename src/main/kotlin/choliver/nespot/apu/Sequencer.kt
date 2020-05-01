package choliver.nespot.apu

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.Rational
import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1

// TODO - interrupts
class Sequencer(
  cyclesPerSample: Rational = CYCLES_PER_SAMPLE,
  frameSequencerFourStepPeriodCycles: Int = FRAME_SEQUENCER_4_STEP_PERIOD_CYCLES,
  frameSequencerFiveStepPeriodCycles: Int = FRAME_SEQUENCER_5_STEP_PERIOD_CYCLES
) {
  enum class Mode {
    FOUR_STEP,
    FIVE_STEP
  }

  data class Ticks(
    val quarter: Boolean,
    val half: Boolean
  )

  private val fourStepPeriod = Rational(frameSequencerFourStepPeriodCycles, 4)
  private val fiveStepPeriod = Rational(frameSequencerFiveStepPeriodCycles, 5)

  private val counter = Counter(cyclesPerSample = cyclesPerSample).apply {
    periodCycles = fourStepPeriod
  }
  private var iSeq = 0
  private var justReset = false

  var mode by observable(FOUR_STEP) {
    counter.periodCycles = when (it) {
      FOUR_STEP -> fourStepPeriod
      FIVE_STEP -> fiveStepPeriod
    }
    iSeq = 0
    justReset = true
  }

  fun take(): Ticks {
    val ret = if (counter.take() == 1) {
      when (mode) {
        FOUR_STEP -> {
          iSeq = (iSeq + 1) % 4
          when (iSeq) {
            0 -> Ticks(quarter = _1, half = _1)
            1 -> Ticks(quarter = _1, half = _0)
            2 -> Ticks(quarter = _1, half = _1)
            3 -> Ticks(quarter = _1, half = _0)
            else -> throw IllegalStateException() // Should never happen
          }
        }
        FIVE_STEP -> {
          iSeq = (iSeq + 1) % 5
          when (iSeq) {
            0 -> Ticks(quarter = _1, half = _1)
            1 -> Ticks(quarter = _1, half = _0)
            2 -> Ticks(quarter = _1, half = _1)
            3 -> Ticks(quarter = _1, half = _0)
            4 -> Ticks(quarter = _0, half = _0)
            else -> throw IllegalStateException() // Should never happen
          }
        }
      }
    } else Ticks(quarter = _0, half = _0)
    val realRet = if (justReset && mode == FIVE_STEP) Ticks(_1, _1) else ret
    justReset = false
    return realRet
  }


}
