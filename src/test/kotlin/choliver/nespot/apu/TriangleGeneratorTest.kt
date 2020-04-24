package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TriangleGeneratorTest {

  // TODO - period
  // TODO - length counter
  // TODO - linear counter
  // TODO - length reloads linear
  // TODO - reaching zero stops the sequence, rather than muting it

  private val triangle = TriangleGenerator(cyclesPerSample = 4.toRational()).apply {
    timer = 3  // Remember that 1 gets added to this internally
    length = 1
    linear = 1
  }

  @Test
  fun `produces triangle wave`() {
    val seq = triangle.take(32)
    val seq2 = triangle.take(32)

    val expected = (15 downTo 0) + (0..15)
    assertEquals(expected, seq)
    assertEquals(expected, seq2)  // Repeats
  }
}
