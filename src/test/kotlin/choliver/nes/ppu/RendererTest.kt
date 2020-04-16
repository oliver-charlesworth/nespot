package choliver.nes.ppu

import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Test
import java.nio.IntBuffer

class RendererTest {
  // TODO - background rendering
  // TODO - correct base addresses
  // TODO - nametable calculation
  // TODO - attribute lookup
  // TODO - scrolling / offset
  // TODO - universal background colour
  // TODO - conditional rendering

  private val renderer = Renderer(
    memory = mock(),
    palette = mock(),
    oam = mock()
  )

  private val buffer = IntBuffer.allocate(SCREEN_WIDTH)

  @Test
  fun yeah() {
    renderer.renderScanlineTo(
      buffer,
      ctx = Renderer.Context(
        nametableAddr = 0x2000,
        bgPatternTableAddr = 0x1000,
        sprPatternTableAddr = 0x0000
      ),
      y = 3
    )

    val palette = (0..31).map { it + 5 }

    val layout = """
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
      0123456789ABCDEF0123456789ABCDEF
    """.trimIndent()
  }
}
