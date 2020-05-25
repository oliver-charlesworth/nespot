package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.ceil
import kotlin.math.min


class JsRunner(rom: Rom) {
  private val canvas = document.getElementById("target") as HTMLCanvasElement
  private val screen = BrowserScreen(canvas)
  private val audio = BrowserAudioPlayer()
  private val nes = Nes(
    sampleRateHz = audio.sampleRateHz,
    rom = rom,
    videoSink = screen.sink,
    audioSink = audio.sink
  )
  private var cycles: Long = 0
  private var originSeconds: Double? = null

  fun run() {
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
    configureDom()
    window.onresize = { configureDom() }
    window.requestAnimationFrame(::executeFrame)
  }

  private fun executeFrame(timeMs: Double) {
    window.requestAnimationFrame(this::executeFrame)
    screen.redraw()
    emulateUntil(timeMs / 1000)
  }

  private fun emulateUntil(timeSeconds: Double) {
    originSeconds = originSeconds ?: timeSeconds
    val target = ceil((timeSeconds - originSeconds!!) * CPU_FREQ_HZ.toDouble()).toInt()
    while (cycles < target) {
      cycles += nes.step()
    }
  }

  private fun handleKeyDown(e: KeyboardEvent) {
    keyToButton(e.code)?.let { nes.joypads.down(1, it) }
  }

  private fun handleKeyUp(e: KeyboardEvent) {
    keyToButton(e.code)?.let { nes.joypads.up(1, it) }
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
      window.innerWidth.toDouble() / (screen.width.toDouble() * RATIO_STRETCH),
      window.innerHeight.toDouble() / screen.height.toDouble()
    )

    val margin = (window.innerWidth - (screen.width.toDouble() * scale * RATIO_STRETCH)) / 2

    with(canvas) {
      width = screen.width
      height = screen.height
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


