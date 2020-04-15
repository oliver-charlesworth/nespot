package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.cartridge.ChrMemory.ChrLoadResult.Data
import choliver.nes.debugger.Screen
import choliver.nes.debugger.Screen.Companion.SCALE
import choliver.nes.ppu.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ShowStuffTest {
  private val app = Screen()

  @Test
  @Disabled
  fun palette() {
    for (y in 0 until SCREEN_HEIGHT * SCALE) {
      for (x in 0 until SCREEN_WIDTH * SCALE) {
        app.buffer.putInt(COLORS[(x / (SCREEN_WIDTH * SCALE / 16)) + (y / (SCREEN_HEIGHT * SCALE / 4)) * 16])
      }
    }

    app.start()
    app.redraw()
    app.await()
  }

  @Test
  @Disabled
  fun patterns() {
    val cartridge = Cartridge(javaClass.getResource("/smb.nes").readBytes())

    fun getPatternData(
      patternTable: Int,    // 0 to 1
      tileRow: Int,         // 0 to 15
      tileCol: Int,         // 0 to 15
      row: Int              // 0 to 7
    ): List<Int> {
      val addr = (((patternTable * 256) + (tileRow * 16) + tileCol) * 16) + row
      val p0 = (cartridge.chr.load(addr) as Data).data
      val p1 = (cartridge.chr.load(addr + 8) as Data).data

      return (0..7).map { ((p0 shr (7 - it)) and 1) or (((p1 shr (7 - it)) and 1) * 2) }
    }

    val palette = listOf(
      15,  // Black
      23,  // Red
      54,  // Yellow
      24   // Shitty green
    ).map { COLORS[it] }

    val scanline = IntArray(SCREEN_WIDTH * SCALE)
    for (yT in 0 until NUM_TILE_ROWS) {
      for (y in 0 until 8) {
        var i = 0
        for (xT in 0 until NUM_TILE_COLUMNS) {
          if (xT < 32 && yT < 16) {
            getPatternData(xT / 16, yT, xT % 16, y).forEach { c ->
              repeat(SCALE) { scanline[i++] = palette[c] }
            }
          } else {
            repeat(TILE_SIZE * SCALE) { scanline[i++] = 0 }
          }
        }

        repeat(SCALE) { scanline.forEach { app.buffer.putInt(it) } }
      }
    }

    app.start()
    app.redraw()
    app.await()
  }
}
