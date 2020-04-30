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
  private var inVbl = false
  private var sprite0Hit = false
  private var spriteOverflow = false

  private var addr: Address = 0x0000
  private var w = false

  private var readBuffer: Data = 0

  // TODO - model addr increment quirks during rendering (see wiki.nesdev.com/w/index.php/PPU_scrolling)
  // TODO - add a reset (to clean up counters and stuff)

  val scanline get() = state.renderer.scanline

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (state.renderer.scanline) {
      in (0 until SCREEN_HEIGHT) -> {
        with(state.renderer) {
          if (bgEnabled || sprEnabled) {
            when (scanline) {
              0 -> coords = state.coords.copy()
              else -> {
                coords.xFine = state.coords.xFine
                coords.xCoarse = state.coords.xCoarse
                coords.xNametable = state.coords.xNametable
                coords.incrementY()
              }
            }
          }
        }

        val result = renderer.renderScanline(state.renderer)
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

    state.renderer.scanline = (state.renderer.scanline + 1) % NUM_SCANLINES
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
      REG_PPUCTRL -> with(state) {
        addrInc = if (data.isBitSet(2)) 32 else 1
        with(renderer) {
          sprPatternTable = if (data.isBitSet(3)) 1 else 0
          bgPatternTable = if (data.isBitSet(4)) 1 else 0
          largeSprites = data.isBitSet(5)
          // TODO - is master/slave important?
        }
        isVblEnabled = data.isBitSet(7)
        with(coords) {
          xNametable = data and 0x01
          yNametable = (data and 0x02) shr 1
        }
      }

      REG_PPUMASK -> with(state) {
        isGreyscale = data.isBitSet(0)
        with(renderer) {
          bgLeftTileEnabled = data.isBitSet(1)
          sprLeftTileEnabled = data.isBitSet(2)
          bgEnabled = data.isBitSet(3)
          sprEnabled = data.isBitSet(4)
        }
        isRedEmphasized = data.isBitSet(5)
        isGreenEmphasized = data.isBitSet(6)
        isBlueEmphasized = data.isBitSet(7)
      }

      REG_OAMADDR -> state.oamAddr = data

      REG_OAMDATA -> {
        oam.store(state.oamAddr, data)
        state = state.withIncrementedOamAddr()
      }

      REG_PPUSCROLL -> {
        val fine   = (data and 0b00000111)
        val coarse = (data and 0b11111000) shr 3

        with(state.coords) {
          if (!w) {
            xCoarse = coarse
            xFine = fine
          } else {
            yCoarse = coarse
            yFine = fine
          }
        }
        w = !w
      }

      REG_PPUADDR -> {
        // Address and scroll settings share registers, so also update coords accordingly.
        // Super Mario Bros split scroll breaks if we don't do this.
        // See http://wiki.nesdev.com/w/index.php/PPU_scrolling#Summary
        if (!w) {
          addr = addr(lo = addr.lo(), hi = data and 0b00111111)
          with(state.coords) {
            yCoarse = ((data and 0b00000011) shl 3) or (yCoarse and 0b00111)
            xNametable = (data and 0b00000100) shr 2
            yNametable = (data and 0b00001000) shr 3
            yFine = (data and 0b00110000) shr 4  // Lose the top bit
          }
        } else {
          addr = addr(lo = data, hi = addr.hi())
          with(state.coords) {
            xCoarse = (data and 0b00011111)
            yCoarse = ((data and 0b11100000) shr 5) or (yCoarse and 0b11000)
          }
          // TODO - should probably happen *after* the incrementY() above
          state.renderer.coords = state.coords.copy()   // Propagate immediately
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

  private fun State.withIncrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())

  @MutableForPerfReasons
  private data class State(
    var addrInc: Int = 1,
    val renderer: Context = Context(
      bgEnabled = false,
      sprEnabled = false,
      bgLeftTileEnabled = false,
      sprLeftTileEnabled = false,
      largeSprites = false,
      bgPatternTable = 0,
      sprPatternTable = 0,
      coords = Coords(),
      scanline = 0
    ),
    var isVblEnabled: Boolean = false,
    var isGreyscale: Boolean = false,
    var isRedEmphasized: Boolean = false,
    var isGreenEmphasized: Boolean = false,
    var isBlueEmphasized: Boolean = false,
    var coords: Coords = Coords(),
    var oamAddr: Address8 = 0x00    // TODO - apparently this is reset to 0 during rendering
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
