package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvelopeTest {
  private val env = Envelope().apply {
    directMode = false
    param = 2 // Actual period is this + 1
  }

  @Test
  fun `direct mode`() {
    env.directMode = true
    env.param = 11

    assertEquals(listOf(11), env.take(1))
  }

  @Test
  fun decay() {
    assertEquals(
      (15 downTo 0).toList().repeatEach(3),
      env.take(48)
    )
  }

  @Test
  fun `fixed zero after end of decay`() {
    assertEquals(
      listOf(0, 0, 0, 0, 0, 0, 0, 0),
      env.take(56).drop(48)
    )
  }

  @Test
  fun `looped decay`() {
    env.loop = true

    assertEquals(
      (15 downTo 0).toList().repeatEach(3).repeat(2),
      env.take(96)
    )
  }

  @Test
  fun `restarts mid-decay`() {
    env.take(16) // Arbitrary number of advances
    env.restart()

    assertEquals(
      (15 downTo 0).toList().repeatEach(3),
      env.take(48)
    )
  }

  private fun Envelope.take(num: Int) = List(num) { advance(); level }
}
