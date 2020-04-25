package choliver.nespot.apu

import choliver.nespot.Memory
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test

class DmcSynthTest {

  @Test
  fun name() {
    val memory = mock<Memory>()
    val synth = DmcSynth(cyclesPerSample = 4.toRational(), memory = memory)

    synth.rate = 15   // Maps to period of 54
//    synth.
  }
}
