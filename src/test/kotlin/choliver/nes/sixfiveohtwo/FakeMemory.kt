package choliver.nes.sixfiveohtwo

import choliver.nes.Address
import choliver.nes.Data
import choliver.nes.Memory
import org.junit.jupiter.api.Assertions.assertEquals

class FakeMemory(
  initial: Map<Address, Data> = emptyMap()
) : Memory {
  private val stores = mutableMapOf<Address, Data>()
  private val map = initial.toMutableMap()
  var trackStores = false

  override fun load(addr: Address) = map[addr] ?: 0xCC  // Easier to spot during debugging than 0x00

  override fun store(addr: Address, data: Data) {
    map[addr] = data
    if (trackStores) {
      stores[addr] = data
    }
  }

  // TODO - inline?
  fun assertStores(expected: Map<Int, Int>, message: String? = null) {
    assertEquals(expected, stores, message)
  }
}
