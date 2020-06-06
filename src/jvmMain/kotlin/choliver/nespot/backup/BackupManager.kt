package choliver.nespot.backup

import choliver.nespot.cartridge.Rom
import choliver.nespot.common.data
import choliver.nespot.hash
import choliver.nespot.memory.Ram
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
      file.parentFile.mkdirs()
      file.writeBytes(ByteArray(persistentRam.size) { persistentRam[it].toByte() })
    }
  }
}
