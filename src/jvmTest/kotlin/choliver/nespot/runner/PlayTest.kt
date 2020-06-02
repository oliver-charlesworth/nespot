package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.runner.Scenario.Stimulus.Snapshot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

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

  private fun assertEquals(expected: Scenario, actual: Scenario) {
    fun Scenario.snapshots() = stimuli.filterIsInstance<Snapshot>()

    (expected.snapshots() zip actual.snapshots()).forEachIndexed { idx, (expected, actual) ->
      assertNearlyEquals(expected.bytes, actual.bytes, idx)
    }
  }

  private fun assertNearlyEquals(expected: List<Byte>, actual: List<Byte>, idxImage: Int) {
    (expected zip actual).forEachIndexed { idx, (expected, actual) ->
      val delta = abs(expected - actual)
      assertTrue(delta <= TOLERANCE, "Deltae at byte #${idx} of image #${idxImage} out of range (${delta})")
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

    private const val TOLERANCE = 1   // PNG isn't completely lossless at lsb

    const val RECORD = false
  }
}
