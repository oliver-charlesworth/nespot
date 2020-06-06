package choliver.nespot.backup

import choliver.nespot.Ram
import choliver.nespot.cartridge.Rom
import choliver.nespot.data
import choliver.nespot.hash
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class BackupManagerTest {
  @Test
  fun `saves data if PRG-RAM present`(@TempDir dir: Path) {
    val expectedData = ByteArray(8) { (0x11 * it).toByte() }
    val bm = BackupManager(
      ROM,
      Ram(8).initialise(expectedData),
      dir.toFile()
    )

    bm.maybeSave()

    val files = dir.toFile().listFiles()!!
    assertTrue(files.size == 1)
    assertEquals(BACKUP_FILENAME, files[0].name)
    assertArrayEquals(expectedData, files[0].readBytes())
  }

  @Test
  fun `doesn't save anything if PRG-RAM not present`(@TempDir dir: Path) {
    val bm = BackupManager(
      ROM,
      null,
      dir.toFile()
    )

    bm.maybeSave()

    assertTrue(dir.toFile().listFiles()!!.isEmpty())
  }

  @Test
  fun `restores data if PRG-RAM and file present`(@TempDir dir: Path) {
    val expectedData = ByteArray(8) { (0x11 * it).toByte() }
    val ram = Ram(8)
    val bm = BackupManager(
      ROM,
      ram,
      dir.toFile()
    )
    File(dir.toFile(), BACKUP_FILENAME).writeBytes(expectedData)

    bm.maybeRestore()

    repeat(8) { assertEquals(expectedData[it].data(), ram[it]) }
  }

  @Test
  fun `doesn't restore if file not present`(@TempDir dir: Path) {
    val ram = Ram(8).initialise(ByteArray(8) { 0 })
    val bm = BackupManager(
      ROM,
      ram,
      dir.toFile()
    )

    bm.maybeRestore()

    repeat(8) { assertEquals(0, ram[it]) }    // Original content
  }

  @Test
  fun `throws if size mismatch`(@TempDir dir: Path) {
    val expectedData = ByteArray(8) { (0x11 * it).toByte() }
    val ram = Ram(9)    // Uh-h
    val bm = BackupManager(
      ROM,
      ram,
      dir.toFile()
    )
    File(dir.toFile(), BACKUP_FILENAME).writeBytes(expectedData)

    assertThrows<RuntimeException> {
      bm.maybeRestore()
    }
  }

  private fun Ram.initialise(data: ByteArray) = apply {
    data.forEachIndexed { i, d -> this[i] = d.data() }
  }

  companion object {
    private val ROM = Rom()
    private val FAKE_HASH = ROM.hash
    private val BACKUP_FILENAME = "${FAKE_HASH}.backup.dat"
  }
}
