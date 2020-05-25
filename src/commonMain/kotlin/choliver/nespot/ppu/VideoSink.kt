package choliver.nespot.ppu

interface VideoSink {
  operator fun set(idx: Int, color: Int)
  fun commit()
}
