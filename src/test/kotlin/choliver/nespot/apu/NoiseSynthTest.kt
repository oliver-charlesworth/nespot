package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoiseSynthTest {
  private val synth = NoiseSynth().apply {
    length = 1
  }

  @Test
  fun `full-length sequence`() {
    // Sequence is too long to test the whole thing against golden vector
    val seq = synth.take(  32767)
    val seq2 = synth.take(32767)

    assertEquals(
      listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
      seq.take(32)
    )
    assertEquals(16384, seq.count { it == 1 })  // Property of LFSR
    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun `short sequence`() {
    synth.mode = 1
    val seq = synth.take(  93)
    val seq2 = synth.take(93)

    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun length() {
    synth.mode = 1    // Use short sequence because easier to test
    synth.length = 32
    synth.nextNonZeroOutput()
    repeat(32) { synth.onHalfFrame() }

    assertEquals(
      List(16) { 0 },
      synth.take(16)
    )
  }
}
