package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
import java.nio.IntBuffer
import kotlin.math.min
import choliver.nespot.ppu.model.State as PpuState


// TODO - eliminate all the magic numbers here
// TODO - colour emphasis
class Renderer(
  private val memory: Memory,
  private val palette: Memory,
  private val oam: Memory,
  private val videoBuffer: IntBuffer,
  private val colors: List<Int> = COLORS
) {
  data class State(
    val paletteIndices: MutableList<Int> = MutableList(SCREEN_WIDTH) { 0 },
    // One extra to detect overflow
    val sprites: List<SpriteToRender> = List(MAX_SPRITES_PER_SCANLINE + 1) { SpriteToRender() }.toList()
  )

  @MutableForPerfReasons
  data class SpriteToRender(
    var x: Int = 0,
    var sprite0: Boolean = false,
    var patternAddr: Address = 0x0000,
    var pattern: Int = 0,
    var palette: Int = 0,
    var flipX: Boolean = false,
    var behind: Boolean = false,
    var valid: Boolean = false
  )

  // Don't persist beyond internal call, so need to be in State
  private val opaqueSpr = MutableList(SCREEN_WIDTH) { false }  // Identifies opaque sprite pixels
  private var iPalette = 0
  private var pattern: Data = 0x00

  private var state = State()

  fun loadAndRenderBackground(ppu: PpuState) {
    with(ppu.coords) {
      for (x in 0 until SCREEN_WIDTH) {
        opaqueSpr[x] = false
        if (ppu.bgEnabled) {
          if ((x == 0) || (xFine == 0)) {
            loadNextBackgroundTile(ppu)
          }
          if (ppu.bgLeftTileEnabled || (x >= TILE_SIZE)) {
            state.paletteIndices[x] = paletteIndex(patternPixel(pattern, xFine), iPalette)
          } else {
            state.paletteIndices[x] = 0
          }
          incrementX()
        } else {
          state.paletteIndices[x] = 0
        }
      }
    }
  }

  private fun loadNextBackgroundTile(state: PpuState) {
    with(state.coords) {
      val addrNt = BASE_NAMETABLES +
        ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
        (yCoarse * NUM_TILE_COLUMNS) +
        xCoarse

      val addrAttr = BASE_NAMETABLES +
        ((yNametable * 2 + xNametable) * NAMETABLE_SIZE_BYTES) +
        960 +
        (yCoarse / 4) * (NUM_TILE_COLUMNS / 4) +
        (xCoarse / 4)

      iPalette = (memory[addrAttr] shr (((yCoarse / 2) % 2) * 4 + ((xCoarse / 2) % 2) * 2)) and 0x03
      pattern = loadPattern(patternAddr(iTable = state.bgPatternTable, iTile = memory[addrNt], iRow = yFine))
    }
  }

  fun evaluateSprites(ppu: PpuState) {
    var iCandidate = 0

    state.sprites.forEach { spr ->
      spr.valid = false

      // Scan until we find a matching sprite
      while (!spr.valid && (iCandidate < NUM_SPRITES)) {
        val y = oam[iCandidate * 4 + 0]
        val iPattern = oam[iCandidate * 4 + 1]
        val attrs = oam[iCandidate * 4 + 2]
        val x = oam[iCandidate * 4 + 3]

        val iRow = ppu.scanline - y
        val flipY = attrs.isBitSet(7)

        with(spr) {
          this.x = x
          sprite0 = (iCandidate == 0)
          palette = (attrs and 0x03) + 4
          flipX = attrs.isBitSet(6)
          behind = attrs.isBitSet(5)
          patternAddr = patternAddr(
            iTable = when (ppu.largeSprites) {
              true -> iPattern and 0x01
              false -> ppu.sprPatternTable
            },
            iTile = when (ppu.largeSprites) {
              true -> (iPattern and 0xFE) + (if (flipY xor (iRow < TILE_SIZE)) 0 else 1)
              false -> iPattern
            },
            iRow = maybeFlip(iRow % TILE_SIZE, flipY)
          )
          valid = iRow in 0 until if (ppu.largeSprites) (TILE_SIZE * 2) else TILE_SIZE
        }

        iCandidate++
      }
    }

    ppu.spriteOverflow = (ppu.bgEnabled || ppu.sprEnabled) && state.sprites.last().valid
  }

  fun loadSprites(ppu: PpuState) {
    if (ppu.sprEnabled) {
      state.sprites
        .dropLast(1)
        .forEach { spr ->
          spr.pattern = if (spr.valid) {
            loadPattern(spr.patternAddr)
          } else {
            loadPattern(DUMMY_SPRITE_PATTERN_ADDR)
            0   // All-transparent
          }
        }
    }
  }

  // Lowest index is highest priority, so render last
  fun renderSprites(ppu: PpuState) {
    if (ppu.sprEnabled) {
      state.sprites
        .dropLast(1)
        .forEach { spr -> renderSprite(spr, ppu) }
    }
  }

  private fun renderSprite(spr: SpriteToRender, ppu: PpuState) {
    for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
      val x = spr.x + xPixel
      val c = patternPixel(spr.pattern, maybeFlip(xPixel, spr.flipX))

      if ((c != 0) && !opaqueSpr[x] && (ppu.sprLeftTileEnabled || (x >= TILE_SIZE))) {
        opaqueSpr[x] = true

        // There is no previous opaque sprite, so if pixel is opaque then it must be background
        val opaqueBg = (state.paletteIndices[x] != 0)

        if (!(spr.behind && opaqueBg)) {
          state.paletteIndices[x] = paletteIndex(c, spr.palette)
        }

        if (spr.sprite0 && opaqueBg && (x < (SCREEN_WIDTH - 1))) {
          ppu.sprite0Hit = true
        }
      }
    }
  }

  fun commitToBuffer(ppu: PpuState) {
    videoBuffer.position(ppu.scanline * SCREEN_WIDTH)
    val mask = if (ppu.greyscale) 0x30 else 0x3F  // TODO - implement greyscale in Palette itself
    state.paletteIndices.forEach {
      videoBuffer.put(colors[palette[it] and mask])
    }
  }

  private fun maybeFlip(v: Int, flip: Boolean) = if (flip) (7 - v) else v

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun loadPattern(addr: Int): Int {
    val p0 = memory[BASE_PATTERNS + addr]
    val p1 = memory[BASE_PATTERNS + addr + TILE_SIZE]
    return (p1 shl 8) or p0
  }

  private fun patternAddr(iTable: Int, iTile: Int, iRow: Int) = (iTable * 4096) + (iTile * 16) + iRow

  // Background colour is universal
  private fun paletteIndex(entry: Int, palette: Int) = if (entry == 0) 0 else (palette * 4 + entry)

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Renderer.state
      set(value) { this@Renderer.state = value }
  }

  val diagnostics = Diagnostics()

  companion object {
    const val MAX_SPRITES_PER_SCANLINE = 8
    private const val DUMMY_SPRITE_PATTERN_ADDR = 0x1FF0
  }
}
