package choliver.nespot.cartridge

import choliver.nespot.cartridge.Rom.Mirroring.*
import choliver.nespot.cartridge.Rom.TvSystem.NTSC
import choliver.nespot.cartridge.Rom.TvSystem.PAL
import choliver.nespot.isBitSet
import java.security.MessageDigest

@Suppress("ArrayInDataClass")
data class Rom(
  val magic: ByteArray = byteArrayOf(),
  val hasPersistentMem: Boolean = false,
  val mirroring: Mirroring = IGNORED,
  val trainerData: ByteArray = byteArrayOf(),
  val prgData: ByteArray = byteArrayOf(),
  val chrData: ByteArray = byteArrayOf(),
  val prgRam: Boolean = false,
  val mapper: Int = 0,
  val nes2: Boolean = false,
  val tvSystem: TvSystem = NTSC
) {
  enum class Mirroring {
    HORIZONTAL,
    VERTICAL,
    IGNORED   // TODO - not sure if this is mutually exclusive
  }

  enum class TvSystem {
    NTSC,
    PAL
  }

  fun printInfo() {
    println("Magic #:       ${String(magic)}")
    println("PRG-ROM size:  ${prgData.size}")
    println("CHR-ROM size:  ${chrData.size}")
    println("Trainer size:  ${trainerData.size}")
    println("PRG-RAM:       ${if (prgRam) "yes" else "no"}")
    println("Mapper #:      $mapper")
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

  companion object {
    fun parse(raw: ByteArray): Rom {
      val sizePrgRom = raw[4] * 16384
      val sizeChrRom = raw[5] * 8192
      val sizeTrainer = if (raw[6].isBitSet(2)) 512 else 0

      return Rom(
        magic = raw.copyOfRange(0, 4),
        hasPersistentMem = raw[6].isBitSet(1),
        mirroring = when {
          raw[6].isBitSet(3) -> IGNORED
          raw[6].isBitSet(0) -> VERTICAL
          else -> HORIZONTAL
        },
        trainerData = raw.copyOfRange(16, 16 + sizeTrainer),
        prgData = raw.copyOfRange(16 + sizeTrainer, 16 + sizeTrainer + sizePrgRom),
        chrData = raw.copyOfRange(16 + sizeTrainer + sizePrgRom, 16 + sizeTrainer + sizePrgRom + sizeChrRom),
        prgRam = raw[6].isBitSet(1),
        mapper = ((raw[6].toInt() and 0xF0) shr 4) or (raw[7].toInt() and 0xF0),
        nes2 = ((raw[7].toInt() and 0x0C) shr 2) == 2,
        tvSystem = if (raw[9].isBitSet(0)) PAL else NTSC
      )
    }
  }
}
