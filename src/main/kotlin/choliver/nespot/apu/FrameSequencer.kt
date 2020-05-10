package choliver.nespot.apu

import choliver.nespot.CYCLES_PER_SAMPLE
import choliver.nespot.Rational
import choliver.nespot.apu.FrameSequencer.Mode.FIVE_STEP
import choliver.nespot.apu.FrameSequencer.Mode.FOUR_STEP
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import choliver.nespot.observable

// TODO - interrupts
class FrameSequencer(
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

  private val fourStepPeriod = frameSequencerFourStepPeriodCycles / 4
  private val fiveStepPeriod = frameSequencerFiveStepPeriodCycles / 5 // TODO - fix the inaccuracy here

  private val timer = Timer(cyclesPerSample = cyclesPerSample).apply {
    periodCycles = fourStepPeriod
  }
  private var iSeq = 0
  private var justReset = false

  var mode by observable(FOUR_STEP) {
    timer.periodCycles = when (it) {
      FOUR_STEP -> fourStepPeriod
      FIVE_STEP -> fiveStepPeriod
    }
    timer.restart()
    iSeq = 0
    justReset = true
  }

  fun take(): Ticks {
    val ret = if (timer.take() == 1) {
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
