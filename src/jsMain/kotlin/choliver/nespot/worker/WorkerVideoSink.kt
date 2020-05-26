package choliver.nespot.worker

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.TILE_SIZE
import choliver.nespot.VideoSink
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.set
import org.w3c.dom.ImageData

class WorkerVideoSink : VideoSink {
  private lateinit var imageData: ImageData
  private lateinit var raw: Uint16Array
  private var i: Int = 0
  private var j: Int = 0

  init {
    reset()
  }

  override fun put(color: Int) {
    if ((i++ / SCREEN_WIDTH) in (TILE_SIZE until SCREEN_HEIGHT - TILE_SIZE)) {
      raw[j++] = ((color shr 8) and 0xFF).toShort()
      raw[j++] = ((color shr 16) and 0xFF).toShort()
      raw[j++] = ((color shr 24) and 0xFF).toShort()
      raw[j++] = 255
    }

    if (i == SCREEN_WIDTH * SCREEN_HEIGHT) {
      postVideoFrame()
      reset()
    }
  }

  private fun postVideoFrame() {
    self.createImageBitmap(imageData).then { image ->
      self.postMessage(arrayOf(MSG_VIDEO_FRAME, image), transfer = arrayOf(image))
    }
  }

  private fun reset() {
    i = 0
    j = 0
    imageData = ImageData(SCREEN_WIDTH, (SCREEN_HEIGHT - 2 * TILE_SIZE))
    raw = imageData.data.unsafeCast<Uint16Array>()  // See https://stackoverflow.com/a/49336551
  }
}
