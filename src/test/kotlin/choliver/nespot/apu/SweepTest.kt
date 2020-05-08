package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SweepTest {
  private val timer = Counter().apply {
    periodCycles = 64
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
  fun `restarts mid-decay`() {
    take(2) // Part way through division
    sweep.restart()

    // Sequence is the same as if none of the above had happened
    assertEquals(
      listOf(64, 64, 64, 80, 80, 80, 80, 100),
      take(8)
    )
  }

  @Test
  fun `mutes if current period too low`() {
    timer.periodCycles = 16
    assertFalse(sweep.mute)

    timer.periodCycles = 15
    assertTrue(sweep.mute)
  }

  @Test
  fun `mutes if target period too high`() {
    sweep.negate = false
    timer.periodCycles = 0x800

    sweep.shift = 1
    assertFalse(sweep.mute)

    sweep.shift = 0
    assertTrue(sweep.mute)
  }

  @Test
  fun `doesn't mute if inhibited`() {
    sweep.inhibitMute = true

    timer.periodCycles = 16
    assertFalse(sweep.mute)

    timer.periodCycles = 15
    assertFalse(sweep.mute)
  }

  @Test
  fun `doesn't adjust period when muted`() {
    timer.periodCycles = 15
    sweep.negate = true

    assertEquals(
      listOf(15, 15, 15, 15),
      take(4)
    )
  }

  private fun take(num: Int) = List(num) { sweep.advance(); timer.periodCycles }
}
