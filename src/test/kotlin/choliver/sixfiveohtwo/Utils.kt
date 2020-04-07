package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.model.AddressMode.*
import choliver.sixfiveohtwo.model.Operand.*
import choliver.sixfiveohtwo.model.Operand.IndexSource.*
import choliver.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.sixfiveohtwo.model.*
import choliver.sixfiveohtwo.model.Opcode.*
import org.junit.jupiter.api.Assertions.assertEquals

// TODO - rename "operand" throughout
private data class Case(
  val enc: UInt8.(operand: Int) -> List<UInt8>,
  val gimp: (operand: Int) -> Operand,
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

const val BASE_TRAMPOLINE = BASE_ROM
const val BASE_USER = BASE_ROM + 0x1000
/** Chosen to straddle a page boundary. */
const val SCARY_ADDR = BASE_RAM + 0x10FF

private val CASES = mapOf(
  IMPLIED to Case(enc = { enc() }, gimp = { Implied }),
  ACCUMULATOR to Case(enc = { enc() }, gimp = { Accumulator }),
  IMMEDIATE to Case(enc = { enc(it) }, gimp = { Immediate(it.u8()) }),
  RELATIVE to Case(enc = { enc(it) }, gimp = { Relative(it.s8())} ),
  ZERO_PAGE to Case(
    enc = { enc(0x30) },
    gimp = { ZeroPage(0x30u) },
    operandAddr = 0x0030
  ),
  ZERO_PAGE_X to Case(
    enc = { enc(0x30) },
    gimp = { ZeroPageIndexed(0x30u, X) },
    state = { with(X = 0x20u) },
    operandAddr = 0x0050
  ),
  ZERO_PAGE_Y to Case(
    enc = { enc(0x30) },
    gimp = { ZeroPageIndexed(0x30u, Y) },
    state = { with(Y = 0x20u) },
    operandAddr = 0x0050
  ),
  ABSOLUTE to Case(
    enc = { enc16(SCARY_ADDR) },
    gimp = { Absolute(SCARY_ADDR.u16()) },
    operandAddr = SCARY_ADDR
  ),
  ABSOLUTE_X to Case(
    enc = { enc16(SCARY_ADDR - 0x20) },
    gimp = { AbsoluteIndexed((SCARY_ADDR - 0x20).u16(), X) },
    state = { with(X = 0x20u) },
    operandAddr = SCARY_ADDR
  ),
  ABSOLUTE_Y to Case(
    enc = { enc16(SCARY_ADDR - 0x20) },
    gimp = { AbsoluteIndexed((SCARY_ADDR - 0x20).u16(), Y) },
    state = { with(Y = 0x20u) },
    operandAddr = SCARY_ADDR
  ),
  INDIRECT to Case(
    enc = { enc16(BASE_RAM + 0x3050) },
    gimp = { Indirect((BASE_RAM + 0x3050).u16()) },
    mem = mem16(BASE_RAM + 0x3050, SCARY_ADDR)
  ),
  INDEXED_INDIRECT to Case(
    enc = { enc(0x30) },
    gimp = { IndexedIndirect(0x30u) },
    state = { with(X = 0x10u) },
    mem = mem16(0x0040, SCARY_ADDR),
    operandAddr = SCARY_ADDR
  ),
  INDIRECT_INDEXED to Case(
    enc = { enc(0x30) },
    gimp = { IndirectIndexed(0x30u) },
    state = { with(Y = 0x10u) },
    mem = mem16(0x0030, SCARY_ADDR - 0x10),
    operandAddr = SCARY_ADDR
  )
)

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
  encodings: Map<AddressMode, UInt8>,
  operand: Int = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Int, Int> = emptyMap(),
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State = { this }
) {
  encodings.forEach { (mode, enc) ->
    PROTO_STATES.forEach { proto ->
      val case = CASES[mode] ?: error("Unhandled mode ${mode}")
      val instruction = case.enc(enc, operand)

      assertCpuEffects(
        instructions = listOf(instruction),
        initState = case.state(proto).with(PC = BASE_USER.toPC()).initState(),
        initStores = case.mem +             // Indirection / pointer
          (case.operandAddr to operand) +   // Operand (user-defined value, case-defined location)
          initStores,                       // User-defined
        expectedState = case.state(proto).with(PC = BASE_USER.toPC() + instruction.size).expectedState(),
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
  // Set up memory so PC jumps from reset vector to trampoline to user instructions
  val trampoline = trampolineFor(initState)
  val memory = FakeMemory(
    mapOf(
      VECTOR_RESET.toInt() to BASE_TRAMPOLINE.lo().toInt(),
      (VECTOR_RESET + 1u).toInt() to BASE_TRAMPOLINE.hi().toInt()
    ) +
      trampoline.memoryMap(BASE_TRAMPOLINE) +
      instructions.memoryMap(BASE_USER) +
      initStores
  )
  val cpu = Cpu(memory)

  repeat(trampoline.size) { cpu.next() }
  memory.trackStores = true
  repeat(instructions.size) { cpu.next() }

  if (expectedState != null) {
    assertEquals(expectedState, cpu.state, "Unexpected state for [${name}]")
  }
  memory.assertStores(expectedStores, "Unexpected store for [${name}]")
}

/** Instructions that set CPU state and trampolines to the user code. */
private fun trampolineFor(state: State) = listOf(
  LDX[IMMEDIATE].enc(state.S),
  TXS[IMPLIED].enc(),
  LDA[IMMEDIATE].enc(state.P.u8()),
  PHA[IMPLIED].enc(),
  LDA[IMMEDIATE].enc(state.A),
  LDX[IMMEDIATE].enc(state.X),
  LDY[IMMEDIATE].enc(state.Y),
  PLP[IMPLIED].enc(),
  JMP[ABSOLUTE].enc16(BASE_USER)
)

fun List<List<UInt8>>.memoryMap(base: Int) = flatten()
  .withIndex()
  .associate { (it.index + base) to it.value.toInt() }

operator fun Opcode.get(mode: AddressMode = IMPLIED) = encodings[mode]!!

fun UInt8.enc() = listOf(this)
fun UInt8.enc(operand: Int) = listOf(this, operand.u8())
fun UInt8.enc(operand: UInt8) = listOf(this, operand)
fun UInt8.enc16(operand: Int) = listOf(this, operand.lo(), operand.hi())
