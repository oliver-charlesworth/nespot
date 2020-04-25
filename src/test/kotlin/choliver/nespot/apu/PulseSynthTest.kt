package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PulseSynthTest {
  private val synth = PulseSynth(cyclesPerSample = 4.toRational()).apply {
    volume = 1
    periodCycles = 8.toRational()
    dutyCycle = 0
    length = 1
  }

  // TODO - weirdness: "timer has a value less than eight"

  @Test
  fun `12,5% duty cycle`() {
    synth.dutyCycle = 0
    val seq = synth.take(32)

    assertEquals(listOf(0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% duty cycle`() {
    synth.dutyCycle = 1
    val seq = synth.take(32)

    assertEquals(listOf(0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `50% duty cycle`() {
    synth.dutyCycle = 2
    val seq = synth.take(32)

    assertEquals(listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% negated duty cycle`() {
    synth.dutyCycle = 3
    val seq = synth.take(32)

    assertEquals(listOf(1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1).repeat(2), seq)
  }

  @Test
  fun volume() {
    synth.dutyCycle = 0
    synth.volume = 5
    val seq = synth.take(16)

    assertEquals(listOf(0, 0, 5, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), seq)
  }

  @Test
  fun `length counter`() {
    synth.dutyCycle = 3 // Easiest to see the impact on
    synth.length = 8
    val seq = synth.take(16, Ticks(quarter = 0, half = 1))

    assertEquals(listOf(1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0), seq)
  }
}
