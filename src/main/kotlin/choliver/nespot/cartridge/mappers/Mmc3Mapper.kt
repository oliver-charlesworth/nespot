package choliver.nespot.cartridge.mappers

import choliver.nespot.*
import choliver.nespot.cartridge.BASE_VRAM
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.Rom

// See https://wiki.nesdev.com/w/index.php/MMC3
class Mmc3Mapper(private val rom: Rom) : Mapper {
  private val prgRam = Ram(8192)
  private val numPrgBanks = (rom.prgData.size / PRG_BANK_SIZE)
  private var mirrorModeFlag = false
  private var chrModeFlag = false
  private var prgModeFlag = false
  private var regSelect = 0
  private val regs = IntArray(8) { 0 }
  private var irqReload = false
  private var irqReloadValue: Data = 0x00
  private var irqEnabled = false
  private var irqCounter = 0x00
  private var prevA12 = false

  override val irq get() = irqEnabled && (irqCounter == 0)

  override val prg = object : Memory {
    override operator fun get(addr: Address) = when {
      (addr >= BASE_PRG_ROM) -> {
        val iBank = when ((addr and 0x6000) shr 13) {
          0 -> if (prgModeFlag) (numPrgBanks - 2) else regs[6]
          1 -> regs[7]
          2 -> if (prgModeFlag) regs[6] else (numPrgBanks - 2)
          3 -> (numPrgBanks - 1)
          else -> throw IllegalArgumentException()  // Should never happen
        }
        rom.prgData[(addr and (PRG_BANK_SIZE - 1)) + PRG_BANK_SIZE * iBank].data()
      }
      (addr >= BASE_PRG_RAM) -> prgRam[addr and 0x1FFF]
      else -> 0x00
    }

    override fun set(addr: Address, data: Data) {
      val even = (addr % 2) == 0
      when {
        (addr >= BASE_PRG_ROM) -> {
          when ((addr and 0x6000) shr 13) {
            0 -> when (even) {
              false -> regs[regSelect] = data
              true -> {
                chrModeFlag = data.isBitSet(7)
                prgModeFlag = data.isBitSet(6)
                regSelect = data and 0b00000111
              }
            }
            1 -> when (even) {
              false -> Unit // TODO - PRG-RAM protect
              true -> mirrorModeFlag = data.isBitSet(0)
            }
            2 -> when (even) {
              false -> irqReload = true
              true -> irqReloadValue = data
            }
            3 -> when (even) {
              false -> irqEnabled = true
              true -> irqEnabled = false  // TODO - "acknowledge any pending interrupts"
            }
          }
        }
        (addr >= BASE_PRG_RAM) -> prgRam[addr and 0x1FFF] = data
      }
    }
  }

  override fun chr(vram: Memory) = object : Memory {
    override fun get(addr: Address): Data {

      return when {
        (addr >= BASE_VRAM) -> vram[mapToVram(addr)]  // This maps everything >= 0x4000 too
        else -> {
          updateIrqState(addr)

          val a = if (chrModeFlag) (addr xor 0x1000) else addr
          val iBank = when (a shr 10) {
            0 -> regs[0] and 0xFE
            1 -> regs[0] or 0x01
            2 -> regs[1] and 0xFE
            3 -> regs[1] or 0x01
            4 -> regs[2]
            5 -> regs[3]
            6 -> regs[4]
            7 -> regs[5]
            else -> throw IllegalArgumentException()  // Should never happen
          }
          rom.chrData[(a and 0x03FF) + 0x0400 * iBank].data()
        }
      }
    }

    override fun set(addr: Address, data: Data) {
//      updateIrqState(addr)
      when {
        (addr >= BASE_VRAM) -> vram[mapToVram(addr)] = data  // This maps everything >= 0x4000 too
      }
    }

    private fun mapToVram(addr: Address): Address = when (mirrorModeFlag) {
      false -> (addr and 2047)
      true -> (addr and 1023) or ((addr and 2048) shr 1)
    }

    private fun updateIrqState(addr: Address) {
      val newA12 = addr.isBitSet(12)

      if (!prevA12 && newA12) {
        if ((irqCounter == 0) || irqReload) {
//          println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx")
          irqReload = false
          irqCounter = irqReloadValue
        } else {
          irqCounter--
        }
        println("irqCounter = ${irqCounter}")
      }
      prevA12 = newA12
    }
  }

  @Suppress("unused")
  companion object {
    const val BASE_PRG_RAM = 0x6000
    const val BASE_PRG_ROM = 0x8000
    const val BASE_REG = 0x8000

    const val PRG_BANK_SIZE = 0x2000
    const val CHR_BANK_SIZE = 0x0400
  }
}
