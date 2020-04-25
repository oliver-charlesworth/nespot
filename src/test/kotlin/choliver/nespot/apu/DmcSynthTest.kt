package choliver.nespot.apu

import choliver.nespot.Memory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test

class DmcSynthTest {

  // TODO - level set

  @Test
  fun name() {
    val memory = mock<Memory>()
    val synth = DmcSynth(cyclesPerSample = 4.toRational(), memory = memory)
    synth.rate = 14   // Maps to period of 72
    synth.address = 0x23 // Maps to address of 0xC8C0
    synth.length = 2  // Corresponds to length of 33

    for (i in 0 until 33) {
      whenever(memory.load(0xC8C0 + i)) doReturn (i * 3 + 1)  // Arbitrary data
    }

    println(synth.take(200))
  }
}
