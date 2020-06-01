package choliver.nespot.cartridge

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BankMapTest {
  @Test
  fun `maps bank addresses`() {
    val bankMap = BankMap(bankSize = 4096, addressSpaceSize = 16384)

    bankMap[0] = 3
    bankMap[1] = 0
    bankMap[2] = 1
    bankMap[3] = 2

    mapOf(
      0x0000 to 0x3000,
      0x1000 to 0x0000,
      0x2000 to 0x1000,
      0x3000 to 0x2000
    ).forEach { (inputBase, outputBase) ->
      assertEquals(outputBase + 0x0000, bankMap.map(inputBase + 0x0000))
      assertEquals(outputBase + 0x0400, bankMap.map(inputBase + 0x0400))
      assertEquals(outputBase + 0x0800, bankMap.map(inputBase + 0x0800))
      assertEquals(outputBase + 0x0C00, bankMap.map(inputBase + 0x0C00))
      assertEquals(outputBase + 0x0FFF, bankMap.map(inputBase + 0x0FFF))
    }
  }

  @Test
  fun `has linear initial mapping`() {
    val bankMap = BankMap(bankSize = 4096, addressSpaceSize = 16384)

    mapOf(
      0x0000 to 0x0000,
      0x1000 to 0x1000,
      0x2000 to 0x2000,
      0x3000 to 0x3000
    ).forEach { (inputBase, outputBase) ->
      assertEquals(outputBase + 0x0000, bankMap.map(inputBase + 0x0000))
      assertEquals(outputBase + 0x0400, bankMap.map(inputBase + 0x0400))
      assertEquals(outputBase + 0x0800, bankMap.map(inputBase + 0x0800))
      assertEquals(outputBase + 0x0C00, bankMap.map(inputBase + 0x0C00))
      assertEquals(outputBase + 0x0FFF, bankMap.map(inputBase + 0x0FFF))
    }
  }
}
