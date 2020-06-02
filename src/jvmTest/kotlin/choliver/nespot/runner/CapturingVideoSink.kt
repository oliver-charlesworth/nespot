package choliver.nespot.runner

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.VideoSink

class CapturingVideoSink(private val delegate: VideoSink) : VideoSink {
  private val bufferA = MutableList<Byte>(SCREEN_WIDTH * SCREEN_HEIGHT * 4) { 0 }
  private val bufferB = MutableList<Byte>(SCREEN_WIDTH * SCREEN_HEIGHT * 4) { 0 }
  private var buffer = bufferA
  private var idx = 0

  val snapshot get() = (if (buffer === bufferA) bufferB else bufferA).toList()

  override val colorPackingMode = delegate.colorPackingMode

  override fun put(color: Int) {
    delegate.put(color)
    buffer[idx++] = (color shr 0).toByte()
    buffer[idx++] = (color shr 8).toByte()
    buffer[idx++] = (color shr 16).toByte()
    buffer[idx++] = (color shr 24).toByte()
    if (idx == buffer.size) {
      buffer = if (buffer === bufferA) bufferB else bufferA
      idx = 0
    }
  }
}
