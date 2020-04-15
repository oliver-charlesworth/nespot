package choliver.nes.ppu

import choliver.nes.*
import javafx.application.Platform
import javafx.stage.Stage
import mu.KotlinLogging
import java.nio.ByteBuffer

class Ppu(
  private val memory: Memory
) {
  private val logger = KotlinLogging.logger {}
  private var state = State()
  private val palette = Ram(32)
  private val oam = Ram(256)

  // Oh god oh god
  fun render() {

    class OhGodOhGod : BaseApplication() {
      override fun populateData(data: ByteBuffer) {
        val buf = IntArray(CANVAS_WIDTH * SCALE)

        // For each row of tiles
        for (yT in 0 until CANVAS_HEIGHT / TILE_SIZE) {
          // For each scan-line
          for (y in 0 until TILE_SIZE) {
            var i = 0

            // For each column of tiles
            for (xT in 0 until CANVAS_WIDTH / TILE_SIZE) {
              val addrNametable = 0x2000 + yT * (CANVAS_WIDTH / TILE_SIZE) + xT

              getPatternData(addrNametable, y).forEach { c ->
                repeat(SCALE) { buf[i++] = palette[c] }
              }
            }

            repeat(SCALE) { buf.forEach { data.putInt(it) } }
          }
        }
      }

      private fun getPatternData(
        addrNametable: Address,
        scanline: Int     // 0 to 7
      ): List<Int> {
        val addr = memory.load(addrNametable) * 16 + 0x1000 + scanline  // TODO - remove hardcoding for pattern table 1
        val p0 = memory.load(addr)
        val p1 = memory.load(addr + 8)

        return (0..7).map { ((p0 shr (7 - it)) and 1) or (((p1 shr (7 - it)) and 1) * 2) }
      }

      private val palette = listOf(
        15,  // Black
        23,  // Red
        54,  // Yellow
        24   // Shitty green
      ).map { COLORS[it] }
    }

    val app = OhGodOhGod()
    app.init()

    Platform.startup {
      val stage = Stage()
      app.start(stage)
    }
  }

  fun readReg(reg: Int): Int {
    return when (reg) {
      REG_PPUSTATUS -> {
        // Reset stuff
        state = state.copy(
          addrWriteLo = false,
          addr = 0
        )
        0x80 // TODO - remove debug hack that emulates VBL
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
  private fun State.withIncrementedPpuAddr() = copy(addr = (state.addr + addrInc).addr()) // TODO - add test for addIncr = 32

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
