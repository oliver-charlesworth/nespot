package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import choliver.nespot.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
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
    var paletteBase: Int = 0,
    var flipX: Boolean = false,
    var behind: Boolean = false,
    var valid: Boolean = false
  )

  // Identifies location of opaque sprite pixels (bitmap *much* faster than array/list of booleans)
  private val anyOpaqueSpr = MutableList(SCREEN_WIDTH / Int.SIZE_BITS) { 0 }
  private val colorLookup = IntArray(32) { 0 }
  private var paletteBase = 0
  private var patternLo: Data = 0x00
  private var patternHi: Data = 0x00

  private var state = State()

  fun loadAndRenderBackground(ppu: PpuState) {
    if (ppu.bgEnabled) {
      ppu.loadAndRenderEnabledTiles()
    } else {
      renderDisabledTiles()
    }
  }

  private fun PpuState.loadAndRenderEnabledTiles() {
    var x = 0
    val indices = state.paletteIndices

    loadNextBackgroundTile()

    // Potentially clipped left-hand tile
    while (x < TILE_SIZE) {
      if (bgLeftTileEnabled) {
        indices[x++] = paletteBase + patternPixel(patternLo, patternHi, coords.xFine)
      } else {
        indices[x++] = 0
      }
      coords.incrementX()
      if (coords.xFine == 0) {
        loadNextBackgroundTile()
      }
    }

    // Align with memory loads
    while (coords.xFine != 0) {
      state.paletteIndices[x++] = paletteBase + patternPixel(patternLo, patternHi, coords.xFine)
      coords.incrementX()
    }
    loadNextBackgroundTile()

    // Do as many fully-aligned tiles as possible (reverse through each tile because of pattern packing order)
    repeat(NUM_TILE_COLUMNS - 2) {
      patternHi = patternHi shl 1
      indices[x + 7] = paletteBase or ((patternLo shr 0) and 1) or ((patternHi shr 0) and 2)
      indices[x + 6] = paletteBase or ((patternLo shr 1) and 1) or ((patternHi shr 1) and 2)
      indices[x + 5] = paletteBase or ((patternLo shr 2) and 1) or ((patternHi shr 2) and 2)
      indices[x + 4] = paletteBase or ((patternLo shr 3) and 1) or ((patternHi shr 3) and 2)
      indices[x + 3] = paletteBase or ((patternLo shr 4) and 1) or ((patternHi shr 4) and 2)
      indices[x + 2] = paletteBase or ((patternLo shr 5) and 1) or ((patternHi shr 5) and 2)
      indices[x + 1] = paletteBase or ((patternLo shr 6) and 1) or ((patternHi shr 6) and 2)
      indices[x + 0] = paletteBase or ((patternLo shr 7) and 1) or ((patternHi shr 7) and 2)
      coords.incrementXByTile()
      loadNextBackgroundTile()
      x += 8
    }

    // Epilogue
    while (x < SCREEN_WIDTH) {
      indices[x] = paletteBase + patternPixel(patternLo, patternHi, coords.xFine)
      coords.incrementX()
      x++
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

    paletteBase = ((memory[addrAttr] shr shift) and 0x03) * NUM_ENTRIES_PER_PALETTE
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
        val iRow = ppu.scanline - y

        spr.valid = iRow in 0 until if (ppu.largeSprites) (TILE_SIZE * 2) else TILE_SIZE
        if (spr.valid) {
          val iPattern = oam[iCandidate * 4 + 1]
          val attrs = oam[iCandidate * 4 + 2]
          val flipY = attrs.isBitSet(7)
          spr.x = oam[iCandidate * 4 + 3]
          spr.sprite0 = (iCandidate == 0)
          spr.paletteBase = ((attrs and 0x03) + 4) * NUM_ENTRIES_PER_PALETTE
          spr.flipX = attrs.isBitSet(6)
          spr.behind = attrs.isBitSet(5)
          spr.patternAddr = patternAddr(
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
    for (x in 0 until anyOpaqueSpr.size) {
      anyOpaqueSpr[x] = 0
    }

    if (ppu.sprEnabled) {
      state.sprites
        .dropLast(1)
        .forEach { spr -> renderSprite(spr, ppu) }
    }
  }

  private fun renderSprite(spr: SpriteToRender, ppu: PpuState) {
    val indices = state.paletteIndices
    val mask = if (spr.flipX) 0b111 else 0b000
    for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - spr.x)) {
      val x = spr.x + xPixel
      val c = patternPixel(spr.patternLo, spr.patternHi, xPixel xor mask)

      if (
        (c != 0) &&
        !anyOpaqueSpr[x / Int.SIZE_BITS].isBitSet(x % Int.SIZE_BITS) &&
        (ppu.sprLeftTileEnabled || (x >= TILE_SIZE))
      ) {
        anyOpaqueSpr[x / Int.SIZE_BITS] += 1 shl (x % Int.SIZE_BITS)

        // There is no higher-priority opaque sprite, so if pixel is opaque then it must be background
        val opaqueBg = (indices[x] % NUM_ENTRIES_PER_PALETTE) != 0

        if (!(spr.behind && opaqueBg)) {
          indices[x] = spr.paletteBase + c
        }

        if (spr.sprite0 && opaqueBg && (x < (SCREEN_WIDTH - 1))) {
          ppu.sprite0Hit = true
        }
      }
    }
  }

  fun commitToBuffer(ppu: PpuState, buffer: IntArray) {
    val mask = if (ppu.greyscale) 0x30 else 0x3F  // TODO - implement greyscale in Palette itself
    for (i in 0 until 32) {
      // Background colour is universal
      colorLookup[i] = colors[palette[if (i % NUM_ENTRIES_PER_PALETTE == 0) 0 else i] and mask]
    }

    var idx = ppu.scanline * SCREEN_WIDTH
    state.paletteIndices.forEach { buffer[idx++] = colorLookup[it] }
  }

  private fun maybeFlip(v: Int, flip: Boolean) = if (flip) (7 - v) else v

  private fun patternPixel(patternLo: Data, patternHi: Data, xPixel: Int) = 0 +
    (if (patternLo.isBitSet(7 - xPixel)) 1 else 0) +
    (if (patternHi.isBitSet(7 - xPixel)) 2 else 0)

  private fun loadPatternLo(addr: Address) = memory[BASE_PATTERNS + addr]
  private fun loadPatternHi(addr: Address) = memory[BASE_PATTERNS + addr + TILE_SIZE]

  private fun patternAddr(iTable: Int, iTile: Int, iRow: Int) = (iTable * 4096) + (iTile * 16) + iRow

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
