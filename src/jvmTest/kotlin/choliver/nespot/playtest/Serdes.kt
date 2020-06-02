package choliver.nespot.playtest

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.playtest.Scenario.Stimulus.*
import choliver.nespot.sha1
import com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET
import com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class Serdes {
  private val mapper = jacksonObjectMapper()
    .enable(INDENT_OUTPUT)
    .disable(AUTO_CLOSE_TARGET)
    .disable(AUTO_CLOSE_SOURCE)

  fun serialiseTo(zip: File, scenario: Scenario) {
    val converted = convertScenario(scenario)
    toZip(zip, converted)
  }

  fun deserialiseFrom(zip: File): Scenario {
    val converted = fromZip(zip)
    return unconvertScenario(converted)
  }

  private fun toZip(zip: File, converted: ScenarioAndSnapshots) {
    ZipOutputStream(zip.outputStream().buffered()).use { zos ->
      converted.snapshots.forEach { (hash, image) ->
        zos.putNextEntry(ZipEntry("${hash}.png"))
        ImageIO.write(image, "PNG", zos)
        zos.closeEntry()
      }

      zos.putNextEntry(ZipEntry(SCENARIO_FILENAME))
      mapper.writeValue(zos, converted.scenario)
      zos.closeEntry()
    }
  }

  private fun fromZip(zip: File): ScenarioAndSnapshots {
    val snapshots = mutableMapOf<String, BufferedImage>()
    var scenario: SerialisedScenario? = null

    ZipInputStream(zip.inputStream().buffered()).use { zis ->
      loop@ while (true) {
        when (val name = zis.nextEntry?.name) {
          null -> break@loop
          SCENARIO_FILENAME -> scenario = mapper.readValue(zis)
          else -> snapshots[name.removeSuffix(".png")] = ImageIO.read(zis)
        }
      }
    }

    if (scenario != null) {
      return ScenarioAndSnapshots(scenario!!, snapshots)
    } else {
      throw RuntimeException("Malformed scenario file")
    }
  }

  private fun convertScenario(scenario: Scenario): ScenarioAndSnapshots {
    val snapshots = mutableMapOf<String, BufferedImage>()
    val converted = SerialisedScenario(
      scenario.romHash,
      scenario.stimuli.map { s ->
        when (s) {
          is ButtonDown -> SerialisedScenario.Stimulus.ButtonDown(s.timestamp, s.button)
          is ButtonUp -> SerialisedScenario.Stimulus.ButtonUp(s.timestamp, s.button)
          is Snapshot -> {
            val imageAndHash = convertImage(s.bytes)
            snapshots[imageAndHash.hash] = imageAndHash.image
            SerialisedScenario.Stimulus.Snapshot(s.timestamp, imageAndHash.hash)
          }
          is Close -> SerialisedScenario.Stimulus.Close(s.timestamp)
        }
      }
    )

    return ScenarioAndSnapshots(
      scenario = converted,
      snapshots = snapshots
    )
  }

  private fun unconvertScenario(converted: ScenarioAndSnapshots): Scenario {
    return Scenario(
      converted.scenario.romHash,
      converted.scenario.stimuli.map { s ->
        when (s) {
          is SerialisedScenario.Stimulus.ButtonDown -> ButtonDown(s.timestamp, s.button)
          is SerialisedScenario.Stimulus.ButtonUp -> ButtonUp(s.timestamp, s.button)
          is SerialisedScenario.Stimulus.Snapshot -> {
            Snapshot(s.timestamp, unconvertImage(converted.snapshots[s.hash]!!))
          }
          is SerialisedScenario.Stimulus.Close -> Close(s.timestamp)
        }
      }
    )
  }

  private fun convertImage(bytes: List<Byte>): ImageAndHash {
    return ImageAndHash(
      image = createImage(bytes),
      hash = bytes.toByteArray().sha1()
    )
  }

  private fun unconvertImage(image: BufferedImage): List<Byte> {
    val raw = MutableList<Byte>(SCREEN_WIDTH * SCREEN_HEIGHT * 4) { 0 }
    for (i in 0 until SCREEN_WIDTH * SCREEN_HEIGHT) {
      val pixel = image.getRGB(i % SCREEN_WIDTH, i / SCREEN_WIDTH)
      raw[i * 4 + 0] = (pixel shr 24).toByte()
      raw[i * 4 + 1] = (pixel shr 16).toByte()
      raw[i * 4 + 2] = (pixel shr 8).toByte()
      raw[i * 4 + 3] = (pixel shr 0).toByte()
    }
    return raw
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

  private class ScenarioAndSnapshots(
    val scenario: SerialisedScenario,
    val snapshots: Map<String, BufferedImage>
  )

  private class ImageAndHash(
    val image: BufferedImage,
    val hash: String
  )

  companion object {
    private const val SCENARIO_FILENAME = "scenario.json"
  }
}
