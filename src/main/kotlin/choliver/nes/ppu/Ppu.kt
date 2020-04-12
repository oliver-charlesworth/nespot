package choliver.nes.ppu

import choliver.nes.*
import choliver.nes.ppu.Ppu.Register.*
import mu.KotlinLogging

class Ppu(
  private val memory: Memory
) {
  private val logger = KotlinLogging.logger {}

  // http://wiki.nesdev.com/w/index.php/PPU_registers
  enum class Register {
    PPUCTRL,
    PPUMASK,
    PPUSTATUS,
    OAMADDR,
    OAMDATA,
    PPUSCROLL,
    PPUADDR,
    PPUDATA
  }

  fun readReg(reg: Int): Int {
    logger.info("Reading reg #${reg}")
    return when (reg) {
      REG_PPUSTATUS -> 0x80 // TODO - remove debug hack that emulates VBL
      else -> 0x00
    }
  } // TODO

  fun writeReg(reg: Int, data: Data) {
    logger.info("Writing 0x%02x to reg #${reg}".format(data))
  } // TODO

  fun readReg(reg: Register): Data = when (reg) {
    PPUSTATUS -> TODO()
    OAMDATA -> TODO() // Unclear what the semantics of read are
    PPUDATA -> TODO()
    else -> throw IllegalArgumentException("Attempt to read from ${reg.name}")
  }

  fun writeReg(reg: Register, data: Data) {
    when (reg) {
      PPUCTRL -> {
        addrInc = if (data.isBitSet(2)) 32 else 1
        nametableAddr = TODO()
        spriteTableAddr = if (data.isBitSet(3)) 0x1000 else 0x0000
        backgroundTableAddr = if (data.isBitSet(4)) 0x1000 else 0x0000
        isLargeSprites = data.isBitSet(5)
        // TODO - is master/slave important?
        isNmiEnabled = data.isBitSet(7)
      }
      PPUMASK -> {
        isGreyscale = data.isBitSet(0)
        isLeftmostBackgroundShown = data.isBitSet(1)
        isLeftmostSpritesShown = data.isBitSet(2)
        isBackgroundShown = data.isBitSet(3)
        isSpritesShown = data.isBitSet(4)
        isRedEmphasized = data.isBitSet(5)
        isGreenEmphasized = data.isBitSet(6)
        isBlueEmphasized = data.isBitSet(7)
      }
      OAMADDR -> oamAddr = data
      OAMDATA -> {
        oamData[oamAddr] = data.toByte()
        oamAddr++
      }
      PPUSCROLL -> TODO()
      PPUADDR -> TODO()
      PPUDATA -> TODO()
      else -> throw IllegalArgumentException("Attempt to write to ${reg.name}")
    }
  }

  // TODO - what about immutability ?

  private var addrInc: Int = 1
  private var nametableAddr: Address = 0x2000
  private var spriteTableAddr: Address = 0x0000
  private var backgroundTableAddr: Address = 0x0000
  private var isLargeSprites: Boolean = false
  private var isNmiEnabled: Boolean = false

  private var isGreyscale: Boolean = false
  private var isLeftmostBackgroundShown: Boolean = false
  private var isLeftmostSpritesShown: Boolean = false
  private var isBackgroundShown: Boolean = false
  private var isSpritesShown: Boolean = false
  private var isRedEmphasized: Boolean = false
  private var isGreenEmphasized: Boolean = false
  private var isBlueEmphasized: Boolean = false

  private var oamAddr: Address8 = 0x00    // TODO - apparently this is reset to 0 during rendering
  private val oamData = ByteArray(256)

  companion object {
    private const val REG_PPUCTRL = 0
    private const val REG_PPUMASK = 1
    private const val REG_PPUSTATUS = 2
    private const val REG_OAMADDR = 3
    private const val REG_OAMDATA = 4
    private const val REG_PPUSCROLL = 5
    private const val REG_PPUADDR = 6
    private const val REG_PPUDATA = 7
  }
}
