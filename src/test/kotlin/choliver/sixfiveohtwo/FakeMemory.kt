package choliver.sixfiveohtwo

import org.junit.jupiter.api.Assertions.*

class FakeMemory(
  initial: Map<Int, Int> = emptyMap()
) : Memory {
  private val stores = mutableMapOf<UInt16, UInt8>()
  private val map = initial.entries
    .associate { (k, v) -> k.u16() to v.u8() }
    .toMutableMap()

  override fun load(address: UInt16) = map[address] ?: 0u

  override fun store(address: UInt16, data: UInt8) {
    map[address] = data
    stores[address] = data
  }

  fun assertStores(expected: Map<Int, Int>, message: String? = null) {
    assertEquals(
      expected.entries.associate { (k, v) -> k.u16() to v.u8() },
      stores,
      message
    )
  }
}
