package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP

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
    val quarter: Int,
    val half: Int
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
            0 -> Ticks(quarter = 1, half = 1)
            1 -> Ticks(quarter = 1, half = 0)
            2 -> Ticks(quarter = 1, half = 1)
            3 -> Ticks(quarter = 1, half = 0)
            else -> throw IllegalStateException() // Should never happen
          }
        }
        FIVE_STEP -> {
          iSeq = (iSeq + 1) % 5
          when (iSeq) {
            0 -> Ticks(quarter = 1, half = 1)
            1 -> Ticks(quarter = 1, half = 0)
            2 -> Ticks(quarter = 1, half = 1)
            3 -> Ticks(quarter = 1, half = 0)
            4 -> Ticks(quarter = 0, half = 0)
            else -> throw IllegalStateException() // Should never happen
          }
        }
      }
    } else Ticks(quarter = 0, half = 0)
    val realRet = if (justReset && mode == FIVE_STEP) Ticks(1, 1) else ret
    justReset = false
    return realRet
  }


}
