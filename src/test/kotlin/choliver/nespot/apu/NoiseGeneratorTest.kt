package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoiseGeneratorTest {
  private val gen = NoiseGenerator(cyclesPerSample = 4.toRational()).apply {
    volume = 1
    length = 1
    period = 0  // Corresponds to actual period of 4
  }

  @Test
  fun `full-length sequence`() {
    // Sequence is too long to test the whole thing against golden vector
    val seq = gen.take(  32767)
    val seq2 = gen.take(32767)

    assertEquals(
      listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
      seq.take(32)
    )
    assertEquals(16384, seq.count { it == 1 })  // Property of LFSR
    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun `short sequence`() {
    gen.mode = 1
    val seq = gen.take(  93)
    val seq2 = gen.take(93)

    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun `different period`() {
    gen.mode = 1    // Use short sequence because easier to test
    val ref = gen.take(  93)

    gen.period = 2  // Corresponds to actual period of 16
    val seq = gen.take(93 * 4)

    assertEquals(
      ref.flatMap { listOf(it).repeat(4) },
      seq
    ) // Long-period sequence should match the padded short-period sequence
  }

  @Test
  fun volume() {
    gen.volume = 5
    val seq = gen.take(  32767)

    assertEquals(setOf(0, 5), seq.distinct().toSet())
  }

  @Test
  fun length() {
    gen.mode = 1    // Use short sequence because easier to test
    gen.length = 30 // Maps to actual length of 32
    val seq = gen.take(93, Ticks(quarter = 0, half = 1))

    assertEquals(setOf(0, 1), seq.take(32).distinct().toSet())
    assertEquals(setOf(0), seq.drop(32).distinct().toSet())  // Everything else is zeroed
  }
}
