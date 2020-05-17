package choliver.nespot.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.mirrorHorizontal
import choliver.nespot.cartridge.mirrorVertical

// https://wiki.nesdev.com/w/index.php/MMC1
class Mmc1Mapper(
  private val rom: Rom,
  private val getStepCount: () -> Int
) : Mapper {
  private val prgRam = Ram(PRG_RAM_SIZE)
  private val chrRam = Ram(CHR_RAM_SIZE)
  private val usingChrRam = rom.chrData.isEmpty()
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  private val numChrBanks = if (usingChrRam) (CHR_RAM_SIZE / CHR_BANK_SIZE) else (rom.chrData.size / CHR_BANK_SIZE)
  private var srCount = 0
  private var sr = 0
  private var chr0Bank = 0
  private var chr1Bank = 0
  private var prgBank = numPrgBanks - 1   // Bubble Bobble relies on this to start up
  private var mirrorMode = 0
  private var chrMode = 0
  private var prgMode = 0
  private var prevStep = -1

  override val irq = false
  override val persistentRam = prgRam

  override val prg = object : Memory {
    override fun get(addr: Address) = when {
      addr < BASE_PRG0_ROM -> prgRam[addr and 0x1FFF]
      addr < BASE_PRG1_ROM -> getFromBank(addr, when (prgMode) {
        0, 1 -> (prgBank and 0x0E) // 32k mode
        2 -> 0 // Fixed
        3 -> prgBank // Variable
        else -> throw IllegalArgumentException()  // Should never happen
      })
      else -> getFromBank(addr, when (prgMode) {
        0, 1 -> (prgBank or 0x01) // 32k mode
        2 -> prgBank  // Variable
        3 -> numPrgBanks - 1 // Fixed
        else -> throw IllegalArgumentException()  // Should never happen
      })
    }

    private fun getFromBank(addr: Address, iBank: Int) = rom.prgData[(addr % PRG_BANK_SIZE) + iBank * PRG_BANK_SIZE].data()

    override fun set(addr: Address, data: Data) {
      when {
        addr < BASE_PRG0_ROM -> prgRam[addr and 0x1FFF] = data
        addr >= BASE_SR -> updateShiftRegister(addr, data)
      }
    }
  }

  override fun chr(vram: Memory) = object : Memory {
    override fun get(addr: Address) = when {
      addr >= BASE_VRAM -> vram[mapToVram(addr)]  // This maps everything >= 0x4000 too
      addr < BASE_CHR1_ROM -> load(mapToBank(addr, when (chrMode) {
        0 -> (chr0Bank and 0x1E)
        1 -> chr0Bank
        else -> throw IllegalArgumentException()  // Should never happen
      }))
      else -> load(mapToBank(addr, when (chrMode) {
        0 -> (chr0Bank or 0x01)
        1 -> chr1Bank
        else -> throw IllegalArgumentException()  // Should never happen
      }))
    }

    override fun set(addr: Address, data: Data) {
      when {
        addr >= BASE_VRAM -> vram[mapToVram(addr)] = data  // This maps everything >= 0x4000 too
        addr < BASE_CHR1_ROM -> store(mapToBank(addr, when (chrMode) {
          0 -> (chr0Bank and 0x1E)
          1 -> chr0Bank
          else -> throw IllegalArgumentException()  // Should never happen
        }), data)
        else -> store(mapToBank(addr, when (chrMode) {
          0 -> (chr0Bank or 0x01)
          1 -> chr1Bank
          else -> throw IllegalArgumentException()  // Should never happen
        }), data)
      }
    }

    private fun load(addr: Address) = if (usingChrRam) chrRam[addr] else rom.chrData[addr].data()

    private fun store(addr: Address, data: Data) {
      if (usingChrRam) {
        chrRam[addr] = data
      }
    }

    private fun mapToBank(addr: Address, iBank: Int): Address = (addr % CHR_BANK_SIZE) + iBank * CHR_BANK_SIZE

    private fun mapToVram(addr: Address): Address = when (mirrorMode) {
      0 -> (addr and 1023)
      1 -> (addr and 1023) + 1024
      2 -> mirrorVertical(addr)
      3 -> mirrorHorizontal(addr)
      else -> throw UnsupportedOperationException()   // Should never happen
    }
  }

  private fun updateShiftRegister(addr: Address, data: Data) {
    val currentStep = getStepCount()
    if (currentStep != prevStep) {
      println("[%04x] <- %02x".format(addr, data))
      if (data.isBitSet(7)) {
        // Reset
        srCount = 5
        sr = 0x00
        println("Reset")
      } else {
        sr = (sr shr 1) or ((data and 1) shl 4)
        println("SR = %02x".format(sr))
        if (--srCount == 0) {
          when ((addr and 0x6000) shr 13) {
            0 -> {
              mirrorMode = (sr and 0x03)
              prgMode = ((sr and 0x0C) shr 2).also { println("PRG mode = ${it}") }
              chrMode = ((sr and 0x10) shr 4).also { println("CHR mode = ${it}") }
            }
            1 -> chr0Bank = (sr % numChrBanks).also { println("CHR 0 bank = ${it}") }
            2 -> chr1Bank = (sr % numChrBanks).also { println("CHR 1 bank = ${it}") }
            3 -> prgBank = (sr % numPrgBanks).also { println("PRG bank = ${it}") }
          }
          // Reset
          srCount = 5
          sr = 0x00
        }
      }
    }
    prevStep = currentStep
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG_RAM = 0x6000
    const val BASE_PRG0_ROM = 0x8000
    const val BASE_PRG1_ROM = 0xC000
    const val BASE_CHR0_ROM = 0x0000
    const val BASE_CHR1_ROM = 0x1000
    const val BASE_VRAM = 0x2000
    const val BASE_SR = 0x8000

    const val PRG_RAM_SIZE = 8192
    const val CHR_RAM_SIZE = 8192
    const val PRG_BANK_SIZE = 16384
    const val CHR_BANK_SIZE = 4096
  }
}
