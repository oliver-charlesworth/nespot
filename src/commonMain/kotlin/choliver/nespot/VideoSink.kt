package choliver.nespot

interface VideoSink {
  operator fun set(idx: Int, color: Int)
  fun commit()
}
