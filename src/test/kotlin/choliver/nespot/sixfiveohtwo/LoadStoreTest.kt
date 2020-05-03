package choliver.nespot.sixfiveohtwo

import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    assertForAddressModes(LDA, target = 0x69) { with(a =  0x69, z =  _0, n =  _0) }
    assertForAddressModes(LDA, target = 0x96) { with(a =  0x96, z =  _0, n =  _1) }
    assertForAddressModes(LDA, target = 0x00) { with(a =  0x00, z =  _1, n =  _0) }
  }

  @Test
  fun ldx() {
    assertForAddressModes(LDX, target = 0x69) { with(x =  0x69, z =  _0, n =  _0) }
    assertForAddressModes(LDX, target = 0x96) { with(x =  0x96, z =  _0, n =  _1) }
    assertForAddressModes(LDX, target = 0x00) { with(x =  0x00, z =  _1, n =  _0) }
  }

  @Test
  fun ldy() {
    assertForAddressModes(LDY, target = 0x69) { with(y =   0x69, z =  _0, n =  _0) }
    assertForAddressModes(LDY, target = 0x96) { with(y =   0x96, z =  _0, n =  _1) }
    assertForAddressModes(LDY, target = 0x00) { with(y =   0x00, z =  _1, n =  _0) }
  }

  @Test
  fun sta() {
    assertForAddressModes(
      STA,
      initState = { with(a =  0x69) },
      expectedState = { with(a =  0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun stx() {
    assertForAddressModes(
      STX,
      initState = { with(x =  0x69) },
      expectedState = { with(x =  0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun sty() {
    assertForAddressModes(
      STY,
      initState = { with(y =   0x69) },
      expectedState = { with(y =   0x69) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }
}
