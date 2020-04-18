package choliver.nes.ppu

import choliver.nes.*
import java.nio.IntBuffer

class Ppu(
  private val memory: Memory,
  screen: IntBuffer,
  private val onVbl: () -> Unit,
  private val oam: Memory = Ram(256),
  private val palette: Memory = Palette(),
  private val renderer: Renderer = Renderer(memory, palette, oam, screen)
) {
  private var state = State()
  private var scanline = 0
  private var inVbl = false

  private var gross = false

  // TODO - add a reset (to clean up counters and stuff)

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (scanline) {
      in (0 until SCREEN_HEIGHT) -> renderer.renderScanline(
        y = scanline,
        ctx = Renderer.Context(
          nametableAddr = state.nametableAddr,
          bgPatternTableAddr = state.bgPatternTableAddr,
          sprPatternTableAddr = state.sprPatternTableAddr
        )
      )

      (SCREEN_HEIGHT + 1) -> {
        inVbl = true

        // TODO - this is set if isVblEnabled *becomes* true during VBL phase
        if (state.isVblEnabled) {
          onVbl()
        }
      }

      (NUM_SCANLINES - 1) -> {
        inVbl = false
      }
    }

    scanline = (scanline + 1) % NUM_SCANLINES
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        val ret =
          (if (inVbl) 0x80 else 0x00) +
            (if (gross) 0x40 else 0x00)

        // Reset stuff
        state = state.copy(
          addrWriteLo = false,
          addr = 0
        )
        inVbl = false
        gross = !gross

        ret
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

    const val NUM_SCANLINES = 262
  }
}
