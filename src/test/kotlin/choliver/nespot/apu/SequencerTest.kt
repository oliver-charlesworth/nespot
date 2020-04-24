package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Mode.FIVE_STEP
import choliver.nespot.apu.Sequencer.Mode.FOUR_STEP
import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SequencerTest {

  // TODO - what happens when the mode changes

  @Test
  fun `4-step sequence`() {
    val sequencer = Sequencer(
      cyclesPerSample = 4.toRational(),
      frameSequencerFourStepPeriodCycles = 32,
      frameSequencerFiveStepPeriodCycles = 40
    )
    sequencer.mode = FOUR_STEP

    val expected = listOf(
      Ticks(0, 0),
      Ticks(1, 0),
      Ticks(0, 0),
      Ticks(1, 1),
      Ticks(0, 0),
      Ticks(1, 0),
      Ticks(0, 0),
      Ticks(1, 1)
    ).repeat(2)

    assertEquals(expected, sequencer.take(16))
  }

  @Test
  fun `5-step sequence`() {
    val sequencer = Sequencer(
      cyclesPerSample = 4.toRational(),
      frameSequencerFourStepPeriodCycles = 32,
      frameSequencerFiveStepPeriodCycles = 40
    )
    sequencer.mode = FIVE_STEP

    val expected = listOf(
      Ticks(0, 0),
      Ticks(1, 0),
      Ticks(0, 0),
      Ticks(1, 1),
      Ticks(0, 0),
      Ticks(1, 0),
      Ticks(0, 0),
      Ticks(0, 0),
      Ticks(0, 0),
      Ticks(1, 1)
    ).repeat(2)

    assertEquals(expected, sequencer.take(20))
  }
}
