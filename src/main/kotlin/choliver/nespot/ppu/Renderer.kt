package choliver.nespot.ppu

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.isBitSet
import choliver.nespot.ppu.Ppu.Companion.BASE_PATTERNS
import java.nio.IntBuffer
import kotlin.math.min

// TODO - eliminate all the magic numbers here
// TODO - conditional rendering
// TODO - scrolling
// TODO - large sprites
// TODO - only select four sprites
// TODO - priority
// TODO - grayscale
// TODO - emphasize
class Renderer(
  private val memory: Memory,
  private val palette: Memory,
  private val oam: Memory,
  private val screen: IntBuffer,
  private val colors: List<Int> = COLORS
) {
  data class Context(
    val isLargeSprites: Boolean,
    val nametableAddr: Address,
    val bgPatternTable: Int,  // 0 or 1
    val sprPatternTable: Int, // 0 or 1
    val scrollX: Data,
    val scrollY: Data
  )

  private data class Pixel(
    val c: Int,
    val p: Int
  )

  private val pixels = Array(SCREEN_WIDTH) { Pixel(0, 0) }

  fun renderScanlineAndDetectHit(y: Int, ctx: Context): Boolean {
    prepareBackground(y, ctx)
    val isHit = prepareSpritesAndDetectHit(y, ctx)
    renderToBuffer(y)
    return isHit
  }

  private fun prepareBackground(y: Int, ctx: Context) {
    val yTile = y / TILE_SIZE
    val yPixel = y % TILE_SIZE
    for (xTile in 0 until NUM_TILE_COLUMNS) {
      val xDash = (xTile + (ctx.scrollX / 8)) % NUM_TILE_COLUMNS
      val xDashBig = (xTile + (ctx.scrollX / 8)) / NUM_TILE_COLUMNS

      val addrNt = ctx.nametableAddr + (xDashBig * 1024) + (yTile * NUM_TILE_COLUMNS + xDash)
      val addrAttr = ctx.nametableAddr + (xDashBig * 1024)  + 960 + ((yTile / 4) * (NUM_TILE_COLUMNS / 4) + (xDash / 4))
      val iPalette = (memory.load(addrAttr) shr (((yTile / 2) % 2) * 4 + ((xDash / 2) % 2) * 2)) and 0x03
      val pattern = getPattern(iTable = ctx.bgPatternTable, iTile = memory.load(addrNt), iRow = yPixel)

      for (xPixel in 0 until TILE_SIZE) {
        val c = patternPixel(pattern, xPixel)
        pixels[xTile * TILE_SIZE + xPixel] = Pixel(c, iPalette)
      }
    }
  }

  private fun prepareSpritesAndDetectHit(y: Int, ctx: Context): Boolean {
    var isHit = false

    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam.load(iSprite * 4 + 0) + 1   // Offset of one scanline
      val xSprite = oam.load(iSprite * 4 + 3)
      val iPattern = oam.load(iSprite * 4 + 1)
      val attrs = oam.load(iSprite * 4 + 2)
      val iPalette = (attrs and 0x03) + 4
      val isBehind = attrs.isBitSet(5)
      val flipX = attrs.isBitSet(6)
      val flipY = attrs.isBitSet(7)
      val yPixel = y - ySprite

      val pattern  = if (ctx.isLargeSprites) {
        when (yPixel) {
          in (0 until TILE_SIZE) -> {
            getPattern(
              iTable = iPattern and 0x01,
              iTile = (iPattern and 0xFE) + (if (flipY) 1 else 0),
              iRow = yPixel,
              flip = flipY
            )
          }
          in (TILE_SIZE until TILE_SIZE * 2) -> {
            getPattern(
              iTable = iPattern and 0x01,
              iTile = (iPattern and 0xFE) + (if (flipY) 0 else 1),
              iRow = yPixel - TILE_SIZE,
              flip = flipY
            )
          }
          else -> null
        }
      } else {
        if (yPixel in 0 until TILE_SIZE) {
          getPattern(
            iTable = ctx.sprPatternTable,
            iTile = iPattern,
            iRow = yPixel,
            flip = flipY
          )
        } else null
      }

      if (pattern != null) {
        for (xPixel in 0 until min(TILE_SIZE, SCREEN_WIDTH - xSprite)) {
          val c = patternPixel(
            pattern,
            if (flipX) (7 - xPixel) else xPixel
          )

          val x = xSprite + xPixel
          val spr = Pixel(c, iPalette)
          val bg = pixels[x]
          val opaqueSpr = (c != 0)
          val opaqueBg = (bg.c != 0)

          pixels[x] = if (opaqueSpr && (!isBehind || !opaqueBg)) spr else bg

          // Collision detection
          isHit = isHit || ((iSprite == 0) && opaqueBg && opaqueSpr &&
            (x < (SCREEN_WIDTH - 1)))   // Weird, but apparently it's a thing (note test-case)
        }
      }
    }

    return isHit
  }

  private fun renderToBuffer(y: Int) {
    screen.position(y * SCREEN_WIDTH)
    pixels.forEach {
      val paletteAddr = if (it.c == 0) 0 else (it.p * 4 + it.c) // Background colour is universal
      screen.put(colors[palette.load(paletteAddr)])
    }
  }

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun getPattern(iTable: Int, iTile: Int, iRow: Int, flip: Boolean = false): Int {
    val addr = (iTable * 4096) + (iTile * 16) + (if (flip) (7 - iRow) else iRow)
    val p0 = memory.load(BASE_PATTERNS + addr)
    val p1 = memory.load(BASE_PATTERNS + addr + TILE_SIZE)
    return (p1 shl 8) or p0
  }

}
