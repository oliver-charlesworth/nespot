package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SquareSynthTest {
  private val synth = SquareSynth().apply {
    enabled = true
    dutyCycle = 0
    length = 8
    haltLength = false
  }

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
  fun `freezes once length counter exhausted`() {
    synth.dutyCycle = 3 // Easiest to see the impact on
    synth.nextNonZeroOutput()
    repeat(8) { synth.onHalfFrame() }   // Exhaust counter

    assertEquals(
      List(8) { 0 },                     // Expect to be frozen on zero
      synth.take(8)
    )
  }

  @Test
  fun `length counter not exhausted if halted`() {
    synth.haltLength = true
    synth.dutyCycle = 3 // Easiest to see the impact on
    synth.nextNonZeroOutput()
    repeat(8) { synth.onHalfFrame() }   // Would ordinarily exhaust counter

    assertEquals(
      listOf(1, 0, 0, 1, 1, 1, 1, 1),          // We don't expect to be frozen
      synth.take(8)
    )
  }

  @Test
  fun `exhaustion is visible`() {
    repeat(7) { synth.onHalfFrame() }

    assertTrue(synth.hasRemainingOutput)

    synth.onHalfFrame()

    assertFalse(synth.hasRemainingOutput)
  }

  @Test
  fun `exhausts if disabled`() {
    synth.enabled = false

    assertEquals(0, synth.length)
    assertFalse(synth.hasRemainingOutput)
  }
}
