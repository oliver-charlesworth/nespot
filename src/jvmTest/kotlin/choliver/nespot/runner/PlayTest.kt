package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PlayTest {

  // TODO - expectation that .nes file exists
  @Test
  fun `super mario bros`() = execute("smb")

  private fun execute(name: String) {
    if (RECORD) {
      val recording = runner(name).run()
      serdes.serialiseTo(captureFile(name), recording)
    } else {
      val original = serdes.deserialiseFrom(captureFile(name))
      val recording = runner(name, original).run()
      assertEquals(original, recording)
    }
  }

  private fun runner(name: String, ghost: Scenario? = null) =
    CapturingRunner(Rom.parse(romFile(name).readBytes()), ghost)

  private fun captureFile(name: String) = File(CAPTURES_BASE, "${name}.zip")
  private fun romFile(name: String) = File(TEST_ROMS_BASE, "${name}.nes")

  private val serdes = Serdes()

  companion object {
    private val CAPTURES_BASE = File("captures")
    private val TEST_ROMS_BASE = File("roms")

    const val RECORD = false
  }
}
