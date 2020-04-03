package cpu

import assertForAddressModes
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import org.junit.jupiter.api.Test

class LoadStoreTest {
  @Test
  fun lda() {
    val ops = mapOf(
      IMMEDIATE to 0xA9,
      ZERO_PAGE to 0XA5,
      ZERO_PAGE_X to 0xB5,
      ABSOLUTE to 0xAD,
      ABSOLUTE_X to 0xBD,
      ABSOLUTE_Y to 0xB9,
      INDEXED_INDIRECT to 0xA1,
      INDIRECT_INDEXED to 0xB1
    )
    assertForAddressModes(ops, 0x69) { with(A = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(ops, 0x96) { with(A = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(ops, 0x00) { with(A = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun ldx() {
    val ops = mapOf(
      IMMEDIATE to 0xA2,
      ZERO_PAGE to 0xA6,
      ZERO_PAGE_Y to 0xB6,
      ABSOLUTE to 0xAE,
      ABSOLUTE_Y to 0xBE
    )
    assertForAddressModes(ops, 0x69) { with(X = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(ops, 0x96) { with(X = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(ops, 0x00) { with(X = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun ldy() {
    val ops = mapOf(
      IMMEDIATE to 0xA0,
      ZERO_PAGE to 0xA4,
      ZERO_PAGE_X to 0xB4,
      ABSOLUTE to 0xAC,
      ABSOLUTE_X to 0xBC
    )
    assertForAddressModes(ops, 0x69) { with(Y = 0x69u, Z = _0, N = _0) }
    assertForAddressModes(ops, 0x96) { with(Y = 0x96u, Z = _0, N = _1) }
    assertForAddressModes(ops, 0x00) { with(Y = 0x00u, Z = _1, N = _0) }
  }

  @Test
  fun sta() {
    assertForAddressModes(
      mapOf(
        ZERO_PAGE to 0x85,
        ZERO_PAGE_X to 0x95,
        ABSOLUTE to 0x8D,
        ABSOLUTE_X to 0x9D,
        ABSOLUTE_Y to 0x99,
        INDEXED_INDIRECT to 0x81,
        INDIRECT_INDEXED to 0x91
      ),
      originalState = { with(A = 0x69u) },
      expectedState = { with(A = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun stx() {
    assertForAddressModes(
      mapOf(
        ZERO_PAGE to 0x86,
        ZERO_PAGE_Y to 0x96,
        ABSOLUTE to 0x8E
      ),
      originalState = { with(X = 0x69u) },
      expectedState = { with(X = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }

  @Test
  fun sty() {
    assertForAddressModes(
      mapOf(
        ZERO_PAGE to 0x84,
        ZERO_PAGE_X to 0x94,
        ABSOLUTE to 0x8C
      ),
      originalState = { with(Y = 0x69u) },
      expectedState = { with(Y = 0x69u) },
      expectedStores = { mapOf(it to 0x69) }
    )
  }
}
