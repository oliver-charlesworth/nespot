package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PulseGeneratorTest {
  private val gen = PulseGenerator(cyclesPerSample = 4.toRational()).apply {
    volume = 1
    timer = 3   // Remember that 1 gets added to this internally
    dutyCycle = 0
    length = 1
  }

  // TODO - weirdness: "timer has a value less than eight"

  @Test
  fun `12,5% duty cycle`() {
    gen.dutyCycle = 0
    val seq = gen.take(32)

    assertEquals(listOf(0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% duty cycle`() {
    gen.dutyCycle = 1
    val seq = gen.take(32)

    assertEquals(listOf(0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `50% duty cycle`() {
    gen.dutyCycle = 2
    val seq = gen.take(32)

    assertEquals(listOf(0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0).repeat(2), seq)
  }

  @Test
  fun `25% negated duty cycle`() {
    gen.dutyCycle = 3
    val seq = gen.take(32)

    assertEquals(listOf(1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1).repeat(2), seq)
  }
}
