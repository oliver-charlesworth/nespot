package choliver.nespot

import choliver.nespot.cartridge.Rom
import java.security.MessageDigest


val Rom.hash get() = (prgData + chrData).sha1()

fun Rom.printInfo() {
  println("Magic #:       ${String(magic)}")
  println("PRG-ROM size:  ${prgData.size}")
  println("CHR-ROM size:  ${chrData.size}")
  println("Trainer size:  ${trainerData.size}")
  println("PRG-RAM:       ${if (prgRam) "yes" else "no"}")
  println("Mapper #:      ${mapper}")
  println("Mirroring:     ${mirroring.name.toLowerCase()}")
  println("NES 2.0:       ${if (nes2) "yes" else "no"}")
  println("TV system:     ${tvSystem.name}")
  println("PRG + CHR:     ${(prgData + chrData).sha1()}")
  println("PRG:           ${prgData.sha1()}")
  println("CHR:           ${if (chrData.isNotEmpty()) chrData.sha1() else "-"}")
}

private fun ByteArray.sha1(): String {
  val md = MessageDigest.getInstance("SHA-1")
  md.update(this)
  return md.digest().hex()
}

private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }
