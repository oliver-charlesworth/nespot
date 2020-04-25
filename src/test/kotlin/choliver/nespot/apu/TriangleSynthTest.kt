package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TriangleSynthTest {
  private val synth = TriangleSynth(cyclesPerSample = 4.toRational()).apply {
    periodCycles = 4.toRational()
    length = 1
    linear = 1
  }

  @Test
  fun `produces triangle wave`() {
    val seq = synth.take(64)

    assertEquals(((15 downTo 0) + (0..15)).repeat(2), seq)
  }

  @Test
  fun `linear counter`() {
    synth.linear = 40
    val seq = synth.take(64, Ticks(quarter = _1, half = _0))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 8) + listOf(8).repeat(24), // Sticks on last sample
      seq
    )
  }

  @Test
  fun `setting length counter reloads linear counter`() {
    synth.linear = 40
    val seq1 = synth.take(4, Ticks(quarter = _1, half = _0))
    synth.length = 5 // Whatever
    val seq2 = synth.take(60, Ticks(quarter = _1, half = _0))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 4) + listOf(4).repeat(20), // Sticks on last sample
      seq1 + seq2
    )
  }

  @Test
  fun `length counter`() {
    synth.length = 40
    val seq = synth.take(64, Ticks(quarter = _0, half = _1))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 8) + listOf(8).repeat(24), // Sticks on last sample
      seq
    )
  }
}
