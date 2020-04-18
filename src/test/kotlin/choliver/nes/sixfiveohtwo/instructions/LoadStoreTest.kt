package choliver.nes.sixfiveohtwo.instructions

import choliver.nes.sixfiveohtwo.assertForAddressModes
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.utils._0
import choliver.nes.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    assertForAddressModes(LDA, target = 0x69) { with(A = 0x69, Z = _0, N = _0) }
    assertForAddressModes(LDA, target = 0x96) { with(A = 0x96, Z = _0, N = _1) }
    assertForAddressModes(LDA, target = 0x00) { with(A = 0x00, Z = _1, N = _0) }
  }

  @Test
  fun ldx() {
    assertForAddressModes(LDX, target = 0x69) { with(X = 0x69, Z = _0, N = _0) }
    assertForAddressModes(LDX, target = 0x96) { with(X = 0x96, Z = _0, N = _1) }
    assertForAddressModes(LDX, target = 0x00) { with(X = 0x00, Z = _1, N = _0) }
  }

  @Test
  fun ldy() {
    assertForAddressModes(LDY, target = 0x69) { with(Y = 0x69, Z = _0, N = _0) }
    assertForAddressModes(LDY, target = 0x96) { with(Y = 0x96, Z = _0, N = _1) }
    assertForAddressModes(LDY, target = 0x00) { with(Y = 0x00, Z = _1, N = _0) }
  }

  @Test
  fun sta() {
    assertForAddressModes(
      STA,
      initState = { with(A = 0x69) },
      expectedState = { with(A = 0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun stx() {
    assertForAddressModes(
      STX,
      initState = { with(X = 0x69) },
      expectedState = { with(X = 0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun sty() {
    assertForAddressModes(
      STY,
      initState = { with(Y = 0x69) },
      expectedState = { with(Y = 0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }
}
