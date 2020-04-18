package choliver.nes.ppu

import choliver.nes.*
import java.nio.IntBuffer

class Ppu(
  private val memory: Memory,
  screen: IntBuffer,
  private val onVbl: () -> Unit
) {
  private var state = State()
  private val palette = Palette()
  private val oam = Ram(256)
  private val renderer = Renderer(memory, palette, oam, screen)
  private var nextScanline = 0

  private var gross = false

  // TODO - add a reset (to clean up counters and stuff)

  fun renderNextScanline() {
    renderer.renderScanline(
      y = nextScanline,
      ctx = Renderer.Context(
        nametableAddr = state.nametableAddr,
        bgPatternTableAddr = state.bgPatternTableAddr,
        sprPatternTableAddr = state.sprPatternTableAddr
      )
    )
    nextScanline++
    if (nextScanline == SCREEN_HEIGHT) {
      if (state.isVblEnabled) {
        onVbl()
      }
      nextScanline = 0
    }
    // TODO - should we account for VBL timing somewhere?
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        // Reset stuff
        state = state.copy(
          addrWriteLo = false,
          addr = 0
        )
        gross = !gross
        0x80 + if (gross) 0x40 else 0x00 // TODO - remove debug hack that emulates VBL
      }

      REG_OAMDATA -> {
        val ret = oam.load(state.oamAddr)
        state = state.withincrementedOamAddr()
        ret
      }

      REG_PPUDATA -> {
        val ret = when {
          state.addr < BASE_PALETTE -> {
            val ret = state.ppuReadBuffered
            state = state.copy(ppuReadBuffered = memory.load(state.addr))
            ret
          }
          else -> palette.load(state.addr and 0x1F)
        }
        state = state.withIncrementedPpuAddr()
        ret
      }
      else -> 0x00
    }
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_PPUCTRL -> state = state.copy(
        addrInc = if (data.isBitSet(2)) 32 else 1,
        nametableAddr = 0x2000, // TODO
        sprPatternTableAddr = if (data.isBitSet(3)) 0x1000 else 0x0000,
        bgPatternTableAddr = if (data.isBitSet(4)) 0x1000 else 0x0000,
        isLargeSprites = data.isBitSet(5),
        // TODO - is master/slave important?
        isVblEnabled = data.isBitSet(7)
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
        state = state.withincrementedOamAddr()
      }

      REG_PPUSCROLL -> {} // TODO

      // TODO - this probably latches the data on second write
      REG_PPUADDR -> state = state.copy(
        addr = if (state.addrWriteLo) {
          addr(lo = data, hi = state.addr.hi())
        } else {
          addr(lo = state.addr.lo(), hi = data)
        },
        addrWriteLo = !state.addrWriteLo
      )

      REG_PPUDATA -> {
        when {
          state.addr < BASE_PALETTE -> memory.store(state.addr, data)
          else -> palette.store(state.addr and 0x1F, data)
        }
        state = state.withIncrementedPpuAddr()
      }

      else -> throw IllegalArgumentException("Attempt to write to reg #${reg}")   // Should never happen
    }
  }

  private fun State.withincrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())
  private fun State.withIncrementedPpuAddr() = copy(addr = (state.addr + addrInc).addr())

  private data class State(
    val addrInc: Int = 1,
    val nametableAddr: Address = 0x2000,
    val sprPatternTableAddr: Address = 0x0000,
    val bgPatternTableAddr: Address = 0x0000,
    val isLargeSprites: Boolean = false,
    val isVblEnabled: Boolean = false,

    val isGreyscale: Boolean = false,
    val isLeftmostBackgroundShown: Boolean = false,
    val isLeftmostSpritesShown: Boolean = false,
    val isBackgroundShown: Boolean = false,
    val isSpritesShown: Boolean = false,
    val isRedEmphasized: Boolean = false,
    val isGreenEmphasized: Boolean = false,
    val isBlueEmphasized: Boolean = false,

    val addrWriteLo: Boolean = false, // TODO - better name, or encapsulation
    val addr: Address = 0x0000,
    val ppuReadBuffered: Data = 0x00,

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

    const val BASE_PALETTE: Address = 0x3F00
  }
}
