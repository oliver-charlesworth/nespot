package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads.Button
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
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

  fun run() {
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
    configureDom()
    window.onresize = { configureDom() }
    window.requestAnimationFrame(::executeFrame)
  }

  private val wotsit = JsWotsit(rom)

  private fun executeFrame(timeMs: Double) {
    window.requestAnimationFrame(this::executeFrame)
    updateImage(wotsit.latestVideoFrame)
    wotsit.runUntil(timeMs / 1000)
  }

  private fun updateImage(frame: IntArray) {
    var i = SCREEN_WIDTH * TILE_SIZE
    var j = 0
    for (y in TILE_SIZE until SCREEN_HEIGHT - TILE_SIZE) {
      for (x in 0 until SCREEN_WIDTH) {
        val pixel = frame[i++]
        data[j++] = ((pixel shr 8) and 0xFF).toShort()
        data[j++] = ((pixel shr 16) and 0xFF).toShort()
        data[j++] = ((pixel shr 24) and 0xFF).toShort()
        data[j++] = 255
      }
    }

    ctx.putImageData(img, 0.0, 0.0)
  }

  private fun handleKeyDown(e: KeyboardEvent) {
    keyToButton(e.code)?.let { wotsit.joypads.down(1, it) }
  }

  private fun handleKeyUp(e: KeyboardEvent) {
    keyToButton(e.code)?.let { wotsit.joypads.up(1, it) }
  }

  private fun keyToButton(code: String) = when (code) {
    "KeyZ" -> Button.A
    "KeyX" -> Button.B
    "BracketLeft" -> Button.SELECT
    "BracketRight" -> Button.START
    "ArrowLeft" -> Button.LEFT
    "ArrowRight" -> Button.RIGHT
    "ArrowUp" -> Button.UP
    "ArrowDown" -> Button.DOWN
    else -> null
  }

  // Main loop works thus:
  //  - Run emulation in chunks of BUFFER_LENGTH_MS.
  //  - Buffer up 3 chunks, and schedule.
  //  - Run another chunk whenever a scheduled chunk ends.
  //  - requestAnimationFrame just grabs latest video frame buffer.

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

  companion object {
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}


