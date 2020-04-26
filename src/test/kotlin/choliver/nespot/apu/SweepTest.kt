package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SweepTest {
  private val timer = Counter().apply {
    periodCycles = 64.toRational()
  }
  private val sweep = Sweep(timer).apply {
    enabled = true
    shift = 2
    divider = 3
  }

  @Test
  fun `increases period`() {
    sweep.negate = false

    assertEquals(
      listOf(64, 64, 64, 80, 80, 80, 80, 100),
      take(8)
    )
  }

  @Test
  fun `decreases period`() {
    sweep.negate = true

    assertEquals(
      listOf(64, 64, 64, 48, 48, 48, 48, 36),
      take(8)
    )
  }

  @Test
  fun `doesn't adjust period out of range`() {
    timer.periodCycles = 8.toRational()
    sweep.negate = true

    assertEquals(
      listOf(8, 8, 8, 8),
      take(4)
    )
  }

  @Test
  fun `resets mid-decay`() {
    take(2) // Part way through division
    sweep.reset()

    // Sequence is the same as if none of the above had happened
    assertEquals(
      listOf(64, 64, 64, 80, 80, 80, 80, 100),
      take(8)
    )
  }

  private fun take(num: Int) = List(num) { sweep.advance(); timer.periodCycles.toInt() }
}
