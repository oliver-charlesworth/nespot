package choliver.nes.sixfiveohtwo

import choliver.nes.*
import org.junit.jupiter.api.Assertions.assertEquals

class FakeMemory(
  initial: Map<Int, Int> = emptyMap()
) : Memory {
  private val stores = mutableMapOf<UInt16, UInt8>()
  private val map = initial.entries
    .associate { (k, v) -> k.u16() to v.u8() }
    .toMutableMap()
  var trackStores = false

  override fun load(addr: UInt16) = map[addr] ?: 0xCCu  // Easier to spot during debugging than 0x00

  override fun store(addr: UInt16, data: UInt8) {
    map[addr] = data
    if (trackStores) {
      stores[addr] = data
    }
  }

  fun assertStores(expected: Map<Int, Int>, message: String? = null) {
    assertEquals(
      expected.entries.associate { (k, v) -> k.u16() to v.u8() },
      stores,
      message
    )
  }
}
