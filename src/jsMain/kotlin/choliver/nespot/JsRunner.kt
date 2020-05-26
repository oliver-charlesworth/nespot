package choliver.nespot

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.worker.*
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.Worker
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min

class JsRunner(private val worker: Worker) {
  private val canvas = document.getElementById("target") as HTMLCanvasElement
  private var image: ImageBitmap? = null

  fun run() {
    worker.onmessage = messageHandler(::handleMessage)
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
    configureDom()
    window.onresize = { configureDom() }
    window.requestAnimationFrame(::executeFrame)
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_VIDEO_FRAME -> image = payload as ImageBitmap
    }
  }

  // Every browser frame, we draw the latest completed emulator output, and schedule the emulator to catch up.
  // The emulator generates frames asynchronously, so we don't necessarily draw every emulator frame.
  // It also generates audio asynchronously - we schedule every audio chunk to be played.
  private fun executeFrame(timeMs: Double) {
    window.requestAnimationFrame(this::executeFrame)
    redraw()
    postMessage(MSG_EMULATE_UNTIL, timeMs / 1000)
  }

  private fun redraw() {
    image?.let {
      val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
      ctx.drawImage(it, 0.0, 0.0)
      it.close()
    }
  }

  private fun handleKeyDown(e: KeyboardEvent) {
    keyToButton(e.code)?.let { postMessage(MSG_BUTTON_DOWN, it.name) }
  }

  private fun handleKeyUp(e: KeyboardEvent) {
    keyToButton(e.code)?.let { postMessage(MSG_BUTTON_UP, it.name) }
  }

  private fun postMessage(type: String, payload: Any?) {
    worker.postMessage(arrayOf(type, payload))
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

  private fun configureDom() {
    with(document.body!!.style) {
      margin = "0"
      padding = "0"
      backgroundColor = "black"
    }

    val canvasWidth = SCREEN_WIDTH
    val canvasHeight = (SCREEN_HEIGHT - 2 * TILE_SIZE)

    val scale = min(
      window.innerWidth.toDouble() / (canvasWidth.toDouble() * RATIO_STRETCH),
      window.innerHeight.toDouble() / canvasHeight.toDouble()
    )

    val margin = (window.innerWidth - (canvasWidth.toDouble() * scale * RATIO_STRETCH)) / 2

    with(canvas) {
      width = canvasWidth
      height = canvasHeight
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


