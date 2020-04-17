package choliver.nes.sixfiveohtwo

import choliver.nes.*
import choliver.nes.sixfiveohtwo.Cpu.Companion.VECTOR_RESET
import choliver.nes.sixfiveohtwo.model.*
import choliver.nes.sixfiveohtwo.model.AddressMode.*
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals

private data class Case(
  val operand: (value: Data) -> Operand,
  val state: State.() -> State = { this },
  val mem: Map<Address, Data> = emptyMap(),
  val targetAddr: Address = 0x0000
)

private val PROTO_STATES = listOf(
  State(),
  State(P = Flags(C = true)),
  State(P = Flags(Z = true)),
  State(P = Flags(I = true)),
  State(P = Flags(D = true)),
  State(P = Flags(V = true)),
  State(P = Flags(N = true)),
  State(A = 0xAA),
  State(X = 0xAA),
  State(Y = 0xAA),
  State(S = 0xAA)
)

const val BASE_ZERO_PAGE: Address = 0x0000
const val BASE_STACK: Address = 0x0100
const val BASE_RAM: Address = 0x0200
const val BASE_ROM: Address = 0x8000

const val BASE_TRAMPOLINE: Address = BASE_ROM
const val BASE_USER: Address = BASE_ROM + 0x1000
/** Chosen to straddle a page boundary. */
const val SCARY_ADDR: Address = BASE_RAM + 0x10FF

private val CASES = mapOf(
  IMPLIED to Case(operand = { Implied }),
  ACCUMULATOR to Case(operand = { Accumulator }),
  IMMEDIATE to Case(operand = { Immediate(it) }),
  RELATIVE to Case(operand = { Relative(it)} ),
  ZERO_PAGE to Case(
    operand = { ZeroPage(0x30) },
    targetAddr = 0x0030
  ),
  ZERO_PAGE_X to Case(
    operand = { ZeroPageIndexed(0x30, X) },
    state = { with(X = 0x20) },
    targetAddr = 0x0050
  ),
  ZERO_PAGE_Y to Case(
    operand = { ZeroPageIndexed(0x30, Y) },
    state = { with(Y = 0x20) },
    targetAddr = 0x0050
  ),
  ABSOLUTE to Case(
    operand = { Absolute(SCARY_ADDR) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_X to Case(
    operand = { AbsoluteIndexed(SCARY_ADDR - 0x20, X) },
    state = { with(X = 0x20) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_Y to Case(
    operand = { AbsoluteIndexed(SCARY_ADDR - 0x20, Y) },
    state = { with(Y = 0x20) },
    targetAddr = SCARY_ADDR
  ),
  INDIRECT to Case(
    operand = { Indirect(BASE_RAM + 0x3050) },
    mem = addrToMem(BASE_RAM + 0x3050, SCARY_ADDR)
  ),
  INDEXED_INDIRECT to Case(
    operand = { IndexedIndirect(0x30) },
    state = { with(X = 0x10) },
    mem = addrToMem(0x0040, SCARY_ADDR),
    targetAddr = SCARY_ADDR
  ),
  INDIRECT_INDEXED to Case(
    operand = { IndirectIndexed(0x30) },
    state = { with(Y = 0x10) },
    mem = addrToMem(0x0030, SCARY_ADDR - 0x10),
    targetAddr = SCARY_ADDR
  )
)

fun assertForAddressModes(
  op: Opcode,
  modes: Set<AddressMode> = OPCODES_TO_ENCODINGS[op]?.keys ?: error("Unhandled opcode ${op.name}"),
  target: Data = 0x00,
  initState: State.() -> State = { this },
  initStores: Map<Address, Data> = emptyMap(),
  expectedStores: (operandAddr: Address) -> Map<Address, Data> = { emptyMap() },
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
        name = instruction.toString()
      )
    }
  }
}

fun assertCpuEffects(
  instructions: List<Instruction>,
  initState: State,
  initStores: Map<Address, Data> = emptyMap(),
  expectedState: State? = null,
  expectedStores: Map<Address, Data> = emptyMap(),
  name: String = ""
) {
  // Set up memory so PC jumps from reset vector to trampoline to user instructions
  val trampoline = trampolineFor(initState)

  val memory = mock<Memory> {
    val memoryMap = addrToMem(VECTOR_RESET, BASE_TRAMPOLINE) +
      trampoline.memoryMap(BASE_TRAMPOLINE) +
      instructions.memoryMap(BASE_USER) +
      mapOf((initState.S + BASE_STACK) to initState.P.data()) +
      initStores

    on { load(any()) } doAnswer { memoryMap[it.getArgument(0)] ?: 0xCC } // Easier to spot during debugging than 0x00
  }

  val cpu = Cpu(memory, pollReset = ImmediateOneShot()::poll)
  cpu.runSteps(1 + trampoline.size + instructions.size)  // 1 for reset

  if (expectedState != null) {
    assertEquals(expectedState, cpu.state, "Unexpected state for [${name}]")
  }
  expectedStores.forEach { (addr, data) -> verify(memory).store(addr, data) }
  verify(memory, times(expectedStores.size)).store(any(), any())
}

private fun Instruction.encode(): List<Data> {
  fun opEnc(mode: AddressMode) =
    OPCODES_TO_ENCODINGS[opcode]!![mode]?.encoding ?: error("Unsupported mode ${mode}")
  return when (val o = operand) {
    is Accumulator -> listOf(opEnc(ACCUMULATOR))
    is Implied -> listOf(opEnc(IMPLIED))
    is Immediate -> listOf(opEnc(IMMEDIATE), o.literal)
    is Relative -> listOf(opEnc(RELATIVE), o.offset)
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

/** Instructions that sets A/X/Y/S, loads P from stack, and trampolines to the user code. */
private fun trampolineFor(state: State) = listOf(
  Instruction(LDX, Immediate((state.S - 1).addr8())),
  Instruction(TXS),
  Instruction(LDA, Immediate(state.A)),
  Instruction(LDX, Immediate(state.X)),
  Instruction(LDY, Immediate(state.Y)),
  Instruction(PLP),
  Instruction(JMP, Absolute(BASE_USER))
)

private fun List<Instruction>.memoryMap(base: Address) = map { it.encode() }
  .flatten()
  .withIndex()
  .associate { (base + it.index).addr() to it.value }

private class ImmediateOneShot {
  private var b = true
  fun poll() = b.also { b = false }
}

fun addrToMem(addr: Address, data: Int) = mapOf(addr to data.lo(), (addr + 1) to data.hi())
