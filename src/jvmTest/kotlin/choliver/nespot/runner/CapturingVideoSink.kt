package choliver.nespot.runner

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.VideoSink

class CapturingVideoSink(private val delegate: VideoSink) : VideoSink {
  private val bufferA = MutableList(SCREEN_WIDTH * SCREEN_HEIGHT) { 0 }
  private val bufferB = MutableList(SCREEN_WIDTH * SCREEN_HEIGHT) { 0 }
  private var buffer = bufferA
  private var idx = 0

  val snapshot: List<Int> get() = (if (buffer === bufferA) bufferB else bufferA).toList()

  override val colorPackingMode = delegate.colorPackingMode

  override fun put(color: Int) {
    delegate.put(color)
    buffer[idx++] = color
    if (idx == SCREEN_WIDTH * SCREEN_HEIGHT) {
      buffer = if (buffer === bufferA) bufferB else bufferA
      idx = 0
    }
  }
}
