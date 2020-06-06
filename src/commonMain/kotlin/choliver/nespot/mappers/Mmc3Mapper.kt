package choliver.nespot.mappers

import choliver.nespot.BASE_PRG_ROM
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.common.isBitSet

// See https://wiki.nesdev.com/w/index.php/MMC3
class Mmc3Mapper(rom: Rom) : Mapper {
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = (rom.chrData.size / CHR_BANK_SIZE)
  private var chrMode = 0
  private var prgMode = 0
  private var regSelect = 0
  private val regs = IntArray(8) { 0 }
  private var irqReload = false
  private var irqReloadValue: Data = 0x00
  private var irqEnabled = false
  private var irqCounter = 0x00
  private var prevA12 = false
  private var _irq = false

  override val prgData = rom.prgData
  override val chrData = rom.chrData
  override val prgBankSize = PRG_BANK_SIZE
  override val chrBankSize = CHR_BANK_SIZE

  override fun Cartridge.onStartup() {
    prg.bankMap[3] = numPrgBanks - 1    // The only thing guaranteed on startup
  }

  override fun Cartridge.onPrgSet(addr: Address, data: Data) {
    val even = (addr % 2) == 0
    when ((addr and 0x6000) shr 13) {
      0 -> {
        when (even) {
          false -> {
            when (regSelect) {
              0, 1, 2, 3, 4, 5 -> regs[regSelect] = data % numChrBanks
              6, 7 -> regs[regSelect] = data % numPrgBanks
            }
          }
          true -> {
            chrMode = (data and 0x80) shr 7
            prgMode = (data and 0x40) shr 6
            regSelect = data and 0b00000111
          }
        }
        updatePrgBankMap()
        updateChrBankMap()
      }
      1 -> when (even) {
        false -> Unit // TODO - PRG-RAM protect
        true -> chr.mirroring = when (data and 0x01) {
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
  }

  override fun Cartridge.onChrGet(addr: Address) {
    updateIrqState(addr)
  }

  override fun Cartridge.onChrSet(addr: Address, data: Data) {
    updateIrqState(addr)
  }

  override val irq get() = _irq
  override val persistRam = true

  private fun Cartridge.updatePrgBankMap() {
    val map = prg.bankMap
    when (prgMode) {
      0 -> {
        map[0] = regs[6]
        map[2] = numPrgBanks - 2
      }
      else -> {
        map[0] = numPrgBanks - 2
        map[2] = regs[6]
      }
    }
    map[1] = regs[7]
    map[3] = numPrgBanks - 1
  }

  private fun Cartridge.updateChrBankMap() {
    val map = chr.bankMap
    when (chrMode) {
      0 -> {
        map[0] = regs[0] and 0xFE
        map[1] = regs[0] or 0x01
        map[2] = regs[1] and 0xFE
        map[3] = regs[1] or 0x01
        map[4] = regs[2]
        map[5] = regs[3]
        map[6] = regs[4]
        map[7] = regs[5]
      }
      else -> {
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

  @Suppress("unused")
  companion object {
    const val PRG_BANK_SIZE = 8192
    const val CHR_BANK_SIZE = 1024
    const val BASE_REG = BASE_PRG_ROM
  }
}
