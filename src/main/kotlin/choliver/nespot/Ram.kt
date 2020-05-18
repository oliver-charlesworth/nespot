package choliver.nespot

// TODO - power of two for efficiency?  (i.e. bitmask rather than modulo)
class Ram private constructor(private val raw: ByteArray) : Memory {
  constructor(size: Int) : this(ByteArray(size) { 0xCC.toByte() })

  val size = raw.size

  override fun get(addr: Address): Data = raw[addr].data()

  override fun set(addr: Address, data: Data) {
    raw[addr] = data.toByte()
  }

  companion object {
    fun backedBy(raw: ByteArray) = Ram(raw)
  }
}
