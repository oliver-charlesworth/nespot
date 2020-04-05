package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import org.junit.jupiter.api.Assertions.assertEquals

private data class Case(
  val enc: (operand: Int) -> Array<UInt8>,
  val state: State.() -> State = { this },
  val mem: Map<Int, Int> = emptyMap(),
  val operandAddr: Int = 0x0000
)

private val PROTO_STATES = listOf(
  State(),
  State(P = Flags(C = true)),
  State(P = Flags(Z = true)),
  State(P = Flags(I = true)),
  State(P = Flags(D = true)),
  State(P = Flags(V = true)),
  State(P = Flags(N = true)),
  State(A = 0xCCu),
  State(X = 0xCCu),
  State(Y = 0xCCu),
  State(S = 0xCCu)
)

/** Chosen to straddle a page boundary. */
const val SCARY_ADDR = 0x12FF

private val CASES = mapOf(
  IMMEDIATE to Case(enc = { enc(it) }),
  IMPLIED to Case(enc = { emptyArray() }),
  ZERO_PAGE to Case(
    enc = { enc(0x30) },
    operandAddr = 0x0030
  ),
  ZERO_PAGE_X to Case(
    enc = { enc(0x30) },
    state = { with(X = 0x20u) },
    operandAddr = 0x0050
  ),
  ZERO_PAGE_Y to Case(
    enc = { enc(0x30) },
    state = { with(Y = 0x20u) },
    operandAddr = 0x0050
  ),
  ABSOLUTE to Case(
    enc = { enc16(SCARY_ADDR) },
    operandAddr = SCARY_ADDR
  ),
  ABSOLUTE_X to Case(
    enc = { enc16(SCARY_ADDR - 0x20) },
    state = { with(X = 0x20u) },
    operandAddr = SCARY_ADDR
  ),
  ABSOLUTE_Y to Case(
    enc = { enc16(SCARY_ADDR - 0x20) },
    state = { with(Y = 0x20u) },
    operandAddr = SCARY_ADDR
  ),
  INDIRECT to Case(
    enc = { enc16(0x4050) },
    mem = mem16(0x4050, SCARY_ADDR)
  ),
  INDEXED_INDIRECT to Case(
    enc = { enc(0x30) },
    state = { with(X = 0x10u) },
    mem = mem16(0x0040, SCARY_ADDR),
    operandAddr = SCARY_ADDR
  ),
  INDIRECT_INDEXED to Case(
    enc = { enc(0x30) },
    state = { with(Y = 0x10u) },
    mem = mem16(0x0030, SCARY_ADDR - 0x10),
    operandAddr = SCARY_ADDR
  )
)

private fun enc16(data: Int) = enc(data.loI(), data.hiI())
private fun mem16(addr: Int, data: Int) = mapOf(addr to data.loI(), (addr + 1) to data.hiI())

fun assertForAddressModes(
  ops: Map<AddrMode, Int>,
  operand: Int = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Int, Int> = emptyMap(),
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State = { this }
) {
  ops.forEach { (mode, enc) ->
    PROTO_STATES.forEach { proto ->
      val case = CASES[mode]!!

      val memory = FakeMemory(case.mem + (case.operandAddr to operand) + initStores)
      val cpu = Cpu(memory)

      val encoding = enc(enc) + case.enc(operand)
      val init = case.state(proto).with(PC = 0x5678u).initState()
      val expected = case.state(proto).with(PC = (0x5678u + encoding.size.u8()).u16()).expectedState()

      assertEquals(expected, cpu.execute(encoding, init), "Unexpected state for [${mode.name}]")
      memory.assertStores(expectedStores(case.operandAddr), "Unexpected store for [${mode.name}]")
    }
  }
}

fun enc(vararg bytes: Int) = bytes.map { it.u8() }.toTypedArray()
