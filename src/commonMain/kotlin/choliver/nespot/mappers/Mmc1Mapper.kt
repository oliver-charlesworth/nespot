package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.*
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.cartridge.StandardMapper.Config

// https://wiki.nesdev.com/w/index.php/MMC1
class Mmc1Mapper(rom: Rom, private val getStepCount: () -> Int) : Config {
  override val prgData = rom.prgData
  override val chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData
  override val prgBankSize = PRG_BANK_SIZE
  override val chrBankSize = CHR_BANK_SIZE
  override val persistRam = true

  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = (chrData.size / CHR_BANK_SIZE)
  private var srCount = 0
  private var sr = 0
  private var chr0Bank = 0
  private var chr1Bank = 0
  private var prgBank = (numPrgBanks - 1)   // Bubble Bobble relies on this to start up
  private var chrMode = 0
  private var prgMode = 0
  private var prevStep = -1

  override fun StandardMapper.onStartup() {
    updatePrgBankMap()
    updateChrBankMap()
    chr.mirroring = FIXED_LOWER
  }

  override fun StandardMapper.onPrgSet(addr: Address, data: Data) {
    // We don't update on consecutive stores (we approximate this as multiple stores in the same instruction step)
    val currentStep = getStepCount()
    if (currentStep != prevStep) {
      if (data.isBitSet(7)) {
        // Reset
        srCount = 5
        sr = 0x00
      } else {
        sr = (sr shr 1) or ((data and 1) shl 4)
        if (--srCount == 0) {
          when ((addr and 0x6000) shr 13) {
            0 -> {
              chr.mirroring = Rom.Mirroring.values()[(sr and 0x03)]
              prgMode = (sr and 0x0C) shr 2
              chrMode = (sr and 0x10) shr 4
            }
            1 -> chr0Bank = sr % numChrBanks
            2 -> chr1Bank = sr % numChrBanks
            3 -> prgBank = sr % numPrgBanks
          }
          updatePrgBankMap()
          updateChrBankMap()

          // Reset
          srCount = 5
          sr = 0x00
        }
      }
    }
    prevStep = currentStep
  }

  private fun StandardMapper.updatePrgBankMap() {
    val map = prg.bankMap
    when (prgMode) {
      0, 1 -> {
        map[0] = prgBank and 0x0E
        map[1] = prgBank or 0x01
      }
      2 -> {
        map[0] = 0
        map[1] = prgBank
      }
      else -> {
        map[0] = prgBank
        map[1] = numPrgBanks - 1
      }
    }
  }

  private fun StandardMapper.updateChrBankMap() {
    val map = chr.bankMap
    when (chrMode) {
      0 -> {
        map[0] = chr0Bank and 0x1E
        map[1] = chr0Bank or 0x01
      }
      else -> {
        map[0] = chr0Bank
        map[1] = chr1Bank
      }
    }
  }

  @Suppress("unused")
  companion object {
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 4096
    const val BASE_SR = BASE_PRG_ROM
  }
}
