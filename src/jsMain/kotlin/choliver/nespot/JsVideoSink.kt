package choliver.nespot

class JsVideoSink(
  private val onBufferReady: (IntArray) -> Unit = {}
) : VideoSink {
  private val bufferA = IntArray(SCREEN_HEIGHT * SCREEN_WIDTH)
  private val bufferB = IntArray(SCREEN_HEIGHT * SCREEN_WIDTH)
  private var buffer = bufferA

  override fun set(idx: Int, color: Int) {
    buffer[idx] = color
  }

  override fun commit() {
    onBufferReady(buffer)
    buffer = if (buffer === bufferA) bufferB else bufferA
  }
}
