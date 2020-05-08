package choliver.nespot.apu

import choliver.nespot.toRational
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimerTest {
  @Test
  fun `integer multiple`() {
    val timer = Timer(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 16
    }

    assertEquals(
      listOf(0, 0, 0, 1).repeat(3).adjustForStartup(),
      timer.take(12)
    )
  }

  @Test
  fun `rational multiple`() {
    val timer = Timer(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 10
    }

    assertEquals(
      listOf(0, 0, 1, 0, 1).repeat(6).adjustForStartup(),
      timer.take(30)
    )
  }

  @Test
  fun `unity multiple`() {
    val timer = Timer(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 4
    }

    assertEquals(
      listOf(1).repeat(20).adjustForStartup(),
      timer.take(20)
    )
  }

  @Test
  fun `sub-unity integer multiple`() {
    val timer = Timer(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 2
    }

    assertEquals(
      listOf(2).repeat(20).adjustForStartup(),
      timer.take(20)
    )
  }

  @Test
  fun `sub-unity rational multiple`() {
    val timer = Timer(cyclesPerSample = 10.toRational()).apply {
      periodCycles = 4
    }

    assertEquals(
      listOf(2, 3).repeat(10).adjustForStartup(),
      timer.take(20)
    )
  }

  @Test
  fun `doesn't immediately restart on new period`() {
    val timer = Timer(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 16
    }

    timer.take(1) // Make some progress
    timer.periodCycles = 4

    assertEquals(
      listOf(0, 0, 1, 1, 1, 1), // If the period immediately reset, then these would all be 1's
      timer.take(6)
    )
  }

  private fun Timer.take(num: Int) = List(num) { take() }

  // Always an initial tick because pos starts at zero
  private fun List<Int>.adjustForStartup() = listOf(first() + 1) + drop(1)
}
