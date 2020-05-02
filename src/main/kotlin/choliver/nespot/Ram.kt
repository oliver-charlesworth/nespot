package choliver.nespot

import java.util.*

// TODO - power of two for efficiency?  (i.e. bitmask rather than modulo)
class Ram(size: Int) : Memory {
  private val raw = IntArray(size) { 0xCC } // Not bytes, to avoid conversion overhead

  override fun load(addr: Address): Data = raw[addr]

  override fun store(addr: Address, data: Data) {
    raw[addr] = data
  }

  fun snapshot(): String = Base64.getEncoder().encodeToString(raw.map { it.toByte() }.toByteArray())
}
