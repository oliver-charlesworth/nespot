package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
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

const val BASE_ZERO_PAGE = 0x0000
const val BASE_STACK = 0x0100
const val BASE_RAM = 0x0200
const val BASE_ROM = 0x8000

const val PRELUDE_PC = BASE_ROM
const val INIT_PC = BASE_ROM + 0x1000
/** Chosen to straddle a page boundary. */
const val SCARY_ADDR = BASE_RAM + 0x10FF

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
    enc = { enc16(BASE_RAM + 0x3050) },
    mem = mem16(BASE_RAM + 0x3050, SCARY_ADDR)
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
      val case = CASES[mode] ?: error("Unhandled mode ${mode}")

      val instruction = listOf(enc) + case.enc(operand)

      assertCpuEffects(
        instructions = listOf(instruction),
        initState = case.state(proto).with(PC = INIT_PC.u16()).initState(),
        initStores = case.mem +             // Indirection / pointer
          (case.operandAddr to operand) +   // Operand (user-defined value, case-defined location)
          initStores,                       // User-defined
        expectedState = case.state(proto).with(PC = (INIT_PC + instruction.size).u16()).expectedState(),
        expectedStores = expectedStores(case.operandAddr),
        name = mode.name
      )
    }
  }
}

fun assertCpuEffects(
  instructions: List<List<UInt8>>,
  initState: State,
  initStores: Map<Int, Int> = emptyMap(),
  expectedState: State? = null,
  expectedStores: Map<Int, Int> = emptyMap(),
  name: String = ""
) {
  // Set up memory so PC trampolines from reset vector to prelude to user instructions
  val prelude = preludeFor(initState)
  val memory = FakeMemory(
    mapOf(
      VECTOR_RESET.toInt() to PRELUDE_PC.lo().toInt(),
      (VECTOR_RESET + 1u).toInt() to PRELUDE_PC.hi().toInt()
    ) +
      prelude.memoryMap(PRELUDE_PC) +
      instructions.memoryMap(INIT_PC) +
      initStores
  )
  val cpu = Cpu(memory)

  repeat(prelude.size) { cpu.next() }
  memory.trackStores = true
  repeat(instructions.size) { cpu.next() }

  if (expectedState != null) {
    assertEquals(expectedState, cpu.state, "Unexpected state for [${name}]")
  }
  memory.assertStores(expectedStores, "Unexpected store for [${name}]")
}

/** Prelude instructions that set CPU state (but with hardcoded PC). */
private fun preludeFor(state: State) = listOf(
  enc(LDX[IMMEDIATE], state.S.toInt()),
  enc(TXS[IMPLIED]),
  enc(LDA[IMMEDIATE], state.P.u8().toInt()),
  enc(PHA[IMPLIED]),
  enc(LDA[IMMEDIATE], state.A.toInt()),
  enc(LDX[IMMEDIATE], state.X.toInt()),
  enc(LDY[IMMEDIATE], state.Y.toInt()),
  enc(PLP[IMPLIED]),
  enc(JMP[ABSOLUTE], INIT_PC.lo().toInt(), INIT_PC.hi().toInt())
)

fun List<List<UInt8>>.memoryMap(base: Int) = flatten()
  .withIndex()
  .associate { (it.index + base) to it.value.toInt() }

fun enc(vararg bytes: Int) = bytes.map { it.u8() }

operator fun Opcode.get(mode: AddrMode): Int = encodings[mode]!!.toInt()
