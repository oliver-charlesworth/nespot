package choliver.sixfiveohtwo.cpu

import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.Opcode.*
import choliver.sixfiveohtwo.utils._1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BizarreTest {

  @Test
  fun bizarre() {
    val state = State(
      A = 0x30u,
      X = 0x40u,
      Y = 0x50u,
      S = 0x60u,
      P = Flags(N = _1, V = _1, D = _1, I = _1, Z = _1, C = _1),
      PC = 0x1230u
    )

    val instructions = listOf(
      enc(LDX[IMMEDIATE], state.S.toInt()),
      enc(TXS[IMPLIED]),
      enc(LDA[IMMEDIATE], state.P.u8().toInt()),
      enc(PHA[IMPLIED]),
      enc(LDA[IMMEDIATE], state.A.toInt()),
      enc(LDX[IMMEDIATE], state.X.toInt()),
      enc(LDY[IMMEDIATE], state.Y.toInt()),
      enc(PLP[IMPLIED]),
      enc(JMP[ABSOLUTE], state.PC.lo().toInt(), state.PC.hi().toInt())
    )

    // TODO - de-duplicate this
    val mem = FakeMemory(instructions
      .flatMap { it.toList() }
      .withIndex()
      .associate { (it.index + INIT_PC) to it.value.toInt() }
    )
    val cpu = Cpu(mem, State(PC = INIT_PC.u16()))

    repeat(instructions.size) { cpu.next() }

    assertEquals(state, cpu.state)
  }
}
