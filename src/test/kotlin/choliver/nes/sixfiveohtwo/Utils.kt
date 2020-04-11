package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nes.sixfiveohtwo.model.*
import choliver.nes.sixfiveohtwo.model.AddressMode.*
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import org.junit.jupiter.api.Assertions.assertEquals

private data class Case(
  val operand: (value: Int) -> Operand,
  val state: State.() -> State = { this },
  val mem: Map<Int, Int> = emptyMap(),
  val targetAddr: Int = 0x0000
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
  IMPLIED to Case(operand = { Implied }),
  ACCUMULATOR to Case(operand = { Accumulator }),
  IMMEDIATE to Case(operand = { Immediate(it.u8()) }),
  RELATIVE to Case(operand = { Relative(it.s8())} ),
  ZERO_PAGE to Case(
    operand = { ZeroPage(0x30u) },
    targetAddr = 0x0030
  ),
  ZERO_PAGE_X to Case(
    operand = { ZeroPageIndexed(0x30u, X) },
    state = { with(X = 0x20u) },
    targetAddr = 0x0050
  ),
  ZERO_PAGE_Y to Case(
    operand = { ZeroPageIndexed(0x30u, Y) },
    state = { with(Y = 0x20u) },
    targetAddr = 0x0050
  ),
  ABSOLUTE to Case(
    operand = { Absolute(SCARY_ADDR.u16()) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_X to Case(
    operand = { AbsoluteIndexed((SCARY_ADDR - 0x20).u16(), X) },
    state = { with(X = 0x20u) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_Y to Case(
    operand = { AbsoluteIndexed((SCARY_ADDR - 0x20).u16(), Y) },
    state = { with(Y = 0x20u) },
    targetAddr = SCARY_ADDR
  ),
  INDIRECT to Case(
    operand = { Indirect((BASE_RAM + 0x3050).u16()) },
    mem = mem16(BASE_RAM + 0x3050, SCARY_ADDR)
  ),
  INDEXED_INDIRECT to Case(
    operand = { IndexedIndirect(0x30u) },
    state = { with(X = 0x10u) },
    mem = mem16(0x0040, SCARY_ADDR),
    targetAddr = SCARY_ADDR
  ),
  INDIRECT_INDEXED to Case(
    operand = { IndirectIndexed(0x30u) },
    state = { with(Y = 0x10u) },
    mem = mem16(0x0030, SCARY_ADDR - 0x10),
    targetAddr = SCARY_ADDR
  )
)

fun mem16(addr: Int, data: Int) = mapOf(addr to data.loI(), (addr + 1) to data.hiI())

fun assertForAddressModes(
  op: Opcode,
  modes: Set<AddressMode> = OPCODES_TO_ENCODINGS[op]?.keys ?: error("Unhandled opcode ${op.name}"),
  target: Int = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Int, Int> = emptyMap(),
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State = { this }
) {
  modes.forEach { mode ->
    PROTO_STATES.forEach { proto ->
      val case = CASES[mode] ?: error("Unhandled mode ${mode}")
      val instruction = Instruction(op, case.operand(target))

      assertCpuEffects(
        instructions = listOf(instruction),
        initState = case.state(proto).with(PC = BASE_USER.toPC()).initState(),
        initStores = case.mem +             // Indirection / pointer
          (case.targetAddr to target) +     // Target (user-defined value, case-defined location)
          initStores,                       // User-defined
        expectedState = case.state(proto).with(PC = BASE_USER.toPC() + instruction.encode().size).expectedState(),
        expectedStores = expectedStores(case.targetAddr),
        name = instruction.format()
      )
    }
  }
}

fun assertCpuEffects(
  instructions: List<Instruction>,
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

private fun Instruction.encode(): List<UInt8> {
  fun opEnc(mode: AddressMode) = OPCODES_TO_ENCODINGS[opcode]!![mode] ?: error("Unsupported mode ${mode}")
  return when (val o = operand) {
    is Accumulator -> listOf(opEnc(ACCUMULATOR))
    is Implied -> listOf(opEnc(IMPLIED))
    is Immediate -> listOf(opEnc(IMMEDIATE), o.literal)
    is Relative -> listOf(opEnc(RELATIVE), o.addr.u8())
    is Absolute -> listOf(opEnc(ABSOLUTE), o.addr.lo(), o.addr.hi())
    is AbsoluteIndexed -> when (o.source) {
      X -> listOf(opEnc(ABSOLUTE_X), o.addr.lo(), o.addr.hi())
      Y -> listOf(opEnc(ABSOLUTE_Y), o.addr.lo(), o.addr.hi())
    }
    is ZeroPage -> listOf(opEnc(ZERO_PAGE), o.addr)
    is ZeroPageIndexed -> when (o.source) {
      X -> listOf(opEnc(ZERO_PAGE_X), o.addr)
      Y -> listOf(opEnc(ZERO_PAGE_Y), o.addr)
    }
    is Indirect -> listOf(opEnc(INDIRECT), o.addr.lo(), o.addr.hi())
    is IndexedIndirect -> listOf(opEnc(INDEXED_INDIRECT), o.addr)
    is IndirectIndexed -> listOf(opEnc(INDIRECT_INDEXED), o.addr)
  }
}



/** Instructions that set CPU state and trampolines to the user code. */
private fun trampolineFor(state: State) = listOf(
  Instruction(LDX, Immediate(state.S)),
  Instruction(TXS),
  Instruction(LDA, Immediate(state.P.u8())),
  Instruction(PHA),
  Instruction(LDA, Immediate(state.A)),
  Instruction(LDX, Immediate(state.X)),
  Instruction(LDY, Immediate(state.Y)),
  Instruction(PLP),
  Instruction(JMP, Absolute(BASE_USER.u16()))
)

private fun List<Instruction>.memoryMap(base: Int) = map { it.encode() }
  .flatten()
  .withIndex()
  .associate { (it.index + base) to it.value.toInt() }

