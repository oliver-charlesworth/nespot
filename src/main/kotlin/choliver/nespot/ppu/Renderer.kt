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
    var patternLo: Data = 0x00,
    var patternHi: Data = 0x00,
    var palette: Int = 0,
    var flipX: Boolean = false,
    var behind: Boolean = false,
    var valid: Boolean = false
  )

  // Don't persist beyond internal call, so need to be in State
  private val opaqueSpr = MutableList(SCREEN_WIDTH) { false }  // Identifies opaque sprite pixels
  private var iPalette = 0
  private var patternLo: Data = 0x00
  private var patternHi: Data = 0x00

  private var state = State()

  fun loadAndRenderBackground(ppu: PpuState) {
    if (ppu.bgEnabled) {
      ppu.loadNextBackgroundTile()
      ppu.loadAndRenderLeftTile()
      ppu.loadAndRenderOtherTiles()
    } else {
      renderDisabledTiles()
    }
  }

  private fun PpuState.loadAndRenderLeftTile() {
    for (x in 0 until TILE_SIZE) {
      state.paletteIndices[x] = if (bgLeftTileEnabled) {
        paletteIndex(patternPixel(patternLo, patternHi, coords.xFine), iPalette)
      } else {
        0
      }
      coords.incrementX()
      if (coords.xFine == 0) {
        loadNextBackgroundTile()
      }

    }
  }

  private fun PpuState.loadAndRenderOtherTiles() {
    for (x in TILE_SIZE until SCREEN_WIDTH) {
      state.paletteIndices[x] = paletteIndex(patternPixel(patternLo, patternHi, coords.xFine), iPalette)
      coords.incrementX()
      if (coords.xFine == 0) {
        loadNextBackgroundTile()
      }
    }
  }

  private fun renderDisabledTiles() {
    for (x in 0 until SCREEN_WIDTH) {
      state.paletteIndices[x] = 0
    }
  }

  private fun PpuState.loadNextBackgroundTile() {
    val addrNt = BASE_NAMETABLES +
      (coords.nametable * NAMETABLE_SIZE_BYTES) +
      (coords.yCoarse * NUM_TILE_COLUMNS) +
      coords.xCoarse

    val addrAttr = BASE_NAMETABLES +
      (coords.nametable * NAMETABLE_SIZE_BYTES) +
      960 +
      (coords.yCoarse / 4) * (NUM_TILE_COLUMNS / 4) +
      (coords.xCoarse / 4)

    val patternAddr = patternAddr(iTable = bgPatternTable, iTile = memory[addrNt], iRow = coords.yFine)

    val shift = 0 +
      (if (coords.yCoarse.isBitSet(1)) 4 else 0) +
      (if (coords.xCoarse.isBitSet(1)) 2 else 0)

    iPalette = (memory[addrAttr] shr shift) and 0x03
    patternLo = loadPatternLo(patternAddr)
    patternHi = loadPatternHi(patternAddr)
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
          if (spr.valid) {
            spr.patternLo = loadPatternLo(spr.patternAddr)
            spr.patternHi = loadPatternHi(spr.patternAddr)
          } else {
            loadPatternLo(DUMMY_SPRITE_PATTERN_ADDR)
            loadPatternHi(DUMMY_SPRITE_PATTERN_ADDR)
            spr.patternLo = 0   // All-transparent
            spr.patternHi = 0   // All-transparent
          }
        }
    }
  }

  // Lowest index is highest priority, so render last
  fun renderSprites(ppu: PpuState) {
    for (x in 0 until SCREEN_WIDTH) {
      opaqueSpr[x] = false
    }

    if (ppu.sprEnabled) {
      state.sprites
        .dropLast(1)
        .forEach { spr -> renderSprite(spr, ppu) }
    }
  }

  private fun renderSprite(spr: SpriteToRender, ppu: PpuState) {
    val mask = if (spr.flipX) 0b111 else 0b000
    for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
      val x = spr.x + xPixel
      val c = patternPixel(spr.patternLo, spr.patternHi, xPixel xor mask)

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

  fun commitToBuffer(ppu: PpuState, buffer: IntBuffer) {
    val mask = if (ppu.greyscale) 0x30 else 0x3F  // TODO - implement greyscale in Palette itself
    val lookup = IntArray(32) { colors[palette[it] and mask] }  // Optimisation

    buffer.position(ppu.scanline * SCREEN_WIDTH)
    state.paletteIndices.forEach { buffer.put(lookup[it]) }
  }

  private fun maybeFlip(v: Int, flip: Boolean) = if (flip) (7 - v) else v

  private fun patternPixel(patternLo: Data, patternHi: Data, xPixel: Int) = 0 +
    (if (patternLo.isBitSet(7 - xPixel)) 1 else 0) +
    (if (patternHi.isBitSet(7 - xPixel)) 2 else 0)

  private fun loadPatternLo(addr: Address) = memory[BASE_PATTERNS + addr]
  private fun loadPatternHi(addr: Address) = memory[BASE_PATTERNS + addr + TILE_SIZE]

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
