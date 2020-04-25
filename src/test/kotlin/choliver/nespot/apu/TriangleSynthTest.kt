package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TriangleSynthTest {
  private val synth = TriangleSynth().apply {
    length = 4
    linear = 4
    haltLength = false
    preventReloadClear = false
    onQuarterFrame()  // Reload linear counter
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
    repeat(4) { synth.onQuarterFrame() }   // Exhaust counter

    assertEquals(
      List(16) { 15 },    // Expect to be frozen on most recent output sample
      synth.take(16)
    )
  }

  @Test
  fun `freezes once length counter exhausted`() {
    repeat(4) { synth.onHalfFrame() }     // Exhaust counter

    assertEquals(
      List(16) { 15 },    // Expect to be frozen on most recent output sample
      synth.take(16)
    )
  }

  @Test
  fun `length counter not exhausted if halted`() {
    synth.haltLength = true
    repeat(4) { synth.onHalfFrame() }     // Would ordinarily exhaust counter

    assertEquals(
      (15 downTo 0).toList(),    // We don't expect to be frozen
      synth.take(16)
    )
  }

  @Test
  fun `setting length counter reloads linear counter`() {
    repeat(2) { synth.onQuarterFrame() }   // Not all the way
    synth.length = 5                             // Arbitrary value, but will trigger a reload
    synth.onQuarterFrame()                       // Cause reload
    repeat(2) { synth.onQuarterFrame() }   // Should be starting from the top

    assertEquals((15 downTo 11).toList(), synth.take(5))  // So not frozen yet

    repeat(2) { synth.onQuarterFrame() }   // Should be frozen now

    assertEquals(
      List(16) { 10 },    // Expect to be frozen on most recent output sample
      synth.take(16)
    )
  }

  @Test
  fun `new linear value not reloaded on next tick if clear permitted`() {
    reloadLinearThenSetNewValueThenExhaust()

    assertEquals(
      List(16) { 15 },    // Expect to be stuck on most recent output sample
      synth.take(16)
    )
  }

  @Test
  fun `new linear value is reloaded on next tick if clear prevented`() {
    synth.preventReloadClear = true
    reloadLinearThenSetNewValueThenExhaust()

    assertEquals(
      (15 downTo 0).toList(),    // We don't expect to be frozen, as the updated linear counter *has* been absorbed
      synth.take(16)
    )
  }

  private fun reloadLinearThenSetNewValueThenExhaust() {
    repeat(2) { synth.onQuarterFrame() }   // Not all the way
    synth.length = 5                             // Arbitrary value, but will trigger a reload
    synth.onQuarterFrame()                       // Cause reload
    synth.linear = 10                            // Should be ignored
    repeat(4) { synth.onQuarterFrame() }   // Should exhaust linear counter
  }
}
