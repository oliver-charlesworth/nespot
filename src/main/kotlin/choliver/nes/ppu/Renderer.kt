package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Memory
import choliver.nes.isBitSet
import java.nio.IntBuffer

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
  private val colors: List<Int> = COLORS
) {
  data class Context(
    val nametableAddr: Address,
    val bgPatternTableAddr: Address,
    val sprPatternTableAddr: Address
  )

  private val scanline = IntArray(SCREEN_WIDTH)

  // TODO - move to orchestration layer
  fun renderTo(
    buffer: IntBuffer,
    ctx: Context
  ) {
    for (y in 0 until SCREEN_HEIGHT) {
      renderScanlineTo(buffer, ctx, y)
    }
  }

  fun renderScanlineTo(
    buffer: IntBuffer,
    ctx: Context,
    y: Int
  ) {
    renderBackground(ctx, y)
    renderSprites(ctx, y)
    scanline.forEach { buffer.put(it) }
  }

  private fun renderBackground(ctx: Context, y: Int) {
    val yTile = y / TILE_SIZE
    val yPixel = y % TILE_SIZE
    for (xTile in 0 until NUM_TILE_COLUMNS) {
      val addrNt = ctx.nametableAddr + (yTile * NUM_TILE_COLUMNS + xTile)
      val addrAttr = ctx.nametableAddr + 960 + ((yTile / 4) * (NUM_TILE_COLUMNS / 4) + (xTile / 4))
      val iPalette = (memory.load(addrAttr) shr (((yTile / 2) % 2) * 4 + ((xTile / 2) % 2) * 2)) and 0x03
      val pattern = getPattern(ctx.bgPatternTableAddr, memory.load(addrNt), yPixel)

      for (xPixel in 0 until TILE_SIZE) {
        val c = patternPixel(pattern, xPixel)
        val paletteAddr = if (c == 0) 0 else (iPalette * 4 + c) // Background colour is universal
        scanline[xTile * TILE_SIZE + xPixel] = colors[palette.load(paletteAddr)]
      }
    }
  }

  private fun renderSprites(ctx: Context, y: Int) {
    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam.load(iSprite * 4 + 0) + 1   // Offset of one scanline
      val xSprite = oam.load(iSprite * 4 + 3)
      val iPattern = oam.load(iSprite * 4 + 1)
      val attrs = oam.load(iSprite * 4 + 2)
      val iPalette = (attrs and 0x03) + 4
      val flipX = attrs.isBitSet(6)
      val flipY = attrs.isBitSet(7)

      val yPixel = y - ySprite
      if (yPixel in 0 until TILE_SIZE) {
        val pattern = getPattern(
          ctx.sprPatternTableAddr,
          iPattern,
          if (flipY) (7 - yPixel) else yPixel
        )

        for (xPixel in 0 until TILE_SIZE) {
          val c = patternPixel(
            pattern,
            if (flipX) (7 - xPixel) else xPixel
          )

          // Handle transparency
          if (c != 0) {
            val paletteAddr = (iPalette * 4 + c)
            scanline[xSprite + xPixel] = colors[palette.load(paletteAddr)]
          }
        }
      }
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
