package choliver.nespot.apu

import choliver.nespot.toRational
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CounterTest {
  @Test
  fun `integer multiple`() {
    val counter = Counter(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 16.toRational()
    }

    assertEquals(
      listOf(0, 0, 0, 1).repeat(3).adjustForStartup(),
      counter.take(12)
    )
  }

  @Test
  fun `rational multiple`() {
    val counter = Counter(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 10.toRational()
    }

    assertEquals(
      listOf(0, 0, 1, 0, 1).repeat(6).adjustForStartup(),
      counter.take(30)
    )
  }

  @Test
  fun `unity multiple`() {
    val counter = Counter(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 4.toRational()
    }

    assertEquals(
      listOf(1).repeat(20).adjustForStartup(),
      counter.take(20)
    )
  }

  @Test
  fun `sub-unity integer multiple`() {
    val counter = Counter(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 2.toRational()
    }

    assertEquals(
      listOf(2).repeat(20).adjustForStartup(),
      counter.take(20)
    )
  }

  @Test
  fun `sub-unity rational multiple`() {
    val counter = Counter(cyclesPerSample = 10.toRational()).apply {
      periodCycles = 4.toRational()
    }

    assertEquals(
      listOf(2, 3).repeat(10).adjustForStartup(),
      counter.take(20)
    )
  }

  @Test
  fun `doesn't immediately restart on new period`() {
    val counter = Counter(cyclesPerSample = 4.toRational()).apply {
      periodCycles = 16.toRational()
    }

    counter.take(1) // Make some progress
    counter.periodCycles = 4.toRational()

    assertEquals(
      listOf(0, 0, 1, 1, 1, 1), // If the period immediately reset, then these would all be 1's
      counter.take(6)
    )
  }

  private fun Counter.take(num: Int) = List(num) { take() }

  // Always an initial tick because pos starts at zero
  private fun List<Int>.adjustForStartup() = listOf(first() + 1) + drop(1)
}
