package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.Renderer.Context
import java.nio.IntBuffer

class Ppu(
  private val memory: Memory,
  videoBuffer: IntBuffer,
  private val onVbl: () -> Unit,
  private val oam: Memory = Ram(256),
  private val palette: Memory = Palette(),
  private val renderer: Renderer = Renderer(memory, palette, oam, videoBuffer)
) {
  private var state = State()
  private var _scanline = 0
  private var inVbl = false
  private var sprite0Hit = false
  private var spriteOverflow = false

  private var addr: Address = 0x0000
  private val coords = Coords()
  private var coordsWorking = Coords()
  private var w = false

  private var readBuffer: Data = 0

  // TODO - model addr increment quirks during rendering (see wiki.nesdev.com/w/index.php/PPU_scrolling)
  // TODO - add a reset (to clean up counters and stuff)

  val scanline get() = _scanline

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (_scanline) {
      in (0 until SCREEN_HEIGHT) -> {
        if (renderingEnabled()) {
          when (_scanline) {
            0 -> coordsWorking = coords.copy()
            else -> {
              coordsWorking.xFine = coords.xFine
              coordsWorking.xCoarse = coords.xCoarse
              coordsWorking.xNametable = coords.xNametable
              coordsWorking.incrementY()
            }
          }
        }

        val result = renderer.renderScanline(Context(
          // TODO - clean up "state" situation
          bgRenderingEnabled = state.bgRenderingEnabled,
          sprRenderingEnabled = state.sprRenderingEnabled,
          largeSprites = state.largeSprites,
          bgPatternTable = state.bgPatternTable,
          sprPatternTable = state.sprPatternTable,
          coords = coordsWorking,
          yScanline = scanline
        ))
        sprite0Hit = sprite0Hit || result.sprite0Hit
        spriteOverflow = spriteOverflow || result.spriteOverflow
      }

      (SCREEN_HEIGHT + 1) -> {
        inVbl = true

        // TODO - this is set if isVblEnabled *becomes* true during VBL phase
        if (state.isVblEnabled) {
          onVbl()
        }
      }

      // Pre-render line
      (NUM_SCANLINES - 1) -> {
        inVbl = false
        sprite0Hit = false
        spriteOverflow = false
      }
    }

    _scanline = (_scanline + 1) % NUM_SCANLINES
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        val ret = 0 +
          (if (inVbl) 0x80 else 0x00) +
          (if (sprite0Hit) 0x40 else 0x00) +
          (if (spriteOverflow) 0x20 else 0x00)

        // Reset stuff
        inVbl = false
        w = false
        ret
      }

      REG_OAMDATA -> {
        val ret = oam.load(state.oamAddr)
        state = state.withIncrementedOamAddr()
        ret
      }

      REG_PPUDATA -> {
        val ret = when {
          (addr < BASE_PALETTE) -> readBuffer.also { readBuffer = memory.load(addr) }
          else -> palette.load(addr and 0x001F)
        }
        addr = (addr + state.addrInc) and 0x7FFF
        ret
      }

      else -> 0x00
    }
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_PPUCTRL -> {
        coords.xNametable = data and 0x01
        coords.yNametable = (data and 0x02) shr 1

        state = state.copy(
          addrInc = if (data.isBitSet(2)) 32 else 1,
          sprPatternTable = if (data.isBitSet(3)) 1 else 0,
          bgPatternTable = if (data.isBitSet(4)) 1 else 0,
          largeSprites = data.isBitSet(5),
          // TODO - is master/slave important?
          isVblEnabled = data.isBitSet(7)
        )
      }

      REG_PPUMASK -> state = state.copy(
        isGreyscale = data.isBitSet(0),
        isLeftmostBackgroundShown = data.isBitSet(1),
        isLeftmostSpritesShown = data.isBitSet(2),
        bgRenderingEnabled = data.isBitSet(3),
        sprRenderingEnabled = data.isBitSet(4),
        isRedEmphasized = data.isBitSet(5),
        isGreenEmphasized = data.isBitSet(6),
        isBlueEmphasized = data.isBitSet(7)
      )

      REG_OAMADDR -> state = state.copy(oamAddr = data)

      REG_OAMDATA -> {
        oam.store(state.oamAddr, data)
        state = state.withIncrementedOamAddr()
      }

      REG_PPUSCROLL -> {
        val fine   = (data and 0b00000111)
        val coarse = (data and 0b11111000) shr 3

        if (!w) {
          coords.xCoarse = coarse
          coords.xFine = fine
        } else {
          coords.yCoarse = coarse
          coords.yFine = fine
        }
        w = !w
      }

      REG_PPUADDR -> {
        // Address and scroll settings share registers, so also update coords accordingly.
        // Super Mario Bros split scroll breaks if we don't do this.
        // See http://wiki.nesdev.com/w/index.php/PPU_scrolling#Summary
        if (!w) {
          addr = addr(lo = addr.lo(), hi = data and 0b00111111)
          coords.yCoarse    = ((data and 0b00000011) shl 3) or (coords.yCoarse and 0b00111)
          coords.xNametable =  (data and 0b00000100) shr 2
          coords.yNametable =  (data and 0b00001000) shr 3
          coords.yFine      =  (data and 0b00110000) shr 4  // Lose the top bit
        } else {
          addr = addr(lo = data, hi = addr.hi())
          coords.xCoarse =  (data and 0b00011111)
          coords.yCoarse = ((data and 0b11100000) shr 5) or (coords.yCoarse and 0b11000)
          // TODO - should probably happen *after* the incrementY() above
          coordsWorking = coords.copy()   // Propagate immediately
        }
        w = !w
      }

      REG_PPUDATA -> {
        when {
          addr < BASE_PALETTE -> memory.store(addr, data)
          else -> palette.store(addr and 0x1F, data)
        }
        addr = (addr + state.addrInc) and 0x7FFF
      }
    }
  }

  private fun renderingEnabled() = state.bgRenderingEnabled || state.sprRenderingEnabled

  private fun State.withIncrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())

  private data class State(
    val addrInc: Int = 1,
    val sprPatternTable: Int = 0,
    val bgPatternTable: Int = 0,
    val largeSprites: Boolean = false,
    val isVblEnabled: Boolean = false,

    val isGreyscale: Boolean = false,
    val isLeftmostBackgroundShown: Boolean = false,
    val isLeftmostSpritesShown: Boolean = false,
    val bgRenderingEnabled: Boolean = false,
    val sprRenderingEnabled: Boolean = false,
    val isRedEmphasized: Boolean = false,
    val isGreenEmphasized: Boolean = false,
    val isBlueEmphasized: Boolean = false,

    val oamAddr: Address8 = 0x00    // TODO - apparently this is reset to 0 during rendering
  )

  @Suppress("unused")
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

    const val BASE_PATTERN_TABLES: Address = 0x0000
    const val BASE_NAMETABLES: Address = 0x2000
    const val BASE_PATTERNS: Address = 0x0000
    const val BASE_PALETTE: Address = 0x3F00

    const val NAMETABLE_SIZE_BYTES = 0x0400
    const val PATTERN_TABLE_SIZE_BYTES = 0x1000

    const val NUM_SCANLINES = 262
  }
}
