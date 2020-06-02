package choliver.nespot.playtest

import choliver.nespot.cartridge.Rom
import choliver.nespot.playtest.Scenario.Stimulus.Snapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

class PlayTest {
  @Test
  fun `super mario bros`() = execute("smb")

  @Test
  fun `super mario bros 3`() = execute("smb3")

  @Test
  fun `bubble bobble`() = execute("bb")

  @Test
  fun `donkey kong`() = execute("dk")

  @Test
  fun `kirby's adventure`() = execute("kirby")

  @Test
  fun `micro machines`() = execute("mm")

  @Test
  fun `mig-29 soviet fighter`() = execute("mig29")

  @Test
  fun castelian() = execute("castelian")

  @Test
  fun `jurassic park`() = execute("jp")

  private fun execute(name: String) {
    val romFile = romFile(name)
    assumeTrue(romFile.exists())
    val rom = Rom.parse(romFile.readBytes())

    if (RECORD) {
      val recording = capture(rom)
      serdes.serialiseTo(captureFile(name), recording)
    } else {
      val original = serdes.deserialiseFrom(captureFile(name))
      val recording = ghostCapture(rom, original)
      assertEquals(original, recording)
    }
  }

  private fun assertEquals(expected: Scenario, actual: Scenario) {
    assertEquals(expected.romHash, actual.romHash, "Mismatched hashes")

    fun Scenario.snapshots() = stimuli.filterIsInstance<Snapshot>()

    (expected.snapshots() zip actual.snapshots()).forEachIndexed { idx, (expected, actual) ->
      assertNearlyEquals(expected.bytes, actual.bytes, idx)
    }
  }

  private fun assertNearlyEquals(expected: List<Byte>, actual: List<Byte>, idxImage: Int) {
    (expected zip actual).forEachIndexed { idx, (expected, actual) ->
      val delta = abs(expected - actual)
      assertTrue(delta <= TOLERANCE, "Delta at byte #${idx} of image #${idxImage} out of range (${delta})")
    }
  }

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
