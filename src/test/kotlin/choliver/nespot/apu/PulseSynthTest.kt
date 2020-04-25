package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PulseSynthTest {
  private val synth = PulseSynth().apply {
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
    synth.nextNonZeroOutput()
    repeat(8) { synth.onHalfFrame() }

    assertEquals(
      List(16) { 0 },
      synth.take(16)
    )
  }
}
