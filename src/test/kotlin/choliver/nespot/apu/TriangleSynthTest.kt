package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TriangleSynthTest {
  private val synth = TriangleSynth().apply {
    length = 1
    linear = 1
  }

  @Test
  fun `produces triangle wave`() {
    assertEquals(
      ((15 downTo 0) + (0..15)).repeat(2),
      synth.take(64)
    )
  }

  @Test
  fun `freezes once linear counter exhausted`() {
    synth.linear = 40
    repeat(40) { synth.onQuarterFrame() }

    assertEquals(
      List(16) { 15 },    // Expect to still be on first sample of sequence
      synth.take(16)
    )
  }

  @Test
  fun `freezes once length counter exhausted`() {
    synth.length = 40
    repeat(40) { synth.onHalfFrame() }

    assertEquals(
      List(16) { 15 },    // Expect to still be on first sample of sequence
      synth.take(16)
    )
  }

  @Test
  fun `setting length counter reloads linear counter`() {
    synth.linear = 40
    repeat(20) { synth.onQuarterFrame() }   // Not all the way
    synth.length = 5 // Whatever
    repeat(20) { synth.onQuarterFrame() }   // Should be starting from the top

    assertEquals((15 downTo 11).toList(), synth.take(5))  // So not frozen yet

    repeat(20) { synth.onQuarterFrame() }   // Should be frozen now

    assertEquals(
      List(16) { 10 },    // Expect to still be on most recent output sample
      synth.take(16)
    )
  }
}
