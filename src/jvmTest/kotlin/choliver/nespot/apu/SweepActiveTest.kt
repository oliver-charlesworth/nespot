package choliver.nespot.apu

import choliver.nespot.Rational
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SweepActiveTest {
  private val timer = Timer(cyclesPerSample = Rational.of(1)).apply {
    periodCycles = 64
  }
  private val sweep = createSweep(false)

  @Test
  fun `increases period`() {
    sweep.negate = false

    assertEquals(
      listOf(64, 64, 64, 80, 80, 80, 80, 100),
      sweep.take(8)
    )
  }

  @Test
  fun `decreases period`() {
    sweep.negate = true

    assertEquals(
      listOf(64, 64, 64, 48, 48, 48, 48, 36),
      sweep.take(8)
    )
  }

  @Test
  fun `decreases period with one's complement`() {
    val sweep = createSweep(true)
    sweep.negate = true

    assertEquals(
      listOf(64, 64, 64, 47, 47, 47, 47, 35),
      sweep.take(8)
    )
  }

  @Test
  fun `restarts mid-decay`() {
    sweep.take(2) // Part way through division
    sweep.restart()

    // Sequence is the same as if none of the above had happened
    assertEquals(
      listOf(64, 64, 64, 80, 80, 80, 80, 100),
      sweep.take(8)
    )
  }

  @Test
  fun `doesn't adjust period for shift == 0`() {
    sweep.negate = true
    sweep.shift = 0

    assertEquals(
      listOf(64, 64, 64, 64, 64, 64, 64, 64),
      sweep.take(8)
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
  fun `doesn't adjust period when muted`() {
    timer.periodCycles = 15
    sweep.negate = true

    assertEquals(
      listOf(15, 15, 15, 15),
      sweep.take(4)
    )
  }

  private fun Sweep.take(num: Int) = List(num) { advance(); timer.periodCycles }

  private fun createSweep(negateWithOnesComplement: Boolean) =
    SweepActive(timer, negateWithOnesComplement = negateWithOnesComplement).apply {
      enabled = true
      shift = 2
      divider = 3
    }
}
