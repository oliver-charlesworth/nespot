package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.BoringChr
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.mappers.Mmc3Mapper.ChrMode.CHR_MODE_0
import choliver.nespot.mappers.Mmc3Mapper.ChrMode.CHR_MODE_1
import choliver.nespot.mappers.Mmc3Mapper.PrgMode.PRG_MODE_0
import choliver.nespot.mappers.Mmc3Mapper.PrgMode.PRG_MODE_1

// See https://wiki.nesdev.com/w/index.php/MMC3
class Mmc3Mapper(rom: Rom) : Mapper {
  private val prgRam = ByteArray(PRG_RAM_SIZE)
  private val prgData = rom.prgData
  private val chrData = rom.chrData
  private val numPrgBanks = (prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = (chrData.size / CHR_BANK_SIZE)
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
      return actualChr[addr]
    }

    override fun set(addr: Address, data: Data) {
      updateIrqState(addr)
      actualChr[addr] = data
    }
  }

  private val actualChr = BoringChr(
    raw = rom.chrData,
    bankSize = CHR_BANK_SIZE,
    mirroring = VERTICAL
  )

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
        true -> actualChr.mirroring = when (data and 0x01) {
          0 -> VERTICAL
          else -> HORIZONTAL
        }
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
    updateChrBankMap()
  }

  private fun updateChrBankMap() {
    val map = actualChr.bankMap
    when (chrMode) {
      CHR_MODE_0 -> {
        map[0] = regs[0] and 0xFE
        map[1] = regs[0] or 0x01
        map[2] = regs[1] and 0xFE
        map[3] = regs[1] or 0x01
        map[4] = regs[2]
        map[5] = regs[3]
        map[6] = regs[4]
        map[7] = regs[5]
      }
      CHR_MODE_1 -> {
        map[0] = regs[2]
        map[1] = regs[3]
        map[2] = regs[4]
        map[3] = regs[5]
        map[4] = regs[0] and 0xFE
        map[5] = regs[0] or 0x01
        map[6] = regs[1] and 0xFE
        map[7] = regs[1] or 0x01
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

  @Suppress("unused")
  companion object {
    const val PRG_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 8192
    const val CHR_BANK_SIZE = 1024

    const val BASE_PRG_RAM = BASE_PRG_ROM - PRG_RAM_SIZE
    const val BASE_REG = BASE_PRG_ROM
  }
}
