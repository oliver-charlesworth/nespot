package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SequencerTest {
  private val sequencer = Sequencer(
    cyclesPerSample = 4.toRational(),
    frameSequencerFourStepPeriodCycles = 48,
    frameSequencerFiveStepPeriodCycles = 60
  )

  @Test
  fun `4-step sequence`() {
    sequencer.mode = FOUR_STEP

    assertEquals(
      expectedFourStepSeq.repeat(2),
      sequencer.take(24)
    )
  }

  @Test
  fun `5-step sequence`() {
    sequencer.mode = FIVE_STEP

    // We set the mode, so get an initial tick fire
    assertEquals(
      expectedFiveStepSeq.repeat(2).replaceFirst(Ticks(1, 1)),
      sequencer.take(30)
    )
  }

  @Test
  fun `reset to 4-step sequence`() {
    sequencer.mode = FOUR_STEP

    sequencer.take(3)   // Make some initial progress
    sequencer.mode = FOUR_STEP  // Reset

    // Sequence back to beginning
    assertEquals(
      expectedFourStepSeq.repeat(2),
      sequencer.take(24)
    )
  }

  @Test
  fun `reset to 5-step sequence`() {
    sequencer.mode = FIVE_STEP

    sequencer.take(3)   // Make some initial progress
    sequencer.mode = FIVE_STEP  // Reset

    // Sequence back to beginning, but note both ticks fire
    assertEquals(
      expectedFiveStepSeq.repeat(2).replaceFirst(Ticks(1, 1)),
      sequencer.take(30)
    )
  }

  private val expectedFourStepSeq = listOf(
    Ticks(1, 0),
    Ticks(1, 1),
    Ticks(1, 0),
    Ticks(1, 1)
  ).spread()

  private val expectedFiveStepSeq = listOf(
    Ticks(1, 0),
    Ticks(1, 1),
    Ticks(1, 0),
    Ticks(0, 0),
    Ticks(1, 1)
  ).spread()

  private fun List<Ticks>.spread() = flatMap { listOf(Ticks(0, 0), Ticks(0, 0), it) }

  private fun List<Ticks>.replaceFirst(ticks: Ticks) = listOf(ticks) + drop(1)

  private fun Sequencer.take(num: Int) = List(num) { take() }
}
