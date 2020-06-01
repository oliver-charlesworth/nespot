package choliver.nespot.runner

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.cartridge.Rom
import choliver.nespot.runner.TimestampedEvent.Event.Snapshot
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.system.exitProcess

//@Disabled("Intended to be run manually")
class PlayTest {
  @Test
  fun `super mario bros`() = execute("smb")

  private fun execute(name: String) {
    if (RECORD) {
      record(name)
    } else {
      compare(name)
    }
  }

  private fun record(name: String) {
    val recording = runner(name).run()
    writeToZipFile(name, recording)
  }

  private fun compare(name: String) {
    val original = readFromZipFile<List<TimestampedEvent>>(name)
    original.snapshots().forEachIndexed { idx, s -> savePng("${idx}", s.data) }
    exitProcess(0)
    val recording = runner(name, original).run()

    assertEquals(original.snapshots(), recording.snapshots())
  }

  private fun writeToZipFile(name: String, data: Any) {
    ZipOutputStream(captureFile(name).outputStream().buffered()).use { zos ->
      zos.putNextEntry(ZipEntry("data.json"))
      mapper.writeValue(zos, data)
    }
  }

  private inline fun <reified T : Any> readFromZipFile(name: String) =
    ZipInputStream(captureFile(name).inputStream().buffered()).use { zis ->
      zis.nextEntry
      mapper.readValue<T>(zis)
    }

  private fun savePng(name: String, data: List<Int>) {
    val bi = BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, TYPE_INT_RGB)
    var i = 0
    for (y in 0 until SCREEN_HEIGHT) {
      for (x in 0 until SCREEN_WIDTH) {
        val pixel = data[i++]
        bi.setRGB(x, y, 0 +
          (((pixel shr 8) and 0xFF) shl 16) +
          (((pixel shr 16) and 0xFF) shl 8) +
          (((pixel shr 24) and 0xFF) shl 0)
        )
      }
    }
    ImageIO.write(bi, "PNG", File(CAPTURES_BASE, "${name}.png"))
  }

  private fun runner(name: String, prerecorded: List<TimestampedEvent>? = null) =
    CapturingRunner(Rom.parse(romFile(name).readBytes()), prerecorded)

  private fun captureFile(name: String) = File(CAPTURES_BASE, "${name}.zip")

  private fun romFile(name: String) = File(TEST_ROMS_BASE, "${name}.nes")

  fun List<TimestampedEvent>.snapshots() = map { it.event }.filterIsInstance<Snapshot>()

  private val mapper = jacksonObjectMapper()

  companion object {
    private val CAPTURES_BASE = File("captures")
    private val TEST_ROMS_BASE = File("roms")

    const val RECORD = false
  }
}
