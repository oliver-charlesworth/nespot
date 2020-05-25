package choliver.nespot

class JsVideoSink(
  private val onBufferReady: (IntArray) -> Unit = {}
) : VideoSink {
  private val bufferA = IntArray(SCREEN_HEIGHT * SCREEN_WIDTH)
  private val bufferB = IntArray(SCREEN_HEIGHT * SCREEN_WIDTH)
  private var buffer = bufferA
  private var idx = 0

  override fun put(color: Int) {
    buffer[idx++] = color
    if (idx == buffer.size) {
      idx = 0
      onBufferReady(buffer)
      buffer = if (buffer === bufferA) bufferB else bufferA
    }
  }
}
