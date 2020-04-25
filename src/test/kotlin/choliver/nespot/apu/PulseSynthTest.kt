package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PulseSynthTest {
  private val synth = PulseSynth(cyclesPerSample = 8.toRational()).apply {
    periodCycles = 8
    dutyCycle = 0
    length = 1
  }

  // TODO - muted
  // TODO - limited sweep

  @Test
  fun `12,5% duty cycle`() {
    synth.dutyCycle = 0
    val seq = synth.take(16)

    assertEquals(listOf(0, 1, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% duty cycle`() {
    synth.dutyCycle = 1
    val seq = synth.take(16)

    assertEquals(listOf(0, 1, 1, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `50% duty cycle`() {
    synth.dutyCycle = 2
    val seq = synth.take(16)

    assertEquals(listOf(0, 1, 1, 1, 1, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% negated duty cycle`() {
    synth.dutyCycle = 3
    val seq = synth.take(16)

    assertEquals(listOf(1, 0, 0, 1, 1, 1, 1, 1).repeat(2), seq)
  }

  @Test
  fun `length counter`() {
    synth.dutyCycle = 3 // Easiest to see the impact on
    synth.length = 8
    val seq = synth.take(8, Ticks(quarter = _0, half = _1))

    assertEquals(listOf(1, 0, 0, 1, 1, 1, 1, 1), seq)
  }
}
