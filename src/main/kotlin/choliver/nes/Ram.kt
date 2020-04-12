package choliver.nes

// TODO - power of two for efficiency?  (i.e. bitmask rather than modulo)
class Ram(size: Int) : Memory {
  private val raw = ByteArray(size) { 0xCC.toByte() }

  override fun load(addr: Address) = raw[addr].data()

  override fun store(addr: Address, data: Data) {
    raw[addr] = data.toByte()
  }
}
