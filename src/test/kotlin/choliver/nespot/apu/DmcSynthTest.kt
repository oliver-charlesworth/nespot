package choliver.nespot.apu

import choliver.nespot.Memory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DmcSynthTest {

  // TODO - level set

  @Test
  fun name() {
    val memory = mock<Memory>()
    val synth = DmcSynth(cyclesPerSample = 4.toRational(), memory = memory)
    synth.periodCycles = 4.toRational()
    synth.address = 0x1230
    synth.length = 5

    for (i in 0 until 5) {
      whenever(memory.load(0x1230 + i)) doReturn (if (i % 2 == 0) 0xFF else 0xAA)  // Nice bit patterns
    }

    println(synth.take(40))

    val expected = listOf(
      0,
      2,  4,  6,  8,  10, 12, 14, 16,
      14, 16, 14, 16, 14, 16, 14, 16,
      18, 20, 22, 24, 26, 28, 30, 32,
      30, 32, 30, 32, 30, 32, 30, 32,
      34, 36, 38, 40, 42, 44, 46
    )

    assertEquals(expected, synth.take(40))
  }
}
