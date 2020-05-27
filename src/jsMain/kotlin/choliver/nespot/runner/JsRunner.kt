package choliver.nespot.runner

import choliver.nespot.*
import choliver.nespot.nes.Joypads.Button
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.Worker
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min

class JsRunner(private val worker: Worker) {
  private val audio = BrowserAudioPlayer()
  private val screen = BrowserScreen()

  fun run() {
    worker.onmessage = messageHandler(::handleMessage)
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
    window.onresize = { configureDom() }
    configureDom()
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_ALIVE -> handleWorkerReady()
      MSG_VIDEO_FRAME -> screen.absorbFrame(Uint8ClampedArray(payload as ArrayBuffer))
      MSG_AUDIO_CHUNK -> audio.absorbChunk(Float32Array(payload as ArrayBuffer))
    }
  }

  private fun handleWorkerReady() {
    postMessage(MSG_SET_SAMPLE_RATE, audio.sampleRateHz)
    window.requestAnimationFrame(::executeFrame)
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

  // Every browser frame, we draw the latest completed emulator output, and schedule the emulator to catch up.
  // The emulator generates frames asynchronously, so we don't necessarily draw every emulator frame.
  // It also generates audio asynchronously - we schedule every audio chunk to be played.
  private fun executeFrame(timeMs: Double) {
    window.requestAnimationFrame(::executeFrame)
    screen.redraw()
    postMessage(MSG_EMULATE_UNTIL, timeMs / 1000)
  }

  private fun configureDom() {
    with(document.body!!.style) {
      margin = "0"
      padding = "0"
      backgroundColor = "black"
    }

    val scale = min(
      window.innerWidth.toDouble() / (VISIBLE_WIDTH * RATIO_STRETCH),
      window.innerHeight.toDouble() / VISIBLE_HEIGHT
    )

    val margin = (window.innerWidth - (VISIBLE_WIDTH * scale * RATIO_STRETCH)) / 2

    with(screen.canvas) {
      width = VISIBLE_WIDTH
      height = VISIBLE_HEIGHT
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


