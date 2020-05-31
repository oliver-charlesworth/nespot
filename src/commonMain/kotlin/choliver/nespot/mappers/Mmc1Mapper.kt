package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring.FIXED_LOWER
import choliver.nespot.mappers.Mmc1Mapper.ChrMode.CHR_MODE_0
import choliver.nespot.mappers.Mmc1Mapper.ChrMode.CHR_MODE_1
import choliver.nespot.mappers.Mmc1Mapper.PrgMode.*

// https://wiki.nesdev.com/w/index.php/MMC1
class Mmc1Mapper(rom: Rom, private val getStepCount: () -> Int) : Mapper {
  private val prgRam = ByteArray(PRG_RAM_SIZE)
  private val prgData = rom.prgData
  private val chrData = if (rom.chrData.isEmpty()) ByteArray(CHR_RAM_SIZE) else rom.chrData
  private val numPrgBanks = (prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = (chrData.size / CHR_BANK_SIZE)
  private var srCount = 0
  private var sr = 0
  private var chr0Bank = 0
  private var chr1Bank = 0
  private var prgBank = (numPrgBanks - 1)   // Bubble Bobble relies on this to start up
  private var chrMode = CHR_MODE_0
  private var prgMode = PRG_MODE_0
  private var prevStep = -1

  override val irq = false
  override val persistentRam = Ram.backedBy(prgRam)

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> prgData[prgRomAddr(addr)]
      else -> prgRam[addr % PRG_RAM_SIZE]
    }.data()

    override fun set(addr: Address, data: Data) {
      when {
        (addr >= BASE_SR) -> updateShiftRegister(addr, data)
        (addr >= BASE_PRG_RAM) -> prgRam[addr % PRG_RAM_SIZE] = data.toByte()
      }
    }
  }

  override val chr = ChrMemory(
    raw = chrData,
    bankSize = CHR_BANK_SIZE,
    mirroring = FIXED_LOWER
  )

  private fun prgRomAddr(addr: Address): Int {
    val iBank = if (addr >= BASE_PRG1_ROM) {
      when (prgMode) {
        PRG_MODE_0, PRG_MODE_1 -> (prgBank or 0x01) // 32k mode
        PRG_MODE_2 -> prgBank  // Variable
        PRG_MODE_3 -> (numPrgBanks - 1) // Fixed
      }
    } else {
      when (prgMode) {
        PRG_MODE_0, PRG_MODE_1 -> (prgBank and 0x0E) // 32k mode
        PRG_MODE_2 -> 0 // Fixed
        PRG_MODE_3 -> prgBank // Variable
      }
    }
    return (addr % PRG_BANK_SIZE) + iBank * PRG_BANK_SIZE
  }

  private fun updateShiftRegister(addr: Address, data: Data) {
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
              prgMode = PrgMode.values()[(sr and 0x0C) shr 2]
              chrMode = ChrMode.values()[(sr and 0x10) shr 4]
            }
            1 -> chr0Bank = sr % numChrBanks
            2 -> chr1Bank = sr % numChrBanks
            3 -> prgBank = sr % numPrgBanks
          }
          updateChrBankMap()

          // Reset
          srCount = 5
          sr = 0x00
        }
      }
    }
    prevStep = currentStep
  }

  private fun updateChrBankMap() {
    when (chrMode) {
      CHR_MODE_0 -> {
        chr.bankMap[0] = chr0Bank and 0x1E
        chr.bankMap[1] = chr0Bank or 0x01
      }
      CHR_MODE_1 -> {
        chr.bankMap[0] = chr0Bank
        chr.bankMap[1] = chr1Bank
      }
    }
  }

  private enum class PrgMode {
    PRG_MODE_0,
    PRG_MODE_1,
    PRG_MODE_2,
    PRG_MODE_3
  }

  private enum class ChrMode {
    CHR_MODE_0,
    CHR_MODE_1
  }

  @Suppress("unused")
  companion object {
    const val PRG_RAM_SIZE = 8192
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 4096

    const val BASE_PRG_RAM = BASE_PRG_ROM - PRG_RAM_SIZE
    const val BASE_PRG0_ROM = BASE_PRG_ROM
    const val BASE_PRG1_ROM = BASE_PRG_ROM + PRG_BANK_SIZE
    const val BASE_SR = BASE_PRG_ROM
  }
}
