package choliver.nespot.sixfiveohtwo.instructions

import choliver.nespot.Data
import choliver.nespot.sixfiveohtwo.assertForAddressModes
import choliver.nespot.sixfiveohtwo.model.Opcode
import choliver.nespot.sixfiveohtwo.model.Opcode.*
import choliver.nespot.sixfiveohtwo.model.State
import choliver.nespot.sixfiveohtwo.utils._0
import choliver.nespot.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun txa() {
    assertTransferAndFlags(TXA, source = { with(x =  it) }, dest = { with(a =  it) })
  }

  @Test
  fun tya() {
    assertTransferAndFlags(TYA, source = { with(y =  it) }, dest = { with(a =  it) })
  }

  @Test
  fun tax() {
    assertTransferAndFlags(TAX, source = { with(a =  it) }, dest = { with(x =  it) })
  }

  @Test
  fun tay() {
    assertTransferAndFlags(TAY, source = { with(a =  it) }, dest = { with(y =  it) })
  }

  @Test
  fun tsx() {
    assertTransferAndFlags(TSX, source = { with(s =  it) }, dest = { with(x =  it) })
  }

  @Test
  fun txs() {
    assertForAddressModes(
      TXS,
      initState = { with(x =  0x10) },
      expectedState = { with(x =  0x10, s =  0x10) }
    )
  }

  private fun assertTransferAndFlags(
    op: Opcode,
    source: State.(Data) -> State,
    dest: State.(Data) -> State
  ) {
    assertForAddressModes(
      op,
      initState = { source(0x10) },
      expectedState = { source(0x10).dest(0x10).with(z =  _0, n =  _0) }
    )
    assertForAddressModes(
      op,
      initState = { source(0xF0) },
      expectedState = { source(0xF0).dest(0xF0).with(z =  _0, n =  _1) }
    )
    assertForAddressModes(
      op,
      initState = { source(0x00) },
      expectedState = { source(0x00).dest(0x00).with(z =  _1, n =  _0) }
    )
  }

}
