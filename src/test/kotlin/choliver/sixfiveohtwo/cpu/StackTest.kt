package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.model.u8
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class StackTest {
  @Test
  fun pha() {
    assertForAddressModes(
      PHA,
      initState = { with(S = 0x30u, A = 0x20u) },
      expectedState = { with(S = 0x2Fu, A = 0x20u) },
      expectedStores = { mapOf(0x0130 to 0x20) }
    )
  }

  @Test
  fun php() {
    assertForAddressModes(
      PHP,
      initState = { with(S = 0x30u, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) },
      expectedState = { with(S = 0x2Fu, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) },
      expectedStores = { mapOf(0x0130 to 0xDF) }  // Note B is also set on stack
    )
  }

  @Test
  fun pla() {
    fun assertBehaviour(data: Int, Z: Boolean, N: Boolean) {
      assertForAddressModes(
        PLA,
        initStores = mapOf(0x123 to data),
        initState = { with(S = 0x22u) },
        expectedState = { with(S = 0x23u, A = data.u8(), Z = Z, N = N) }
      )
    }

    assertBehaviour(0x30, Z = _0, N = _0)
    assertBehaviour(0xD0, Z = _0, N = _1)
    assertBehaviour(0x00, Z = _1, N = _0)
  }

  @Test
  fun plp() {
    assertForAddressModes(
      PLP,
      initStores = mapOf(0x123 to 0xCF),
      initState = { with(S = 0x22u) },
      expectedState = { with(S = 0x23u, N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1) }
    )
  }
}
