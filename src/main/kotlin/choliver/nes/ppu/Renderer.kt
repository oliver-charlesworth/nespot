package choliver.nes.ppu

import choliver.nes.Address
import choliver.nes.Memory
import choliver.nes.debugger.Screen.Companion.SCALE
import java.nio.ByteBuffer

class Renderer(
  private val memory: Memory,
  private val palette: Palette
) {
  private val scanline = IntArray(CANVAS_WIDTH * SCALE)

  fun renderTo(
    buffer: ByteBuffer,
    nametableAddr: Address,
    bgPatternTableAddr: Address
  ) {
    for (yT in 0 until NUM_TILE_ROWS) {
      // For each scan-line
      for (y in 0 until TILE_SIZE) {
        var i = 0

        for (xT in 0 until NUM_TILE_COLUMNS) {
          val addrNt = nametableAddr + (yT * NUM_TILE_COLUMNS + xT)
          val addrAttr = nametableAddr + 960 + ((yT / 4) * (NUM_TILE_COLUMNS / 4) + (xT / 4))

          val addr = bgPatternTableAddr + memory.load(addrNt) * 16 + y

          val attr = (memory.load(addrAttr) shr (((yT / 2) % 2) * 4 + ((xT / 2) % 2) * 2)) and 0x03

          val p0 = memory.load(addr)
          val p1 = memory.load(addr + 8)
          val pattern = (0 until TILE_SIZE).map {
            ((p0 shr (7 - it)) and 1) or (((p1 shr (7 - it)) and 1) * 2)
          }

          pattern.forEach { c ->
            val paletteAddr = if (c == 0) 0 else (attr * 4 + c) // Background colour is universal
            repeat(SCALE) { scanline[i++] = COLORS[palette.load(paletteAddr)] }
          }
        }

        repeat(SCALE) { scanline.forEach { buffer.putInt(it) } }
      }
    }
  }
}
