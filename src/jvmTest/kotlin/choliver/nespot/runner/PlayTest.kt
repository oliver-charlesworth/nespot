package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipInputStream

//@Disabled("Intended to be run manually")
class PlayTest {
  @Test
  fun `super mario bros`() = execute("smb")

  private fun execute(name: String) {
    if (RECORD) {
      record(name)
    } else {
//      compare(name)
    }
  }

  private fun record(name: String) {
    val scenario = runner(name).run()
    Serdes().serialiseTo(captureFile(name), scenario)
  }

//  private fun compare(name: String) {
//    val original = readFromZipFile<List<TimestampedEvent>>(name)
//    original.snapshots().forEachIndexed { idx, s -> savePng("${idx}", s.data) }
//    exitProcess(0)
//    val recording = runner(name, original).run()
//
//    assertEquals(original.snapshots(), recording.snapshots())
//  }

  private inline fun <reified T : Any> readFromZipFile(name: String) =
    ZipInputStream(captureFile(name).inputStream().buffered()).use { zis ->
      zis.nextEntry
      mapper.readValue<T>(zis)
    }

  private fun runner(name: String, ghost: Scenario? = null) =
    CapturingRunner(Rom.parse(romFile(name).readBytes()), ghost)

  private fun captureFile(name: String) = File(CAPTURES_BASE, "${name}.zip")

  private fun romFile(name: String) = File(TEST_ROMS_BASE, "${name}.nes")

  private val mapper = jacksonObjectMapper()

  companion object {
    private val CAPTURES_BASE = File("captures")
    private val TEST_ROMS_BASE = File("roms")

    const val RECORD = true
  }
}
