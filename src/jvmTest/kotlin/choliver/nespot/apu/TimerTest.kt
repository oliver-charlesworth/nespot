package choliver.nespot.apu

import choliver.nespot.apu.Timer.Companion.MIN_PERIOD
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TimerTest {
  private val timer = Timer().apply {
    periodCycles = 4
  }

  @Test
  fun `generates periodic ticks`() {
    assertEquals(
      listOf(0, 0, 0, 1).repeat(3).adjustForStartup(),
      timer.take(12)
    )
  }

  @Test
  fun `generates multi-ticks`() {
    timer.advance(1)  // Ignore startup zero
    assertEquals(3, timer.advance(12))
  }

  @Test
  fun `doesn't immediately restart on new period`() {
    timer.take(1) // Make some progress
    timer.periodCycles = 3

    assertEquals(
      listOf(0, 0, 1, 0, 0, 1),   // If the period immediately reset, then we would expect more leading zeros
      timer.take(6)
    )
  }

  @Test
  fun `disabled if below minimum period`() {
    timer.periodCycles = MIN_PERIOD - 1

    assertEquals(
      listOf(0).repeat(20).adjustForStartup(),
      timer.take(20)
    )
  }

  @Test
  fun `doesn't divide by zero`() {
    timer.periodCycles = 0

    assertDoesNotThrow {
      timer.take(20)
    }
  }

  private fun Timer.take(num: Int) = List(num) { advance(1) }

  // Always an initial tick because pos starts at zero
  private fun List<Int>.adjustForStartup() = listOf(first() + 1) + drop(1)
}
