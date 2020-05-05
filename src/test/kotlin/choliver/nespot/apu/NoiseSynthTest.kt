package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NoiseSynthTest {
  private val synth = NoiseSynth().apply {
    enabled = true
    length = 1
    haltLength = false
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
  fun `freezes once length counter exhausted`() {
    synth.mode = 1    // Use short sequence because easier to test
    synth.length = 32
    synth.nextNonZeroOutput()
    repeat(32) { synth.onHalfFrame() }    // Exhaust counter

    assertEquals(
      List(16) { 0 },   // Expect to be frozen on zero
      synth.take(16)
    )
  }

  @Test
  fun `length counter not exhausted if halted`() {
    synth.haltLength = true
    synth.mode = 1    // Use short sequence because easier to test
    synth.length = 32
    synth.nextNonZeroOutput()
    repeat(32) { synth.onHalfFrame() }    // Would ordinarily exhaust counter

    assertEquals(
      listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),   // We don't expect to be frozen
      synth.take(16)
    )
  }

  @Test
  fun `exhaustion is visible`() {
    synth.length = 8
    repeat(7) { synth.onHalfFrame() }

    assertTrue(synth.hasRemainingOutput)

    synth.onHalfFrame()

    assertFalse(synth.hasRemainingOutput)
  }
}
