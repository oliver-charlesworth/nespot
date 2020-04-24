package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TriangleGeneratorTest {
  private val triangle = TriangleGenerator(cyclesPerSample = 4.toRational()).apply {
    timer = 3  // Remember that 1 gets added to this internally
    length = 1
    linear = 1
  }

  @Test
  fun `produces triangle wave`() {
    val seq = triangle.take(64)

    assertEquals(((15 downTo 0) + (0..15)).repeat(2), seq)
  }

  @Test
  fun `linear counter`() {
    triangle.linear = 40
    val seq = triangle.take(64, Ticks(quarter = 1, half = 0))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 8) + listOf(8).repeat(24), // Sticks on last sample
      seq
    )
  }

  @Test
  fun `setting length counter reloads linear counter`() {
    triangle.linear = 40
    val seq1 = triangle.take(4, Ticks(quarter = 1, half = 0))
    triangle.length = 5 // Whatever
    val seq2 = triangle.take(60, Ticks(quarter = 1, half = 0))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 4) + listOf(4).repeat(20), // Sticks on last sample
      seq1 + seq2
    )
  }

  @Test
  fun `length counter`() {
    triangle.length = 4  // Maps to actual length of 40
    val seq = triangle.take(64, Ticks(quarter = 0, half = 1))

    assertEquals(
      (15 downTo 0) + (0..15) + (15 downTo 8) + listOf(8).repeat(24), // Sticks on last sample
      seq
    )
  }
}
