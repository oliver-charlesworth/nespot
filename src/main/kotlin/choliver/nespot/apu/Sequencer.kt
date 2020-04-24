package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational
import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.apu.Sequencer.Ticks

// TODO - interrupts
class Sequencer(
  cyclesPerSample: Rational,
  frameSequencerFourStepPeriodCycles: Int,
  frameSequencerFiveStepPeriodCycles: Int
) : Takeable<Ticks> {
  enum class Mode {
    FOUR_STEP,
    FIVE_STEP
  }

  data class Ticks(
    val quarter: Int,
    val half: Int
  )

  private val fourStepPeriod = rational(frameSequencerFourStepPeriodCycles, 4)
  private val fiveStepPeriod = rational(frameSequencerFiveStepPeriodCycles, 5)

  // 4-step
  //  7457
  //  14913
  //  22731
  //  29829
  //  29830 -> 0

  // 5-step
  //  7457
  //  14913
  //  22731
  //  29829
  //  37281
  //  37282 -> 0

  private val counter = Counter(
    cyclesPerSample = cyclesPerSample,
    periodCycles = fourStepPeriod
  )
  private var iSeq = 0

  var mode: Mode = FOUR_STEP
    set(value) {
      field = value
      counter.periodCycles = when (mode) {
        FOUR_STEP -> fourStepPeriod
        FIVE_STEP -> fiveStepPeriod
      }
      iSeq = 0
    }

  override fun take() = if (counter.take() == 1) {
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


}
