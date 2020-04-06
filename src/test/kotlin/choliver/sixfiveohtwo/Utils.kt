package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.Opcode.*
import org.junit.jupiter.api.Assertions.assertEquals

private data class Case(
  val enc: (operand: Int) -> List<UInt8>,
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
  State(A = 0xAAu),
  State(X = 0xAAu),
  State(Y = 0xAAu),
  State(S = 0xAAu)
)

/** Chosen to straddle a page boundary. */
const val SCARY_ADDR = 0x12FF

const val INIT_PC = 0x8000
const val OTHER_PC = 0x1000

private val CASES = mapOf(
  ACCUMULATOR to Case(enc = { emptyList() }),
  IMMEDIATE to Case(enc = { enc(it) }),
  IMPLIED to Case(enc = { emptyList() }),
  RELATIVE to Case(enc = { enc(it) }),
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

fun enc16(data: Int) = enc(data.loI(), data.hiI())
fun mem16(addr: Int, data: Int) = mapOf(addr to data.loI(), (addr + 1) to data.hiI())

fun assertForAddressModes(
  op: Opcode,
  operand: Int = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Int, Int> = emptyMap(),
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State = { this }
) {
  assertForAddressModes(
    op.encodings,
    operand,
    initState,
    initStores,
    expectedStores,
    expectedState
  )
}

fun assertForAddressModes(
  encodings: Map<AddrMode, UInt8>,
  operand: Int = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Int, Int> = emptyMap(),
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State = { this }
) {
  encodings.forEach { (mode, enc) ->
    PROTO_STATES.forEach { proto ->
      val case = CASES[mode]!!

      val instruction = listOf(enc) + case.enc(operand)

      val init = case.state(proto).with(PC = INIT_PC.u16()).initState()
      val expected = case.state(proto).with(PC = (INIT_PC + instruction.size).u16()).expectedState()

      val prelude = preludeFor(init)

      val memory = FakeMemory(
        prelude.memoryMap(OTHER_PC) +
          listOf(instruction).memoryMap(INIT_PC) +
          case.mem +                        // Indirection / pointer
          (case.operandAddr to operand) +   // Operand (user-defined value, case-defined location)
          initStores                        // User-defined
      )
      val cpu = Cpu(memory, State(PC = OTHER_PC.u16()))
      repeat(prelude.size) { cpu.next() }

      memory.trackStores = true
      cpu.next()

      assertEquals(expected, cpu.state, "Unexpected state for [${mode.name}]")
      memory.assertStores(expectedStores(case.operandAddr), "Unexpected store for [${mode.name}]")
    }
  }
}

fun preludeFor(state: State) = listOf(
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

fun List<List<UInt8>>.memoryMap(base: Int) = flatten()
  .withIndex()
  .associate { (it.index + base) to it.value.toInt() }

fun enc(vararg bytes: Int) = bytes.map { it.u8() }

operator fun Opcode.get(mode: AddrMode): Int = encodings[mode]!!.toInt()
