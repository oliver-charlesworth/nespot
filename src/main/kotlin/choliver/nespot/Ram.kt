package choliver.nespot

// TODO - power of two for efficiency?  (i.e. bitmask rather than modulo)
class Ram(size: Int) : Memory {
  private val raw = IntArray(size) { 0x00 } // Not bytes, to avoid conversion overhead

  override fun get(addr: Address): Data = raw[addr]

  override fun set(addr: Address, data: Data) {
    raw[addr] = data
  }
}
