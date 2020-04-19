package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Memory
import choliver.nes.isBitSet
import choliver.nes.ppu.Ppu.Companion.BASE_NAMETABLES
import choliver.nes.ppu.Ppu.Companion.NAMETABLE_SIZE_BYTES
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
    val bgPatternTableAddr: Address,
    val sprPatternTableAddr: Address,
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

  // TODO - this approach does 8x too many memory loads
  // TODO - how do we combine changing both vertical *and* horizontal nametables?
  private fun prepareBackground(y: Int, ctx: Context) {
    val yOffset = y + ctx.scrollY
    val yTile = yOffset / TILE_SIZE
    val yPixel = yOffset % TILE_SIZE

    for (x in 0 until SCREEN_WIDTH) {
      val xOffset = x + ctx.scrollX
      val xNametable = ((xOffset / 8) / NUM_TILE_COLUMNS) % 2
      val xTile = (xOffset / 8) % NUM_TILE_COLUMNS
      val xPixel = (xOffset % 8)

      val addrNt = BASE_NAMETABLES +
        (xNametable * NAMETABLE_SIZE_BYTES) +
        (yTile * NUM_TILE_COLUMNS) +
        xTile

      val addrAttr = BASE_NAMETABLES +
        (xNametable * NAMETABLE_SIZE_BYTES) +
        960 +
        (yTile / 4) * (NUM_TILE_COLUMNS / 4) +
        (xTile / 4)

      val iPalette = (memory.load(addrAttr) shr (((yTile / 2) % 2) * 4 + ((xTile / 2) % 2) * 2)) and 0x03
      val pattern = getPattern(ctx.bgPatternTableAddr, memory.load(addrNt), yPixel)

      val c = patternPixel(pattern, xPixel)
      pixels[x] = Pixel(c, iPalette)
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
      if (yPixel in 0 until TILE_SIZE) {
        val pattern = getPattern(
          ctx.sprPatternTableAddr,
          iPattern,
          if (flipY) (7 - yPixel) else yPixel
        )

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
          isHit = isHit || ((iSprite == 0) && opaqueBg && opaqueSpr && (x < (SCREEN_WIDTH - 1)))
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

  private fun getPattern(base: Address, idx: Int, yPixel: Int): Int {
    val addr: Address = base + (idx * 16) + yPixel
    val p0 = memory.load(addr)
    val p1 = memory.load(addr + TILE_SIZE)
    return (p1 shl 8) or p0
  }
}
