package choliver.nespot.cpu

import choliver.nespot.Data
import choliver.nespot.cpu.model.Opcode
import choliver.nespot.cpu.model.Opcode.*
import choliver.nespot.cpu.model.Regs
import choliver.nespot.cpu.utils._0
import choliver.nespot.cpu.utils._1
import org.junit.jupiter.api.Test

class TransferTest {
  @Test
  fun txa() {
    assertTransferAndFlags(TXA, source = { with(x = it) }, dest = { with(a = it) })
  }

  @Test
  fun tya() {
    assertTransferAndFlags(TYA, source = { with(y = it) }, dest = { with(a = it) })
  }

  @Test
  fun tax() {
    assertTransferAndFlags(TAX, source = { with(a = it) }, dest = { with(x = it) })
  }

  @Test
  fun tay() {
    assertTransferAndFlags(TAY, source = { with(a = it) }, dest = { with(y = it) })
  }

  @Test
  fun tsx() {
    assertTransferAndFlags(TSX, source = { with(s = it) }, dest = { with(x = it) })
  }

  @Test
  fun txs() {
    assertForAddressModes(
      TXS,
      initRegs = { with(x = 0x10) },
      expectedRegs = { with(x = 0x10, s = 0x10) }
    )
  }

  private fun assertTransferAndFlags(
    op: Opcode,
    source: Regs.(Data) -> Regs,
    dest: Regs.(Data) -> Regs
  ) {
    assertForAddressModes(
      op,
      initRegs = { source(0x10) },
      expectedRegs = { source(0x10).dest(0x10).with(z = _0, n = _0) }
    )
    assertForAddressModes(
      op,
      initRegs = { source(0xF0) },
      expectedRegs = { source(0xF0).dest(0xF0).with(z = _0, n = _1) }
    )
    assertForAddressModes(
      op,
      initRegs = { source(0x00) },
      expectedRegs = { source(0x00).dest(0x00).with(z = _1, n = _0) }
    )
  }
}
