package choliver.nespot

import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

class BrowserScreen(canvas: HTMLCanvasElement) {
  private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
  private val img = ctx.createImageData(
    SCREEN_WIDTH.toDouble(),
    (SCREEN_HEIGHT - 2 * TILE_SIZE).toDouble()
  )
  private val data = img.data.unsafeCast<Uint16Array>() // See https://stackoverflow.com/a/49336551
  private val videoBuffers = Array(NUM_VIDEO_BUFFERS) { IntArray(SCREEN_WIDTH * SCREEN_HEIGHT) }
  private var iBuffer = 0
  private var iPixel = 0

  val width = img.width
  val height = img.height

  val sink = object : VideoSink {
    private var buffer = videoBuffers[iBuffer]
    override fun put(color: Int) {
      buffer[iPixel++] = color
      if (iPixel == buffer.size) {
        iPixel = 0
        iBuffer = (iBuffer + 1) % NUM_VIDEO_BUFFERS
        buffer = videoBuffers[iBuffer]
      }
    }
  }

  fun redraw() {
    val buffer = videoBuffers[(iBuffer + NUM_VIDEO_BUFFERS - 1) % NUM_VIDEO_BUFFERS]
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
    private const val NUM_VIDEO_BUFFERS = 3   // TODO - do we actually need 3 ?
  }
}
