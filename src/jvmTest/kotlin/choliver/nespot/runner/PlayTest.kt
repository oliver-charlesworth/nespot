package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.runner.TimestampedEvent.Event.Snapshot
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    val recording = runner(name).use { runner -> runner.run() }
    writeToZipFile(name, recording)
  }

  private fun compare(name: String) {
    val original = readFromZipFile<List<TimestampedEvent>>(name)
    val recording = runner(name).use { runner -> runner.run(original) }

    fun List<TimestampedEvent>.snapshots() = map { it.event }.filterIsInstance<Snapshot>()

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

  private fun runner(name: String) = CapturingRunner(Rom.parse(romFile(name).readBytes()))

  private fun captureFile(name: String) = File(CAPTURES_BASE, "${name}.zip")

  private fun romFile(name: String) = File(TEST_ROMS_BASE, "${name}.nes")

  private val mapper = jacksonObjectMapper()

  companion object {
    private val CAPTURES_BASE = File("captures")
    private val TEST_ROMS_BASE = File("roms")

    const val RECORD = false
  }
}
