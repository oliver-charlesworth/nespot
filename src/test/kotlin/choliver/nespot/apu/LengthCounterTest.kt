package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LengthCounterTest {
  private val lc = LengthCounter()

  @Test
  fun `length not persisted when enabled`() {
    lc.enabled = false
    lc.length = 3

    assertEquals(0, lc.length)
    assertEquals(0, lc.remaining)
  }

  @Test
  fun `length persisted when enabled`() {
    lc.enabled = true
    lc.length = 3

    assertEquals(3, lc.length)
    assertEquals(3, lc.remaining)
  }

  @Test
  fun `decrementable when enabled`() {
    lc.enabled = true
    lc.length = 3
    lc.decrement()
    lc.decrement()

    assertEquals(3, lc.length)
    assertEquals(1, lc.remaining)
  }

  @Test
  fun `can't decrement below zero`() {
    lc.enabled = true
    lc.length = 3
    lc.decrement()
    lc.decrement()
    lc.decrement()
    lc.decrement()

    assertEquals(0, lc.remaining)
  }

  @Test
  fun `disabling clears settings`() {
    lc.enabled = true
    lc.length = 3
    lc.decrement()
    lc.enabled = false

    assertEquals(0, lc.length)
    assertEquals(0, lc.remaining)
  }

  @Test
  fun `setting length restarts counter`() {
    lc.enabled = true
    lc.length = 3
    lc.decrement()
    lc.decrement()
    lc.length = 5

    assertEquals(5, lc.length)
    assertEquals(5, lc.remaining)
  }
}
