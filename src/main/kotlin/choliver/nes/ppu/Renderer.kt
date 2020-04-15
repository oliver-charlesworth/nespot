package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Memory
import choliver.nes.isBitSet
import java.nio.ByteBuffer

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
  private val oam: Memory
) {
  private val scanline = IntArray(SCREEN_WIDTH)

  fun renderTo(
    buffer: ByteBuffer,
    nametableAddr: Address,
    bgPatternTableAddr: Address,
    sprPatternTableAddr: Address
  ) {
    for (y in 0 until SCREEN_HEIGHT) {
      renderBackground(y, nametableAddr, bgPatternTableAddr)
      renderSprites(y, sprPatternTableAddr)
      scanline.forEach { buffer.putInt(it) }
    }
  }

  private fun renderBackground(y: Int, nametableAddr: Address, bgPatternTableAddr: Address) {
    val yTile = y / TILE_SIZE
    val yPixel = y % TILE_SIZE
    for (xTile in 0 until NUM_TILE_COLUMNS) {
      val addrNt = nametableAddr + (yTile * NUM_TILE_COLUMNS + xTile)
      val addrAttr = nametableAddr + 960 + ((yTile / 4) * (NUM_TILE_COLUMNS / 4) + (xTile / 4))
      val iPalette = (memory.load(addrAttr) shr (((yTile / 2) % 2) * 4 + ((xTile / 2) % 2) * 2)) and 0x03
      val pattern = getPattern(bgPatternTableAddr, memory.load(addrNt), yPixel)

      for (xPixel in 0 until TILE_SIZE) {
        val c = patternPixel(pattern, xPixel)
        val paletteAddr = if (c == 0) 0 else (iPalette * 4 + c) // Background colour is universal
        scanline[xTile * TILE_SIZE + xPixel] = COLORS[palette.load(paletteAddr)]
      }
    }
  }

  private fun renderSprites(y: Int, sprPatternTableAddr: Address) {
    for (iSprite in 0 until NUM_SPRITES) {
      val ySprite = oam.load(iSprite * 4 + 0) + 1   // Offset of one scanline
      val xSprite = oam.load(iSprite * 4 + 3)
      val iTile = oam.load(iSprite * 4 + 1)
      val attrs = oam.load(iSprite * 4 + 2)
      val iPalette = (attrs and 0x03) + 4
      val flipX = attrs.isBitSet(6)
      val flipY = attrs.isBitSet(7)

      val yPixel = y - ySprite
      if (yPixel in 0 until TILE_SIZE) {
        val pattern = getPattern(
          sprPatternTableAddr,
          iTile,
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
            scanline[xSprite + xPixel] = COLORS[palette.load(paletteAddr)]
          }
        }
      }
    }
  }

  private fun patternPixel(pattern: Int, xPixel: Int) =
    ((pattern shr (7 - xPixel)) and 1) or (((pattern shr (14 - xPixel)) and 2))

  private fun getPattern(base: Address, idx: Int, yPixel: Int): Int {
    val addr: Address = base + idx * 16 + yPixel
    val p0 = memory.load(addr)
    val p1 = memory.load(addr + TILE_SIZE)
    return (p1 shl 8) or p0
  }
}
