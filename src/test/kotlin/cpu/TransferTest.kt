package cpu

import assertForAddressModes
import choliver.sixfiveohtwo.AddrMode.IMPLIED
import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.UInt8
import choliver.sixfiveohtwo._0
import choliver.sixfiveohtwo._1
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun txa() {
    assertTransferAndFlags(0x8A, source = { with(X = it) }, dest = { with(A = it) })
  }

  @Test
  fun tya() {
    assertTransferAndFlags(0x98, source = { with(Y = it) }, dest = { with(A = it) })
  }

  @Test
  fun tax() {
    assertTransferAndFlags(0xAA, source = { with(A = it) }, dest = { with(X = it) })
  }

  @Test
  fun tay() {
    assertTransferAndFlags(0xA8, source = { with(A = it) }, dest = { with(Y = it) })
  }

  @Test
  fun tsx() {
    assertTransferAndFlags(0xBA, source = { with(S = it) }, dest = { with(X = it) })
  }

  @Test
  fun txs() {
    assertForAddressModes(
      mapOf(IMPLIED to 0x9A),
      originalState = { with(X = 0x10u) },
      expectedState = { with(X = 0x10u, S = 0x10u) }
    )
  }

  private fun assertTransferAndFlags(
    opcode: Int,
    source: State.(UInt8) -> State,
    dest: State.(UInt8) -> State
  ) {
    assertForAddressModes(
      mapOf(IMPLIED to opcode),
      originalState = { source(0x10u) },
      expectedState = { source(0x10u).dest(0x10u).with(Z = _0, N = _0) }
    )
    assertForAddressModes(
      mapOf(IMPLIED to opcode),
      originalState = { source(0xF0u) },
      expectedState = { source(0xF0u).dest(0xF0u).with(Z = _0, N = _1) }
    )
    assertForAddressModes(
      mapOf(IMPLIED to opcode),
      originalState = { source(0x00u) },
      expectedState = { source(0x00u).dest(0x00u).with(Z = _1, N = _0) }
    )
  }

}
