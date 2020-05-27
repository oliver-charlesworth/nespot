package choliver.nespot.emulator

import choliver.nespot.*
import choliver.nespot.VideoSink.ColorPackingMode.ABGR
import org.khronos.webgl.*

class EmulatorVideoSink : VideoSink {
  private lateinit var raw: Int32Array
  private var x: Int = 0
  private var y: Int = 0
  private var j: Int = 0

  init {
    reset()
  }

  override val colorPackingMode = ABGR

  override fun put(color: Int) {
    if (y in (TILE_SIZE until SCREEN_HEIGHT - TILE_SIZE)) {
      raw[j++] = color
    }
    if (++x == SCREEN_WIDTH) {
      x = 0
      if (++y == SCREEN_HEIGHT) {
        postVideoFrame()
        reset()
      }
    }
  }

  private fun postVideoFrame() {
    self.postMessage(arrayOf(MSG_VIDEO_FRAME, raw.buffer), transfer = arrayOf(raw.buffer))
  }

  private fun reset() {
    x = 0
    y = 0
    j = 0
    raw = Int32Array(VISIBLE_WIDTH * VISIBLE_HEIGHT)
  }
}
