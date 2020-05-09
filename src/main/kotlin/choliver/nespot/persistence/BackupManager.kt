package choliver.nespot.persistence

import choliver.nespot.Ram
import choliver.nespot.cartridge.Rom
import choliver.nespot.data
import java.io.File

class BackupManager(
  rom: Rom,
  private val persistentRam: Ram?,
  backupDir: File
) {
  private val file = File(backupDir, "${rom.hash}.backup.dat")

  fun maybeRestore() {
    if ((persistentRam != null) && file.exists()) {
      val bytes = file.readBytes()
      if (bytes.size != persistentRam.size) {
        throw RuntimeException("Backup size mismatch")
      }
      repeat(persistentRam.size) { persistentRam[it] = bytes[it].data() }
    }
  }

  fun maybeSave() {
    if (persistentRam != null) {
      file.writeBytes(ByteArray(persistentRam.size) { persistentRam[it].toByte() })
    }
  }
}
