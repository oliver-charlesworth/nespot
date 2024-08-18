package choliver.nespot.apu

import choliver.nespot.memory.Memory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DmcSynthTest {
  private val memory = mock<Memory>()
  private val synth = DmcSynth(memory = memory).apply {
    address = 0xC230
  }

  @Test
  fun `sets output level directly`() {
    synth.level = 56
    val expected = listOf(56, 56, 56, 56)

    assertEquals(expected, synth.take(4))
  }

  @Test
  fun `decodes delta sequence`() {
    synth.length = 5
    synth.enabled = true
    for (i in 0 until 5) {
      whenever(memory[0xC230 + i]) doReturn (if (i % 2 == 0) 0xFF else 0xAA)  // Nice bit patterns
    }

    skipWarmupSamples()

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      18, 20, 22, 24, 26, 28, 30, 32,   // Up up up
      30, 32, 30, 32, 30, 32, 30, 32,   // Down and up
      34, 36, 38, 40, 42, 44, 46, 48    // Up up up
    )
    assertEquals(expected, synth.take(40))
  }

  @Test
  fun `memory address wrap around`() {
    synth.length = 5
    synth.address = 0xFFFF
    synth.enabled = true
    whenever(memory[0xFFFF]) doReturn 0xFF
    whenever(memory[0x8000]) doReturn 0xAA

    skipWarmupSamples()

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16    // Down and up
    )
    assertEquals(expected, synth.take(16))
  }

  @Test
  fun `retains last value after sample ends`() {
    synth.length = 2
    synth.enabled = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,
      14, 16, 14, 16, 14, 16, 14, 16,
      16, 16, 16, 16
    )
    assertEquals(expected, synth.take(20))
  }

  @Test
  fun `if loop flag set, then restarts after sample ends, but doesn't assert IRQ`() {
    synth.length = 2
    synth.enabled = true
    synth.irqEnabled = true
    synth.loop = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      18, 20, 22, 24, 26, 28, 30, 32,   // Up up up
      30, 32, 30, 32, 30, 32, 30, 32    // Down and up
    )
    assertEquals(expected, synth.take(32))
    assertFalse(synth.irq)
  }

  @Test
  fun `if loop flag clear, then doesn't restart after sample ends, but does assert IRQ`() {
    synth.length = 2
    synth.enabled = true
    synth.irqEnabled = true
    synth.loop = false
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      16, 16, 16, 16, 16, 16, 16, 16,
      16, 16, 16, 16, 16, 16, 16, 16
    )
    assertEquals(expected, synth.take(32))
    assertTrue(synth.irq)
  }

  @Test
  fun `can't exceed lower bound`() {
    synth.length = 1
    synth.enabled = true
    synth.level = 7
    whenever(memory[0xC230]) doReturn 0x00 // Dowm down down

    skipWarmupSamples()

    val expected = listOf(
      5, 3, 2, 2, 2, 2, 2, 2
    )
    assertEquals(expected, synth.take(8))
  }

  @Test
  fun `can't exceed upper bound`() {
    synth.length = 1
    synth.enabled = true
    synth.level = 120
    whenever(memory[0xC230]) doReturn 0xFF // Up up up

    skipWarmupSamples()

    val expected = listOf(
      122, 124, 125, 125, 125, 125, 125, 125
    )
    assertEquals(expected, synth.take(8))
  }

  @Test
  fun `exhaustion of bytes remaining is visible`() {
    synth.length = 5
    synth.enabled = true
    synth.take(32)

    assertTrue(synth.outputRemaining)

    synth.take(1)   // The sequence is length-40, but the flag is cleared once we've performed all the mem loads

    assertFalse(synth.outputRemaining)
  }

  @Test
  fun `exhausts if disabled`() {
    synth.enabled = false

    assertEquals(0, synth.length)
    assertFalse(synth.outputRemaining)
  }

  @Test
  fun `irq cleared on enable`() {
    synth.length = 2
    synth.irqEnabled = true
    synth.enabled = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    synth.take(33)

    assertTrue(synth.irq)
  }

  @Test
  fun `sample not restarted on enable if mid-sample`() {
    synth.length = 2
    synth.enabled = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()
    val seq1 = synth.take(4)
    synth.enabled = true
    val seq2 = synth.take(12)

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16    // Down and up
    )
    assertEquals(expected, seq1 + seq2)
  }

  @Test
  fun `sample restarted on enable if exhausted`() {
    synth.length = 2
    synth.enabled = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()
    val seq1 = synth.take(8)  // Just after final load
    synth.enabled = true
    val seq2 = synth.take(24)

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      18, 20, 22, 24, 26, 28, 30, 32,   // Up up up
      30, 32, 30, 32, 30, 32, 30, 32    // Down and up
    )
    assertEquals(expected, seq1 + seq2)
  }

  @Test
  fun `sample can only start on octet boundaries`() {
    synth.length = 2
    synth.enabled = true
    whenever(memory[0xC230]) doReturn 0xFF
    whenever(memory[0xC231]) doReturn 0xAA

    skipWarmupSamples()
    val seq1 = synth.take(16 + 4)  // Part way into silence octet
    synth.enabled = true
    val seq2 = synth.take(4 + 16)

    val expected = listOf(
      2,  4,  6,  8,  10, 12, 14, 16,   // Sample
      14, 16, 14, 16, 14, 16, 14, 16,   // "
      16, 16, 16, 16, 16, 16, 16, 16,   // Silence
      18, 20, 22, 24, 26, 28, 30, 32,   // Sample starts on octet boundary
      30, 32, 30, 32, 30, 32, 30, 32    // "
    )
    assertEquals(expected, seq1 + seq2)
  }

  // TODO - play what's left on disable

  private fun skipWarmupSamples() = synth.take(9) // 1 for output-before-advance, 8 for no-sample-loaded
}
