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
  private var addr: Address = 0
  private var scrollX: Data = 0
  private var scrollY: Data = 0

  private val latchScroll = Latch16(
    { scrollX = it },
    { scrollY = it }
  )
  private val latchAddr = Latch16(
    { addr = addr(lo = addr.lo(), hi = it) },
    { addr = addr(lo = it, hi = addr.hi()) }
  )

  // TODO - add a reset (to clean up counters and stuff)

  val scanline get() = _scanline

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  fun executeScanline() {
    when (_scanline) {
      in (0 until SCREEN_HEIGHT) -> {
        val isHit = renderer.renderScanlineAndDetectHit(
          y = _scanline,
          ctx = Renderer.Context(
            isLargeSprites = state.isLargeSprites,
            nametableAddr = state.nametableAddr,
            bgPatternTable = state.bgPatternTable,
            sprPatternTable = state.sprPatternTable,
            scrollX = scrollX,
            scrollY = scrollY
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

    _scanline = (_scanline + 1) % NUM_SCANLINES
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        val ret = 0 +
          (if (inVbl) 0x80 else 0x00) +
          (if (isSprite0Hit) 0x40 else 0x00)

        latchScroll.reset()
        latchAddr.reset()
        inVbl = false

        ret
      }

      REG_OAMDATA -> {
        val ret = oam.load(state.oamAddr)
        state = state.withincrementedOamAddr()
        ret
      }

      REG_PPUDATA -> {
        val ret = when {
          addr < BASE_PALETTE -> {
            val ret = state.ppuReadBuffered
            state = state.copy(ppuReadBuffered = memory.load(addr))
            ret
          }
          else -> palette.load(addr and 0x1F)
        }
        addr = (addr + state.addrInc).addr()
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
        sprPatternTable = if (data.isBitSet(3)) 1 else 0,
        bgPatternTable = if (data.isBitSet(4)) 1 else 0,
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

      REG_PPUSCROLL -> latchScroll.write(data)

      REG_PPUADDR -> latchAddr.write(data)

      REG_PPUDATA -> {
        when {
          addr < BASE_PALETTE -> memory.store(addr, data)
          else -> palette.store(addr and 0x1F, data)
        }
        addr = (addr + state.addrInc).addr()
      }

      else -> throw IllegalArgumentException("Attempt to write to reg #${reg}")   // Should never happen
    }
  }

  private fun State.withincrementedOamAddr() = copy(oamAddr = (state.oamAddr + 1).addr8())

  private class Latch16(
    private val updateFirst: (Data) -> Unit,
    private val updateSecond: (Data) -> Unit
  ) {
    private var state = 0

    fun reset() {
      state = 0
    }

    fun write(data: Data) {
      when (state) {
        0 -> {
          updateFirst(data)
          state = 1
        }
        1 -> {
          updateSecond(data)
          state = 0
        }
      }
    }
  }

  private data class State(
    val addrInc: Int = 1,
    val nametableAddr: Address = 0x2000,
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

    const val BASE_PATTERNS: Address = 0x0000
    const val BASE_PALETTE: Address = 0x3F00

    const val NUM_SCANLINES = 262
  }
}
