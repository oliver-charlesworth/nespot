package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoiseGeneratorTest {
  private val gen = NoiseGenerator(cyclesPerSample = 4.toRational())

  // TODO - volume
  // TODO - mode
  // TODO - length
  // TODO - period

  @Test
  fun `full-length sequence`() {
    gen.volume = 1
    gen.period = 0  // Corresponds to actual period of 4
    gen.length = 1

    // Sequence is too long to test the whole thing against golden vector
    val seq = takeable.take(  32767)
    val seq2 = takeable.take(32767)

    assertEquals(
      listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0),
      seq.take(32)
    )
    assertEquals(16384, seq.count { it == 1 })  // Property of LFSR
    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun volume() {
    gen.volume = 5
    gen.period = 0
    gen.length = 1

    assertEquals(
      listOf(0, 5),
      takeable.take(  32767).distinct()
    )
  }


  private val takeable = object : Takeable<Int> {
    override fun take() = gen.take(Ticks(0, 0))
  }
}
