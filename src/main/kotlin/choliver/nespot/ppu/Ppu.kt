package choliver.nespot.ppu

import choliver.nespot.*
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
  private var isSprite0Hit = false
  private var iNametable: Int = 0
  private var scrollX: Data = 0
  private var scrollY: Data = 0

  private var t: Int = 0
  private var v: Int = 0
  private var x: Int = 0
  private var w: Boolean = false

  private var readBuffer: Data = 0

  // TODO - model addr increment quirks during rendering (see wiki.nesdev.com/w/index.php/PPU_scrolling)
  // TODO - add a reset (to clean up counters and stuff)
  // TODO - only do rendering increments if rendering enabled

  val scanline get() = _scanline

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (_scanline) {
      in (0 until SCREEN_HEIGHT) -> {
        if (renderingEnabled()) {
          if (scanline > 0) {
            updateV()
          }
        }

        val isHit = renderer.renderScanlineAndDetectHit(
          y = _scanline,
          ctx = Renderer.Context(
            isLargeSprites = state.isLargeSprites,
            bgPatternTable = state.bgPatternTable,
            sprPatternTable = state.sprPatternTable,
            addrStart = (v and 0x0FFF),
            fineX = x,
            fineY = (v and 0x7000) shr 12
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

      // Pre-render line
      (NUM_SCANLINES - 1) -> {
        inVbl = false
        isSprite0Hit = false
        if (renderingEnabled()) {
          v = t // TODO - may have to move this to beginning of next scanline
        }
      }
    }

    _scanline = (_scanline + 1) % NUM_SCANLINES
  }

  private fun updateV() {
    // Copy horizontal bits from t
    v = (v and 0x7BE0) or (t and 0x041F)

    var fineY = (v and 0x7000) shr 12
    val coarseX = (v and 0x001F)
    var coarseY = (v and 0x03E0) shr 5
    var iNametable = (v and 0x0C00) shr 10

    if (fineY < 7) {
      fineY++
    } else {
      fineY = 0

      when (coarseY) {
        29 -> {
          coarseY = 0
          iNametable = iNametable xor 2
        }
        31 -> {
          coarseY = 0
        }
        else -> {
          coarseY++
        }
      }
    }

    v = (fineY shl 12) or (iNametable shl 10) or (coarseY shl 5) or coarseX
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
          (v < BASE_PALETTE) -> readBuffer.also { readBuffer = memory.load(v) }
          else -> palette.load(v and 0x001F)
        }
        v = (v + state.addrInc) and 0x7FFF
        ret
      }
      else -> 0x00
    }
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_PPUCTRL -> {
        iNametable = data and 0x03
        t = (iNametable shl 10) or (t and 0x73FF)

        state = state.copy(
          addrInc = if (data.isBitSet(2)) 32 else 1,
          nametableAddr = BASE_NAMETABLES + ((data and 0x03) * NAMETABLE_SIZE_BYTES),
          sprPatternTable = if (data.isBitSet(3)) 1 else 0,
          bgPatternTable = if (data.isBitSet(4)) 1 else 0,
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
        val fine = data and 0x07
        val coarse = (data and 0xF8) shr 3

        if (!w) {
          x = fine
          t = coarse or (t and 0x7FE0)
          scrollX = data
        } else {
          t = (fine shl 12) or (coarse shl 5) or (t and 0x0C1F)
          scrollY = data
        }
        w = !w
      }

      REG_PPUADDR -> {
        if (!w) {
          t = ((data and 0x3F) shl 8) or (t and 0x00FF)
        } else {
          t = (data and 0xFF) or (t and 0x7F00)
          v = t
        }
        w = !w
      }

      REG_PPUDATA -> {
        when {
          v < BASE_PALETTE -> memory.store(v, data)
          else -> palette.store(v and 0x1F, data)
        }
        v = (v + state.addrInc) and 0x7FFF
      }

      else -> throw IllegalArgumentException("Attempt to write to reg #${reg}")   // Should never happen
    }
  }

  private fun renderingEnabled() = state.isBackgroundShown || state.isSpritesShown  // TODO - rename these

  private fun State.withIncrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())

  private data class State(
    val addrInc: Int = 1,
    val nametableAddr: Address = BASE_NAMETABLES,
    val sprPatternTable: Int = 0,
    val bgPatternTable: Int = 0,
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
