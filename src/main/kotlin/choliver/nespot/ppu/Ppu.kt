package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.model.State
import java.nio.IntBuffer

class Ppu(
  private val memory: Memory,
  videoBuffer: IntBuffer,
  private val oam: Memory = Ram(256),
  private val palette: Memory = Palette(),
  private val renderer: Renderer = Renderer(memory, palette, oam, videoBuffer),
  private val onVideoBufferReady: () -> Unit
) {
  private var state = State()

  val vbl get() = state.inVbl && state.vblEnabled

  fun advance(numCycles: Int) {
    repeat(numCycles * DOTS_PER_CYCLE) {
      handleDot()
      incrementFramePos()
    }
  }

  private fun incrementFramePos() {
    state.dot = (state.dot + 1) % DOTS_PER_SCANLINE
    if (state.dot == 0) {
      state.scanline = (state.scanline + 1) % SCANLINES_PER_FRAME
    }
  }

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  private fun handleDot() {
    when (state.scanline) {
      in (0 until SCREEN_HEIGHT) -> when (state.dot) {
        255 -> {
          renderer.loadAndRenderBackground(state)
          renderer.renderSprites(state)
          renderer.commitToBuffer(state)
        }
        256 -> renderer.evaluateSprites(state)
        257 -> updateCoordsForScanline()
        320 -> renderer.loadSprites(state)
      }

      (SCREEN_HEIGHT + 1) -> when (state.dot) {
        1 -> setVblFlag()
      }

      (SCANLINES_PER_FRAME - 1) -> when (state.dot) {
        1 -> clearFlags()
        255 -> renderer.loadAndRenderBackground(state) // This happens even on this line
        257 -> updateCoordsForScanline()
        280 -> updateCoordsForFrame()
        320 -> renderer.loadSprites(state)  // This happens even though we haven't evaluated sprites
      }
    }
  }

  private fun setVblFlag() {
    state.inVbl = true
    onVideoBufferReady()
  }

  private fun clearFlags() {
    state.inVbl = false
    state.sprite0Hit = false
    state.spriteOverflow = false
  }

  private fun updateCoordsForScanline() {
    state.coords.xFine = state.coordsBacking.xFine
    state.coords.xCoarse = state.coordsBacking.xCoarse
    state.coords.xNametable = state.coordsBacking.xNametable
    state.coords.incrementY()
  }

  private fun updateCoordsForFrame() {
    state.coords = state.coordsBacking.copy()
  }

  fun readReg(reg: Int) = with(state) {
    when (reg) {
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
        val ret = oam[oamAddr]
        oamAddr = (oamAddr + 1).addr8()
        ret
      }

      REG_PPUDATA -> {
        val ret = when {
          (addr < BASE_PALETTE) -> readBuffer.also { readBuffer = memory[addr] }
          else -> palette[addr and 0x001F]
        }
        addr = (addr + addrInc) and 0x7FFF
        ret
      }

      else -> 0x00
    }
  }

  fun writeReg(reg: Int, data: Data) {
    with(state) {
      when (reg) {
        REG_PPUCTRL -> {
          addrInc = if (data.isBitSet(2)) 32 else 1
          sprPatternTable = if (data.isBitSet(3)) 1 else 0
          bgPatternTable = if (data.isBitSet(4)) 1 else 0
          largeSprites = data.isBitSet(5)
          // TODO - is master/slave important?
          vblEnabled = data.isBitSet(7)
          with(coordsBacking) {
            xNametable = data and 0x01
            yNametable = (data and 0x02) shr 1
          }
        }

        REG_PPUMASK -> {
          greyscale = data.isBitSet(0)
          bgLeftTileEnabled = data.isBitSet(1)
          sprLeftTileEnabled = data.isBitSet(2)
          bgEnabled = data.isBitSet(3)
          sprEnabled = data.isBitSet(4)
          redEmphasized = data.isBitSet(5)
          greenEmphasized = data.isBitSet(6)
          blueEmphasized = data.isBitSet(7)
        }

        REG_OAMADDR -> oamAddr = data

        REG_OAMDATA -> {
          oam[oamAddr] = data
          oamAddr = (oamAddr + 1).addr8()
        }

        REG_PPUSCROLL -> {
          val fine = (data and 0b00000111)
          val coarse = (data and 0b11111000) shr 3

          with(coordsBacking) {
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
            with(coordsBacking) {
              yCoarse = ((data and 0b00000011) shl 3) or (yCoarse and 0b00111)
              xNametable = (data and 0b00000100) shr 2
              yNametable = (data and 0b00001000) shr 3
              yFine = (data and 0b00110000) shr 4  // Lose the top bit
            }
          } else {
            addr = addr(lo = data, hi = addr.hi())
            with(coordsBacking) {
              xCoarse = (data and 0b00011111)
              yCoarse = ((data and 0b11100000) shr 5) or (yCoarse and 0b11000)
            }
            coords = coordsBacking.copy()   // Propagate immediately
          }
          w = !w
        }

        REG_PPUDATA -> {
          when {
            addr < BASE_PALETTE -> memory[addr] = data
            else -> palette[addr and 0x1F] = data
          }
          addr = (addr + addrInc) and 0x7FFF
        }
      }
    }
  }

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Ppu.state
      set(value) { this@Ppu.state = value }
    val renderer = this@Ppu.renderer.diagnostics
    val oam = this@Ppu.oam
    val palette = this@Ppu.palette
  }

  val diagnostics = Diagnostics()

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
  }
}
