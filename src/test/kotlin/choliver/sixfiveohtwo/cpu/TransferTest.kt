package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.Opcode
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.State
import choliver.sixfiveohtwo.UInt8
import choliver.sixfiveohtwo.assertForAddressModes
import choliver.sixfiveohtwo.utils._0
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun txa() {
    assertTransferAndFlags(TXA, source = { with(X = it) }, dest = { with(A = it) })
  }

  @Test
  fun tya() {
    assertTransferAndFlags(TYA, source = { with(Y = it) }, dest = { with(A = it) })
  }

  @Test
  fun tax() {
    assertTransferAndFlags(TAX, source = { with(A = it) }, dest = { with(X = it) })
  }

  @Test
  fun tay() {
    assertTransferAndFlags(TAY, source = { with(A = it) }, dest = { with(Y = it) })
  }

  @Test
  fun tsx() {
    assertTransferAndFlags(TSX, source = { with(S = it) }, dest = { with(X = it) })
  }

  @Test
  fun txs() {
    assertForAddressModes(
      TXS,
      initState = { with(X = 0x10u) },
      expectedState = { with(X = 0x10u, S = 0x10u) }
    )
  }

  private fun assertTransferAndFlags(
    op: Opcode,
    source: State.(UInt8) -> State,
    dest: State.(UInt8) -> State
  ) {
    assertForAddressModes(
      op,
      initState = { source(0x10u) },
      expectedState = { source(0x10u).dest(0x10u).with(Z = _0, N = _0) }
    )
    assertForAddressModes(
      op,
      initState = { source(0xF0u) },
      expectedState = { source(0xF0u).dest(0xF0u).with(Z = _0, N = _1) }
    )
    assertForAddressModes(
      op,
      initState = { source(0x00u) },
      expectedState = { source(0x00u).dest(0x00u).with(Z = _1, N = _0) }
    )
  }

}