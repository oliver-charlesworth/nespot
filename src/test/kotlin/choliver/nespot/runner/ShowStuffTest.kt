package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.createMapper
import choliver.nespot.ppu.*
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.IntBuffer
import java.util.concurrent.CountDownLatch

class ShowStuffTest {
  private val latch = CountDownLatch(1)
  private val app = Screen { latch.countDown() }
  private val buffer = IntBuffer.allocate(SCREEN_HEIGHT * SCREEN_WIDTH)

  @Test
  @Disabled
  fun palette() {
    for (y in 0 until SCREEN_HEIGHT) {
      for (x in 0 until SCREEN_WIDTH) {
        buffer.put(COLORS[(x / (SCREEN_WIDTH / 16)) + (y / (SCREEN_HEIGHT / 4)) * 16])
      }
    }

    app.show()
    app.redraw(buffer)
    latch.await()
  }

  @Test
  @Disabled
  fun patterns() {
    val mapper = createMapper(Rom.parse(File("roms/smb.nes").readBytes()))
    val chr = mapper.chr(mock())

    fun getPatternData(
      patternTable: Int,    // 0 to 1
      tileRow: Int,         // 0 to 15
      tileCol: Int,         // 0 to 15
      row: Int              // 0 to 7
    ): List<Int> {
      val addr = (((patternTable * 256) + (tileRow * 16) + tileCol) * 16) + row
      val p0 = chr[addr]
      val p1 = chr[addr + 8]

      return (0..7).map { ((p0 shr (7 - it)) and 1) or (((p1 shr (7 - it)) and 1) * 2) }
    }

    val palette = listOf(
      15,  // Black
      23,  // Red
      54,  // Yellow
      24   // Shitty green
    ).map { COLORS[it] }

    val scanline = IntArray(SCREEN_WIDTH)
    for (yT in 0 until NUM_TILE_ROWS) {
      for (y in 0 until 8) {
        var i = 0
        for (xT in 0 until NUM_TILE_COLUMNS) {
          if (xT < 32 && yT < 16) {
            getPatternData(xT / 16, yT, xT % 16, y).forEach { c ->
              scanline[i++] = palette[c]
            }
          } else {
            repeat(TILE_SIZE) { scanline[i++] = 0 }
          }
        }

        scanline.forEach { buffer.put(it) }
      }
    }

    app.show()
    app.redraw(buffer)
    latch.await()
  }
}
