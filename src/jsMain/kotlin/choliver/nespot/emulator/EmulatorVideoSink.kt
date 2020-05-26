package choliver.nespot.emulator

import choliver.nespot.*
import org.khronos.webgl.*

class EmulatorVideoSink : VideoSink {
  private lateinit var raw: Uint8ClampedArray
  private lateinit var view: Uint16Array
  private var i: Int = 0
  private var j: Int = 0

  init {
    reset()
  }

  override fun put(color: Int) {
    if ((i++ / SCREEN_WIDTH) in (TILE_SIZE until SCREEN_HEIGHT - TILE_SIZE)) {
      view[j++] = ((color shr 8) and 0xFF).toShort()
      view[j++] = ((color shr 16) and 0xFF).toShort()
      view[j++] = ((color shr 24) and 0xFF).toShort()
      view[j++] = 255.toShort()
    }

    if (i == SCREEN_WIDTH * SCREEN_HEIGHT) {
      postVideoFrame()
      reset()
    }
  }

  private fun postVideoFrame() {
    self.postMessage(arrayOf(MSG_VIDEO_FRAME, raw.buffer), transfer = arrayOf(raw.buffer))
  }

  private fun reset() {
    i = 0
    j = 0
    raw = Uint8ClampedArray(VISIBLE_WIDTH * VISIBLE_HEIGHT * 4)
    view = raw.unsafeCast<Uint16Array>()  // See https://stackoverflow.com/a/49336551
  }
}
