package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.*
import choliver.nespot.mappers.Mmc3Mapper.ChrMode.CHR_MODE_0
import choliver.nespot.mappers.Mmc3Mapper.ChrMode.CHR_MODE_1
import choliver.nespot.mappers.Mmc3Mapper.MirroringMode.HORIZONTAL
import choliver.nespot.mappers.Mmc3Mapper.MirroringMode.VERTICAL
import choliver.nespot.mappers.Mmc3Mapper.PrgMode.PRG_MODE_0
import choliver.nespot.mappers.Mmc3Mapper.PrgMode.PRG_MODE_1


// See https://wiki.nesdev.com/w/index.php/MMC3
class Mmc3Mapper(rom: Rom) : Mapper {
  private val vram = ByteArray(VRAM_SIZE)
  private val prgRam = ByteArray(PRG_RAM_SIZE)
  private val prgData = rom.prgData
  private val chrData = rom.chrData
  private val numPrgBanks = (prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = (chrData.size / CHR_BANK_SIZE)
  private var mirrorMode = VERTICAL
  private var chrMode = CHR_MODE_0
  private var prgMode = PRG_MODE_0
  private var regSelect = 0
  private val regs = IntArray(8) { 0 }
  private var irqReload = false
  private var irqReloadValue: Data = 0x00
  private var irqEnabled = false
  private var irqCounter = 0x00
  private var prevA12 = false
  private var _irq = false

  override val irq get() = _irq
  override val persistentRam = Ram.backedBy(prgRam)

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> prgData[prgRomAddr(addr)]
      else -> prgRam[addr % PRG_RAM_SIZE]
    }.data()

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_REG) -> setReg(addr, data)
        (addr >= BASE_PRG_RAM) -> prgRam[addr % PRG_RAM_SIZE] = data.toByte()
      }
    }
  }

  override val chr = object : Memory {
    override fun get(addr: Address): Data {
      updateIrqState(addr)
      return when {
        (addr >= BASE_VRAM) -> vram[vramAddr(addr)]  // This maps everything >= 0x4000 too
        else -> chrData[chrRomAddr(addr)]
      }.data()
    }

    override fun set(addr: Address, data: Data) {
      updateIrqState(addr)
      when {
        (addr >= BASE_VRAM) -> vram[vramAddr(addr)] = data.toByte()  // This maps everything >= 0x4000 too
      }
    }
  }

  private fun prgRomAddr(addr: Address): Int {
    val iBank = when ((addr and 0x6000) shr 13) {
      0 -> when (prgMode) {
        PRG_MODE_0 -> regs[6]
        PRG_MODE_1 -> (numPrgBanks - 2)
      }
      1 -> regs[7]
      2 -> when (prgMode) {
        PRG_MODE_0 -> (numPrgBanks - 2)
        PRG_MODE_1 -> regs[6]
      }
      else -> (numPrgBanks - 1)
    }
    return (addr % PRG_BANK_SIZE) + iBank * PRG_BANK_SIZE
  }

  private fun chrRomAddr(addr: Address): Int {
    val a = when (chrMode) {
      CHR_MODE_0 -> addr
      CHR_MODE_1 -> (addr xor 0x1000)  // Conditional inversion of A12
    }
    val iBank = when (a shr 10) {
      0 -> regs[0] and 0xFE
      1 -> regs[0] or 0x01
      2 -> regs[1] and 0xFE
      3 -> regs[1] or 0x01
      4 -> regs[2]
      5 -> regs[3]
      6 -> regs[4]
      else -> regs[5]
    }
    return (a % CHR_BANK_SIZE) + iBank * CHR_BANK_SIZE
  }

  private fun vramAddr(addr: Address): Address = when (mirrorMode) {
    VERTICAL -> mirrorVertical(addr)
    HORIZONTAL -> mirrorHorizontal(addr)
  }

  private fun setReg(addr: Address, data: Data) {
    val even = (addr % 2) == 0
    when ((addr and 0x6000) shr 13) {
      0 -> when (even) {
        false -> {
          when (regSelect) {
            0, 1, 2, 3, 4, 5 -> regs[regSelect] = data % numChrBanks
            6, 7 -> regs[regSelect] = data % numPrgBanks
          }
        }
        true -> {
          chrMode = ChrMode.values()[(data and 0x80) shr 7]
          prgMode = PrgMode.values()[(data and 0x40) shr 6]
          regSelect = data and 0b00000111
        }
      }
      1 -> when (even) {
        false -> Unit // TODO - PRG-RAM protect
        true -> mirrorMode = MirroringMode.values()[data and 0x01]
      }
      2 -> when (even) {
        false -> irqReload = true
        true -> irqReloadValue = data
      }
      3 -> when (even) {
        false -> irqEnabled = true
        true -> {
          irqEnabled = false
          _irq = false
        }
      }
    }
  }

  private fun updateIrqState(addr: Address) {
    if (detectRisingEdge(addr)) {
      val prevCounter = irqCounter

      if ((irqCounter == 0) || irqReload) {
        irqReload = false
        irqCounter = irqReloadValue
      } else {
        irqCounter--
      }

      if (irqEnabled && (prevCounter == 1) && (irqCounter == 0)) {
        _irq = true
      }
    }
  }

  private fun detectRisingEdge(addr: Address): Boolean {
    val newA12 = addr.isBitSet(12)
    val clockEdge = !prevA12 && newA12
    prevA12 = newA12
    return clockEdge
  }

  private enum class PrgMode {
    PRG_MODE_0,
    PRG_MODE_1
  }

  private enum class ChrMode {
    CHR_MODE_0,
    CHR_MODE_1
  }

  private enum class MirroringMode {
    VERTICAL,
    HORIZONTAL
  }

  @Suppress("unused")
  companion object {
    const val PRG_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 8192
    const val CHR_BANK_SIZE = 1024

    const val BASE_PRG_RAM = BASE_PRG_ROM - PRG_RAM_SIZE
    const val BASE_REG = BASE_PRG_ROM
  }
}
