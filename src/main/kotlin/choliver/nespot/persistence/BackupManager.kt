package choliver.nespot.persistence

import choliver.nespot.Ram
import choliver.nespot.cartridge.Rom
import choliver.nespot.data
import java.io.File

class BackupManager(
  rom: Rom,
  private val prgRam: Ram?,
  backupDir: File
) {
  private val file = File(backupDir, "${rom.hash}.backup.dat")

  fun maybeRestore() {
    if ((prgRam != null) && file.exists()) {
      val bytes = file.readBytes()
      if (bytes.size != prgRam.size) {
        throw RuntimeException("Backup size mismatch")
      }
      repeat(prgRam.size) { prgRam[it] = bytes[it].data() }
    }
  }

  fun maybeSave() {
    if (prgRam != null) {
      file.writeBytes(ByteArray(prgRam.size) { prgRam[it].toByte() })
    }
  }
}
