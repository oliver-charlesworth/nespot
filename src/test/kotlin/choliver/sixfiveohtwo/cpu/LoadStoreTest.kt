package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.model.Opcode.*
import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    assertForAddressModes(LDA, 0x69) { with(A = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(LDA, 0x96) { with(A = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(LDA, 0x00) { with(A = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun ldx() {
    assertForAddressModes(LDX, 0x69) { with(X = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(LDX, 0x96) { with(X = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(LDX, 0x00) { with(X = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun ldy() {
    assertForAddressModes(LDY, 0x69) { with(Y = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(LDY, 0x96) { with(Y = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(LDY, 0x00) { with(Y = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun sta() {
    assertForAddressModes(
      STA,
      initState = { with(A = 0x69u) },
      expectedState = { with(A = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun stx() {
    assertForAddressModes(
      STX,
      initState = { with(X = 0x69u) },
      expectedState = { with(X = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun sty() {
    assertForAddressModes(
      STY,
      initState = { with(Y = 0x69u) },
      expectedState = { with(Y = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }
}
