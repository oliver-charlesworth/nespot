package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.min


class JsRunner(rom: Rom) {
  private val audioCtx = AudioContext()
  private val canvas = document.getElementById("target") as HTMLCanvasElement
  private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
  private val img = ctx.createImageData(
    SCREEN_WIDTH.toDouble(),
    (SCREEN_HEIGHT - 2 * TILE_SIZE).toDouble()
  )
  private val data = img.data.unsafeCast<Uint16Array>() // See https://stackoverflow.com/a/49336551

  private var base: Double = 0.0
  private var first: Double? = null
  private var numFrames = 0
  private val list = mutableListOf<IntArray>()
  private val joypads = Joypads()
  private val videoSink = JsVideoSink(onBufferReady = { list += it })
  private val audioSink = XXX
  private val nes = Nes(
    sampleRateHz = audioCtx.sampleRate.toInt(),
    rom = rom,
    joypads = joypads,
    videoSink = videoSink,
    audioSink = audioSink
  )

  fun run() {
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
    configureDom()
    base = audioCtx.currentTime
    window.onresize = { configureDom() }
    window.requestAnimationFrame(::executeFrame)
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

  val frameRateMs = 1000.0 / (CPU_FREQ_HZ / (DOTS_PER_SCANLINE * SCANLINES_PER_FRAME) * DOTS_PER_CYCLE).toDouble()

  private fun executeFrame(timestamp: Double) {
    first = first ?: timestamp
    val delta = (timestamp - first!!) - (frameRateMs * numFrames)
    numFrames++

//    println("delta = ${delta.toInt()}")


    // TODO:
    // - calculate runtime
    // - convert to cycles
    // - run for that many cycles, collect latest buffer


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

  private fun handleKeyDown(e: KeyboardEvent) {
    keyToButton(e.code)?.let { joypads.down(1, it) }
  }

  private fun handleKeyUp(e: KeyboardEvent) {
    keyToButton(e.code)?.let { joypads.up(1, it) }
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

  // TODO: Needs to be driven by audio needs
  //  - Thus use audio currentTime - initialTime to determine number of cycles that need running
  //  - i.e. aim for setpoint of 2 frames


  private fun handleAudioBufferReady(buffer: FloatArray) {
    val target = audioCtx.createBuffer(1, buffer.size, audioCtx.sampleRate)
    val samples = target.getChannelData(0)
    buffer.forEachIndexed { idx, sample -> samples[idx] = 0.0f /*sample*/ }

    val source = audioCtx.createBufferSource()
    source.buffer = target
    source.connect(audioCtx.destination)
    source.start(base)

//    println("Lag: ${((base - audioCtx.currentTime) * 1000).toInt()}")

    base += buffer.size / audioCtx.sampleRate
  }

  companion object {
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}


