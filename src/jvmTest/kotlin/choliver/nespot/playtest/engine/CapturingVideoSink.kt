package choliver.nespot.playtest.engine

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.VideoSink

class CapturingVideoSink(private val delegate: VideoSink) : VideoSink {
  private var idx = 0
  private var buffer = mutableListOf<Byte>()
  private var _snapshot = listOf<Byte>()
  val snapshot get() = _snapshot

  override val colorPackingMode = delegate.colorPackingMode

  override fun put(color: Int) {
    delegate.put(color)
    buffer.add((color shr 0).toByte())
    buffer.add((color shr 8).toByte())
    buffer.add((color shr 16).toByte())
    buffer.add((color shr 24).toByte())
    if (++idx == SCREEN_WIDTH * SCREEN_HEIGHT) {
      _snapshot = buffer
      buffer = mutableListOf()
      idx = 0
    }
  }
}
