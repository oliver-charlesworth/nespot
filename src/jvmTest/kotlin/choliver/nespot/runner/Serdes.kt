package choliver.nespot.runner

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.runner.Scenario.Stimulus.*
import choliver.nespot.sha1
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class Serdes {
  private val mapper = jacksonObjectMapper()
    .enable(INDENT_OUTPUT)

  fun serialiseTo(zip: File, scenario: Scenario) {
    val converted = convertScenario(scenario)

    // TODO - delete if fails partway through
    ZipOutputStream(zip.outputStream().buffered()).use { zos ->
      converted.snapshots.forEach { (hash, image) ->
        zos.putNextEntry(ZipEntry("${hash}.png"))
        ImageIO.write(image, "PNG", zos)
      }

      zos.putNextEntry(ZipEntry("scenario.json"))
      mapper.writeValue(zos, converted.scenario)
    }
  }

  private fun convertScenario(scenario: Scenario): AllThings {
    val snapshots = mutableMapOf<String, BufferedImage>()
    val converted = SerialisedScenario(
      scenario.romHash,
      scenario.stimuli.map { s ->
        when (s) {
          is ButtonDown -> SerialisedScenario.Stimulus.ButtonDown(s.timestamp, s.button)
          is ButtonUp -> SerialisedScenario.Stimulus.ButtonUp(s.timestamp, s.button)
          is Snapshot -> {
            val snapshot = convertSnapshot(s)
            snapshots[snapshot.hash] = snapshot.image
            SerialisedScenario.Stimulus.Snapshot(s.timestamp, snapshot.hash)
          }
          is Close -> SerialisedScenario.Stimulus.Close(s.timestamp)
        }
      }
    )

    return AllThings(
      scenario = converted,
      snapshots = snapshots
    )
  }

  private fun convertSnapshot(snapshot: Snapshot): Converted {
    val raw = toRaw(snapshot)
    return Converted(
      image = createImage(raw),
      hash = raw.toByteArray().sha1()
    )
  }

  private fun createImage(raw: List<Byte>) =
    BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, TYPE_INT_RGB).apply {
      for (i in 0 until SCREEN_WIDTH * SCREEN_HEIGHT) {
        setRGB(i % SCREEN_WIDTH, i / SCREEN_WIDTH, 0 +
          (raw[i * 4 + 0].toInt() shl 24) +
          (raw[i * 4 + 1].toInt() shl 16) +
          (raw[i * 4 + 2].toInt() shl 8) +
          (raw[i * 4 + 3].toInt() shl 0)
        )
      }
    }

  private fun toRaw(snapshot: Snapshot) = snapshot.pixels
    .flatMap { listOf(it shr 0, it shr 8, it shr 16, it shr 24) }
    .map { it.toByte() }

  private class AllThings(
    val scenario: SerialisedScenario,
    val snapshots: Map<String, BufferedImage>
  )

  private class Converted(
    val image: BufferedImage,
    val hash: String
  )
}
