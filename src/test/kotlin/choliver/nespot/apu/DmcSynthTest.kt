package choliver.nespot.apu

import choliver.nespot.Memory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DmcSynthTest {
  private val memory = mock<Memory>()
  private val synth = DmcSynth(memory = memory).apply {
    address = 0x1230
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
    for (i in 0 until 5) {
      whenever(memory[0x1230 + i]) doReturn (if (i % 2 == 0) 0xFF else 0xAA)  // Nice bit patterns
    }

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      18, 20, 22, 24, 26, 28, 30, 32,   // Up up up
      30, 32, 30, 32, 30, 32, 30, 32,   // Down and up
      34, 36, 38, 40, 42, 44, 46, 48    // Up up up
    )

    assertEquals(expected, synth.take(41))
  }

  @Test
  fun `retains last value after sample ends`() {
    synth.length = 2
    whenever(memory[0x1230]) doReturn 0xFF
    whenever(memory[0x1231]) doReturn 0xAA

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,
      14, 16, 14, 16, 14, 16, 14, 16,
      16, 16, 16, 16
    )

    assertEquals(expected, synth.take(21))
  }

  @Test
  fun `restarts after sample ends if loop flag set`() {
    synth.length = 2
    synth.loop = true
    whenever(memory[0x1230]) doReturn 0xFF
    whenever(memory[0x1231]) doReturn 0xAA

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,   // Up up up
      14, 16, 14, 16, 14, 16, 14, 16,   // Down and up
      18, 20, 22, 24, 26, 28, 30, 32,   // Up up up
      30, 32, 30, 32, 30, 32, 30, 32    // Down and up
    )

    assertEquals(expected, synth.take(33))

  }

  @Test
  fun `can't exceed lower bound`() {
    synth.length = 1
    synth.level = 7
    whenever(memory[0x1230]) doReturn 0x00 // Dowm down down

    val expected = listOf(
      7,
      5, 3, 2, 2, 2, 2, 2, 2
    )

    assertEquals(expected, synth.take(9))
  }

  @Test
  fun `can't exceed upper bound`() {
    synth.length = 1
    synth.level = 120
    whenever(memory[0x1230]) doReturn 0xFF // Up up up

    val expected = listOf(
      120,
      122, 124, 125, 125, 125, 125, 125, 125
    )

    assertEquals(expected, synth.take(9))
  }

  @Test
  fun `changing address mid-way resets pattern, but current sample is played to completion`() {
    synth.length = 2
    whenever(memory[0x1230]) doReturn 0xFF
    whenever(memory[0x1231]) doReturn 0xAA
    whenever(memory[0x2340]) doReturn 0x00
    whenever(memory[0x2341]) doReturn 0x55

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,   // This sample plays to completion even though we reset mid-way
      14, 12, 10, 8,  6,  4,  2,  2,    // Now we play the new pattern
      4,  2,  4,  2,  4,  2,  4,  2
    )

    val seq1 = synth.take(5)
    synth.address = 0x2340
    val seq2 = synth.take(20)

    assertEquals(expected, seq1 + seq2)
  }

  @Test
  fun `changing length mid-way resets pattern, but current sample is played to completion`() {
    synth.length = 2
    whenever(memory[0x1230]) doReturn 0xFF
    whenever(memory[0x1231]) doReturn 0xAA

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,   // This sample plays to completion even though we reset mid-way
      18, 20, 22, 24, 26, 28, 30, 32    // Now we start playing the shortened pattern
    )

    val seq1 = synth.take(5)
    synth.length = 1
    val seq2 = synth.take(12)

    assertEquals(expected, seq1 + seq2)
  }

  @Test
  fun `exhaustion of bytes remaining is visible`() {
    synth.length = 5
    synth.take(32)

    assertTrue(synth.hasRemainingOutput)

    synth.take(1)   // The sequence is length-40, but the flag is cleared once we've performed all the mem loads

    assertFalse(synth.hasRemainingOutput)
  }
}
