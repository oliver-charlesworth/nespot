package choliver.sixfiveohtwo.model

interface Memory {
  fun load(address: UInt16): UInt8
  fun store(address: UInt16, data: UInt8): Unit = throw UnsupportedOperationException()
}
