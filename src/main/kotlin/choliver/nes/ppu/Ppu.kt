package choliver.nes.ppu

import choliver.nes.*
import mu.KotlinLogging

class Ppu(
  private val memory: Memory
) {
  private val logger = KotlinLogging.logger {}
  private var state = State()
  private val oam = Ram(256)

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> 0x80 // TODO - remove debug hack that emulates VBL
      else -> 0x00
    }
  } // TODO

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_PPUCTRL -> state = state.copy(
        addrInc = if (data.isBitSet(2)) 32 else 1,
        nametableAddr = 0, // TODO
        spriteTableAddr = if (data.isBitSet(3)) 0x1000 else 0x0000,
        backgroundTableAddr = if (data.isBitSet(4)) 0x1000 else 0x0000,
        isLargeSprites = data.isBitSet(5),
        // TODO - is master/slave important?
        isNmiEnabled = data.isBitSet(7)
      )

      REG_PPUMASK -> state = state.copy(
        isGreyscale = data.isBitSet(0),
        isLeftmostBackgroundShown = data.isBitSet(1),
        isLeftmostSpritesShown = data.isBitSet(2),
        isBackgroundShown = data.isBitSet(3),
        isSpritesShown = data.isBitSet(4),
        isRedEmphasized = data.isBitSet(5),
        isGreenEmphasized = data.isBitSet(6),
        isBlueEmphasized = data.isBitSet(7)
      )


      REG_OAMADDR -> state = state.copy(oamAddr = data)

      REG_OAMDATA -> {
        oam.store(state.oamAddr, data)
        state = state.copy(oamAddr = (state.oamAddr + 1).addr8())
      }

      REG_PPUSCROLL -> {} // TODO

      REG_PPUADDR -> {} // TODO

      REG_PPUDATA -> {} // TODO

      else -> throw IllegalArgumentException("Attempt to write to reg #${reg}")   // Should never happen
    }

  }

  private data class State(
    val addrInc: Int = 1,
    val nametableAddr: Address = 0x2000,
    val spriteTableAddr: Address = 0x0000,
    val backgroundTableAddr: Address = 0x0000,
    val isLargeSprites: Boolean = false,
    val isNmiEnabled: Boolean = false,

    val isGreyscale: Boolean = false,
    val isLeftmostBackgroundShown: Boolean = false,
    val isLeftmostSpritesShown: Boolean = false,
    val isBackgroundShown: Boolean = false,
    val isSpritesShown: Boolean = false,
    val isRedEmphasized: Boolean = false,
    val isGreenEmphasized: Boolean = false,
    val isBlueEmphasized: Boolean = false,

    val oamAddr: Address8 = 0x00    // TODO - apparently this is reset to 0 during rendering
  )



  companion object {
    // http://wiki.nesdev.com/w/index.php/PPU_registers
    const val REG_PPUCTRL = 0
    const val REG_PPUMASK = 1
    const val REG_PPUSTATUS = 2
    const val REG_OAMADDR = 3
    const val REG_OAMDATA = 4
    const val REG_PPUSCROLL = 5
    const val REG_PPUADDR = 6
    const val REG_PPUDATA = 7
  }
}
