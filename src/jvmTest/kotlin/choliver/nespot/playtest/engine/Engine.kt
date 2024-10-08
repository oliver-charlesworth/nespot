package choliver.nespot.playtest.engine

import choliver.nespot.cartridge.Rom
import choliver.nespot.playtest.engine.Engine.Mode.*
import choliver.nespot.playtest.engine.Scenario.Stimulus.Snapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.math.abs

class Engine(
  private val romsBase: File,
  private val capturesBase: File,
  private val snapshotPattern: SnapshotPattern,
  private val mode: Mode = modeFromEnv()
) {
  enum class Mode {
    CAPTURE,
    RECAPTURE,
    COMPARE,
    REPLAY
  }

  fun execute(name: String) {
    val romFile = romsBase.resolve("${name}.nes")
    val captureFile = capturesBase.resolve("${name}.zip")
    assumeTrue(romFile.exists())
    val rom = Rom.parse(romFile.readBytes())

    when (mode) {
      CAPTURE -> {
        val capture = liveCapture(rom, snapshotPattern)
        capture.serialiseTo(captureFile)
      }
      RECAPTURE -> {
        val original = deserialiseFrom(captureFile)
        val capture = headlessGhostCapture(rom, original)
        capture.serialiseTo(captureFile)
      }
      COMPARE -> {
        val original = deserialiseFrom(captureFile)
        val capture = headlessGhostCapture(rom, original)
        assertEquals(original, capture)
      }
      REPLAY -> {
        val original = deserialiseFrom(captureFile)
        uiGhostCapture(rom, original)
      }
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
      assertTrue(delta <= TOLERANCE, "Delta at byte #${idx} of image #${idxImage} above threshold (${delta})")
    }
  }

  companion object {
    private const val TOLERANCE = 1   // PNG has lsb-level rounding errors

    private fun modeFromEnv() =
      valueOf(System.getenv("ENGINE_MODE")?.uppercase() ?: COMPARE.toString())
  }
}

