package choliver.nes.ppu

import choliver.nes.cartridge.Cartridge
import choliver.nes.cartridge.ChrMemory.ChrLoadResult.Data
import choliver.nes.u16
import javafx.application.Application.launch
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ShowStuffTest {
  @Test
  @Disabled
  fun palette() {
    class PaletteApp : BaseApplication() {
      override fun populateData(data: ByteBuffer) {
        for (y in 0 until CANVAS_HEIGHT * SCALE) {
          for (x in 0 until CANVAS_WIDTH * SCALE) {
            data.putInt(COLORS[(x / (CANVAS_WIDTH  * SCALE / 16)) + (y / (CANVAS_HEIGHT  * SCALE / 4)) * 16])
          }
        }
      }
    }

    launch(PaletteApp::class.java)
  }

  @Test
  @Disabled
  fun patterns() {
    class PatternsApp : BaseApplication() {
      val cartridge = Cartridge(javaClass.getResource("/smb.nes").readBytes())

      private fun getPatternData(
        patternTable: Int,    // 0 to 1
        tileRow: Int,         // 0 to 15
        tileCol: Int,         // 0 to 15
        row: Int              // 0 to 7
      ): List<Int> {
        val addr = (((patternTable * 256) + (tileRow * 16) + tileCol) * 16) + row
        val p0 = (cartridge.chr.load(addr.u16()) as Data).data.toInt()
        val p1 = (cartridge.chr.load((addr + 8).u16()) as Data).data.toInt()

        return (0..7).map { ((p0 shr (7 - it)) and 1) or (((p1 shr (7 - it)) and 1) * 2) }
      }

      private val palette = listOf(
        15,  // Black
        23,  // Red
        54,  // Yellow
        24   // Shitty green
      ).map { COLORS[it] }

      override fun populateData(data: ByteBuffer) {
        val buf = IntArray(CANVAS_WIDTH * SCALE)
        for (yT in 0 until CANVAS_HEIGHT / TILE_SIZE) {
          for (y in 0 until 8) {
            var i = 0
            for (xT in 0 until CANVAS_WIDTH / TILE_SIZE) {
              if (xT < 32 && yT < 16) {
                getPatternData(xT / 16, yT, xT % 16, y).forEach { c ->
                  repeat(SCALE) { buf[i++] = palette[c] }
                }
              } else {
                repeat(TILE_SIZE * SCALE) { buf[i++] = 0 }
              }
            }

            repeat(SCALE) { buf.forEach { data.putInt(it) } }
          }
        }
      }
    }

    launch(PatternsApp::class.java)
  }
}
