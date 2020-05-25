package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import kotlin.math.ceil

class JsWotsit(
  rom: Rom,
  joypads: Joypads
) {
  private val audio = JsAudioPlayer()

  private var videoBuffers = Array(NUM_VIDEO_BUFFERS) { IntArray(SCREEN_WIDTH * SCREEN_HEIGHT) }
  private var iBuffer = 0
  private var iPixel = 0

  private val nes = Nes(
    sampleRateHz = audio.sampleRateHz,
    rom = rom,
    joypads = joypads,
    videoSink = object : VideoSink {
      private var buffer = videoBuffers[iBuffer]
      override fun put(color: Int) {
        buffer[iPixel++] = color
        if (iPixel == buffer.size) {
          iPixel = 0
          iBuffer = (iBuffer + 1) % NUM_VIDEO_BUFFERS
          buffer = videoBuffers[iBuffer]
        }
      }
    },
    audioSink = audio.sink
  )

  val latestVideoFrame: IntArray
    get() = videoBuffers[(iBuffer + NUM_VIDEO_BUFFERS - 1) % NUM_VIDEO_BUFFERS]

  private var cycles: Long = 0

  private var originSeconds: Double? = null

  // This is in requestAnimationFrame timebase
  fun runUntil(timeSeconds: Double) {
    originSeconds = originSeconds ?: timeSeconds
    val target = ceil((timeSeconds - originSeconds!!) * CPU_FREQ_HZ.toDouble()).toInt()
    while (cycles < target) {
      cycles += nes.step()
    }
  }

  companion object {
    private const val NUM_VIDEO_BUFFERS = 3   // TODO - do we actually need 3 ?
  }
}
