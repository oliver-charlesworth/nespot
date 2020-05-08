package choliver.nespot.persistence

import choliver.nespot.data
import choliver.nespot.nes.Nes
import java.io.File

class BackupManager(private val nes: Nes) {
  private val file = File("backup/${XXX}.backup.dat")

  fun maybeRestore() {
    if ((nes.prgRam != null) && file.exists()) {
      val bytes = file.readBytes()
      if (bytes.size != nes.prgRam.size) {
        throw RuntimeException("Backup size mismatch")
      }
      bytes.forEachIndexed { i, byte -> nes.prgRam[i] = byte.data() }
    }
  }

  fun maybeSave() {
    if (nes.prgRam != null) {
      file.writeBytes(ByteArray(nes.prgRam.size) { nes.prgRam[it].toByte() })
    }
  }
}
