package choliver.nespot.cpu

import choliver.nespot.*
import choliver.nespot.cpu.Cpu.Companion.INTERRUPT_IRQ
import choliver.nespot.cpu.Cpu.Companion.INTERRUPT_NMI
import choliver.nespot.cpu.Cpu.Companion.INTERRUPT_RESET
import choliver.nespot.cpu.model.*
import choliver.nespot.cpu.model.AddressMode.*
import choliver.nespot.cpu.model.Operand.*
import choliver.nespot.cpu.model.Operand.IndexSource.X
import choliver.nespot.cpu.model.Operand.IndexSource.Y
import choliver.nespot.cpu.utils._0
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals

private data class Case(
  val operand: (value: Data) -> Operand,
  val regs: Regs.() -> Regs = { this },
  val mem: List<Pair<Address, Data>> = emptyList(),
  val targetAddr: Address = 0x0000
)

private val PROTO_REGS = listOf(
  Regs(),
  Regs(p = Flags(c = true)),
  Regs(p = Flags(z = true)),
  Regs(p = Flags(i = true)),
  Regs(p = Flags(d = true)),
  Regs(p = Flags(v = true)),
  Regs(p = Flags(n = true)),
  Regs(a = 0xAA),
  Regs(x = 0xAA),
  Regs(y = 0xAA),
  Regs(s = 0xAA)
)

const val BASE_ZERO_PAGE: Address = 0x0000
const val BASE_STACK: Address = 0x0100
const val BASE_RAM: Address = 0x0200
const val BASE_ROM: Address = 0x8000

const val BASE_USER: Address = BASE_ROM + 0x1230
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
    regs = { with(x = 0x20) },
    targetAddr = 0x0050
  ),
  ZERO_PAGE_Y to Case(
    operand = { ZeroPageIndexed(0x30, Y) },
    regs = { with(y = 0x20) },
    targetAddr = 0x0050
  ),
  ABSOLUTE to Case(
    operand = { Absolute(SCARY_ADDR) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_X to Case(
    operand = { AbsoluteIndexed(SCARY_ADDR - 0x20, X) },
    regs = { with(x = 0x20) },
    targetAddr = SCARY_ADDR
  ),
  ABSOLUTE_Y to Case(
    operand = { AbsoluteIndexed(SCARY_ADDR - 0x20, Y) },
    regs = { with(y = 0x20) },
    targetAddr = SCARY_ADDR
  ),
  INDIRECT to Case(
    operand = { Indirect(BASE_RAM + 0x3050) },
    mem = addrToMem(BASE_RAM + 0x3050, SCARY_ADDR)
  ),
  INDEXED_INDIRECT to Case(
    operand = { IndexedIndirect(0x30) },
    regs = { with(x = 0x10) },
    mem = addrToMem(0x0040, SCARY_ADDR),
    targetAddr = SCARY_ADDR
  ),
  INDIRECT_INDEXED to Case(
    operand = { IndirectIndexed(0x30) },
    regs = { with(y = 0x10) },
    mem = addrToMem(0x0030, SCARY_ADDR - 0x10),
    targetAddr = SCARY_ADDR
  )
)

fun assertForAddressModes(
  op: Opcode,
  modes: Set<AddressMode> = OPCODES_TO_ENCODINGS[op]?.keys ?: error("Unhandled opcode ${op.name}"),
  target: Data = 0x00,
  initRegs: Regs.() -> Regs = { this },
  initStores: List<Pair<Address, Data>> = emptyList(),
  expectedStores: (operandAddr: Address) -> List<Pair<Address, Data>> = { emptyList() },
  expectedRegs: Regs.() -> Regs = { this }
) {
  modes.forEach { mode ->
    PROTO_REGS.forEach { proto ->
      val case = CASES[mode] ?: error("Unhandled mode ${mode}")
      val instruction = Instruction(op, case.operand(target))

      assertCpuEffects(
        instructions = listOf(instruction),
        initRegs = case.regs(proto).initRegs(),
        initStores = case.mem +             // Indirection / pointer
          (case.targetAddr to target) +     // Target (user-defined value, case-defined location)
          initStores,                       // User-defined
        expectedRegs = case.regs(proto).with(pc = BASE_USER + instruction.encode().size).expectedRegs(),
        expectedStores = expectedStores(case.targetAddr),
        name = instruction.toString()
      )
    }
  }
}

fun assertCpuEffects(
  instructions: List<Instruction>,
  numStepsToExecute: Int = instructions.size,
  initRegs: Regs,
  initStores: List<Pair<Address, Data>> = emptyList(),
  expectedRegs: Regs? = null,
  expectedStores: List<Pair<Address, Data>>? = emptyList(), // null == no check
  expectedCycles: Int? = null,
  pollReset: (iStep: Int) -> Boolean = { _0 },
  pollNmi: (iStep: Int) -> Boolean = { _0 },
  pollIrq: (iStep: Int) -> Boolean = { _0 },
  name: String = "???"
) {
  var numCycles = 0
  var iStep = 0

  val memory = mockMemory((instructions.memoryMap(BASE_USER) + initStores).toMap())

  val cpu = Cpu(
    memory,
    pollInterrupts = {
      0 +
        (if (pollReset(iStep)) INTERRUPT_RESET else 0) or
        (if (pollNmi(iStep)) INTERRUPT_NMI else 0) or
        (if (pollIrq(iStep)) INTERRUPT_IRQ else 0)
    }
  )

  cpu.diagnostics.state.regs = initRegs.with(pc = BASE_USER)

  repeat(numStepsToExecute) {
    numCycles += cpu.executeStep()
    iStep++
  }

  if (expectedRegs != null) {
    assertEquals(expectedRegs, cpu.diagnostics.state.regs, "Unexpected registers for [${name}]")
  }

  if (expectedStores != null) {
    verifyStores(memory, expectedStores)
  }

  if (expectedCycles != null) {
    assertEquals(expectedCycles, numCycles, "Unexpected # cycles for [${name}]")
  }
}

private fun verifyStores(memory: Memory, expectedStores: List<Pair<Address, Data>>) {
  verify(memory, times(expectedStores.size))[any()] = any()
  inOrder(memory) {
    expectedStores.forEach { (addr, data) ->
      verify(memory, calls(1))[addr] = data
    }
  }
}

fun List<Instruction>.memoryMap(base: Address) = map { it.encode() }
  .flatten()
  .withIndex()
  .map { (base + it.index).addr() to it.value }

fun mockMemory(init: Map<Address, Data>) = mock<Memory> {
  on { get(any()) } doAnswer { init[it.getArgument(0)] ?: 0xCC } // Easier to spot during debugging than 0x00
}

fun addrToMem(addr: Address, data: Int) = listOf(addr to data.lo(), (addr + 1) to data.hi())

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
