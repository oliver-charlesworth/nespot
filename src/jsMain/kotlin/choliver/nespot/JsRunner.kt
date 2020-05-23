package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.ppu.TILE_SIZE
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min


class JsRunner(rom: Rom) {
  private val canvas = document.getElementById("target") as HTMLCanvasElement
  private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
  private val img = ctx.createImageData(
    SCREEN_WIDTH.toDouble(),
    (SCREEN_HEIGHT - 2 * TILE_SIZE).toDouble()
  )
  private val data = img.data.unsafeCast<Uint16Array>() // See https://stackoverflow.com/a/49336551

  private var prev: Double? = null
  private val list = mutableListOf<IntArray>()
  private val joypads = Joypads()
  private val nes = Nes(
    rom = rom,
    joypads = joypads,
    onVideoBufferReady = { list += it }
  )

  fun start() {
    configureDom()
    window.onresize = { configureDom() }
    window.requestAnimationFrame(this::executeFrame)
  }

  private fun configureDom() {
    with(document.body!!.style) {
      margin = "0"
      padding = "0"
      backgroundColor = "black"
    }

    val scale = min(
      window.innerWidth.toDouble() / (img.width.toDouble() * RATIO_STRETCH),
      window.innerHeight.toDouble() / img.height.toDouble()
    )

    val margin = (window.innerWidth - (img.width.toDouble() * scale * RATIO_STRETCH)) / 2

    with(canvas) {
      width = img.width
      height = img.height
      with(style) {
        display = "block"
        marginLeft = "${margin}px"
        marginRight = "${margin}px"
        padding = "0"
        transformOrigin = "0 0"
        transform = "scale(${scale * RATIO_STRETCH}, ${scale})"
      }
    }
  }

  private fun executeFrame(timestamp: Double) {
    val delta = timestamp - (prev ?: timestamp)
    prev = timestamp

//    val cycles = ((delta / 1000) * CPU_FREQ_HZ.toDouble()).

    while (list.isEmpty()) {
      nes.step()
    }
    updateImage(list.removeAt(0))
    window.requestAnimationFrame(this::executeFrame)
  }

  private fun updateImage(buffer: IntArray) {
    var i = SCREEN_WIDTH * TILE_SIZE
    var j = 0
    for (y in TILE_SIZE until SCREEN_HEIGHT - TILE_SIZE) {
      for (x in 0 until SCREEN_WIDTH) {
        val pixel = buffer[i++]
        data[j++] = ((pixel shr 8) and 0xFF).toShort()
        data[j++] = ((pixel shr 16) and 0xFF).toShort()
        data[j++] = ((pixel shr 24) and 0xFF).toShort()
        data[j++] = 255
      }
    }

    ctx.putImageData(img, 0.0, 0.0)
  }

  companion object {
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}


