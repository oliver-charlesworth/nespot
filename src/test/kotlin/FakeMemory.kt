import choliver.sixfiveohtwo.Memory
import choliver.sixfiveohtwo.UInt16
import choliver.sixfiveohtwo.UInt8

class FakeMemory : Memory {
  private val map = mutableMapOf<UInt16, UInt8>()
  private val _stores = mutableListOf<Pair<UInt16, UInt8>>()

  override fun load(address: UInt16) = map[address] ?: 0u

  override fun store(address: UInt16, data: UInt8) {
    map[address] = data
    _stores.add(address to data)
  }

  val stores get() = _stores.toList()
}
