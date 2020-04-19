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
  private var isSprite0Hit = false
  private var iNametable: Int = 0
  private var addr: Address = 0
  private var scrollX: Data = 0
  private var scrollY: Data = 0
  private var w: Boolean = false
  private var readBuffer: Data = 0

  // TODO - model addr increment quirks during rendering (see wiki.nesdev.com/w/index.php/PPU_scrolling)
  // TODO - add a reset (to clean up counters and stuff)
  // TODO - only do rendering increments if rendering enabled

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (scanline) {
      in (0 until SCREEN_HEIGHT) -> {
        val isHit = renderer.renderScanlineAndDetectHit(
          y = scanline,
          ctx = Renderer.Context(
            bgPatternTableAddr = state.bgPatternTableAddr,
            sprPatternTableAddr = state.sprPatternTableAddr,
            scrollX = scrollX + iNametable * SCREEN_WIDTH,  // TODO - this is wrong for all four nametables
            scrollY = scrollY // TODO - just assert for non-zero scrollY for now
          )
        )
        isSprite0Hit = isSprite0Hit || isHit
      }

      (SCREEN_HEIGHT + 1) -> {
        inVbl = true

        // TODO - this is set if isVblEnabled *becomes* true during VBL phase
        if (state.isVblEnabled) {
          onVbl()
        }
      }

      (NUM_SCANLINES - 1) -> {
        inVbl = false
        isSprite0Hit = false
      }
    }

    scanline = (scanline + 1) % NUM_SCANLINES
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        val ret = 0 +
          (if (inVbl) 0x80 else 0x00) +
          (if (isSprite0Hit) 0x40 else 0x00)

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
        iNametable = data and 0x03
        println("iNametable at #${scanline}: ${iNametable}")

        state = state.copy(
          addrInc = if (data.isBitSet(2)) 32 else 1,
          nametableAddr = BASE_NAMETABLES + ((data and 0x03) * NAMETABLE_SIZE_BYTES),
          sprPatternTableAddr = BASE_PATTERN_TABLES + (if (data.isBitSet(3)) PATTERN_TABLE_SIZE_BYTES else 0),
          bgPatternTableAddr = BASE_PATTERN_TABLES + (if (data.isBitSet(4)) PATTERN_TABLE_SIZE_BYTES else 0),
          isLargeSprites = data.isBitSet(5),
          // TODO - is master/slave important?
          isVblEnabled = data.isBitSet(7)
        )
      }

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
        state = state.withIncrementedOamAddr()
      }

      REG_PPUSCROLL -> {
        if (!w) {
          println("scrollX at #${scanline}: ${scrollX}")
          scrollX = data
        } else {
          scrollY = data
        }
        w = !w
      }

      REG_PPUADDR -> {
        addr = if (!w) {
          addr(lo = addr.lo(), hi = data)
        } else {
          addr(lo = data, hi = addr.hi())
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

      else -> throw IllegalArgumentException("Attempt to write to reg #${reg}")   // Should never happen
    }
  }

  private fun State.withIncrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())

  private data class State(
    val addrInc: Int = 1,
    val nametableAddr: Address = BASE_NAMETABLES,
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

    const val BASE_PATTERN_TABLES: Address = 0x0000
    const val BASE_NAMETABLES: Address = 0x2000
    const val BASE_PALETTE: Address = 0x3F00

    const val NAMETABLE_SIZE_BYTES = 0x0400
    const val PATTERN_TABLE_SIZE_BYTES = 0x1000

    const val NUM_SCANLINES = 262
  }
}
