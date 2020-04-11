package choliver.nes

interface Memory {
  fun load(addr: UInt16): UInt8
  fun store(addr: UInt16, data: UInt8): Unit = throw UnsupportedOperationException()
}
