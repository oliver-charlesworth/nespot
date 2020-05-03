package choliver.nespot.sixfiveohtwo

import choliver.nespot.Data
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class StackTest {
  @Test
  fun pha() {
    assertForAddressModes(
      PHA,
      initState = { with(s = 0x30, a = 0x20) },
      expectedState = { with(s = 0x2F, a = 0x20) },
      expectedStores = { listOf(0x0130 to 0x20) }
    )
  }

  @Test
  fun php() {
    assertForAddressModes(
      PHP,
      initState = { with(s = 0x30, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) },
      expectedState = { with(s = 0x2F, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) },
      expectedStores = { listOf(0x0130 to 0xDF) }  // Note B is also set on stack
    )
  }

  @Test
  fun pla() {
    fun assertBehaviour(data: Data, z: Boolean, n: Boolean) {
      assertForAddressModes(
        PLA,
        initStores = mapOf(0x123 to data),
        initState = { with(s = 0x22) },
        expectedState = { with(s = 0x23, a = data, z = z, n = n) }
      )
    }

    assertBehaviour(0x30, z = _0, n = _0)
    assertBehaviour(0xD0, z = _0, n = _1)
    assertBehaviour(0x00, z = _1, n = _0)
  }

  @Test
  fun plp() {
    assertForAddressModes(
      PLP,
      initStores = mapOf(0x123 to 0xCF),
      initState = { with(s = 0x22) },
      expectedState = { with(s = 0x23, n = _1, v = _1, d = _1, i = _1, z = _1, c = _1) }
    )
  }
}
