import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.AddressMode.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*

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
    enc = { enc(0x30, 0x12) },
    operandAddr = 0x1230
  ),
  ABSOLUTE_X to Case(
    enc = { enc(0x30, 0x12) },
    state = { with(X = 0x20u) },
    operandAddr = 0x1250
  ),
  ABSOLUTE_Y to Case(
    enc = { enc(0x30, 0x12) },
    state = { with(Y = 0x20u) },
    operandAddr = 0x1250
  ),
  INDEXED_INDIRECT to Case(
    enc = { enc(0x30) },
    state = { with(X = 0x10u) },
    mem = mapOf(0x0040 to 0x30, 0x0041 to 0x12),
    operandAddr = 0x1230
  ),
  INDIRECT_INDEXED to Case(
    enc = { enc(0x30) },
    state = { with(Y = 0x10u) },
    mem = mapOf(0x0030 to 0x20, 0x0031 to 0x12),
    operandAddr = 0x1230
  )
)

fun assertForAddressModes(
  ops: Map<AddrMode, Int>,
  operand: Int = 0x00,
  originalState: State.() -> State = { this },
  expectedStores: (operandAddr: Int) -> Map<Int, Int> = { emptyMap() },
  expectedState: State.() -> State
) {
  ops.forEach { (mode, enc) ->
    PROTO_STATES.forEach { proto ->
      val case = CASES[mode]!!

      val memory = FakeMemory(case.mem + (case.operandAddr to operand))
      val cpu = Cpu(memory)

      assertEquals(
        case.state(proto).expectedState(),
        cpu.execute(enc(enc) + case.enc(operand), case.state(proto).originalState()),
        "Unexpected state for [${mode.name}]"
      )

      assertEquals(
        expectedStores(case.operandAddr).entries.associate { (k, v) -> k.u16() to v.u8() },
        memory.stores,
        "Unexpected store for [${mode.name}]"
      )
    }
  }
}



interface OpcodeContext {
  val s: State
  fun assertEquals(expected: State, original: State, addressMode: AddressMode = Implied)
}

interface SweepStatesContext {
  val s: State
  fun assertEquals(expected: State, original: State, encoding: Array<UInt8>)
}

fun sweepStates(block: SweepStatesContext.() -> Unit) {
  val memory = FakeMemory()
  val cpu = Cpu(memory)

  PROTO_STATES.forEach {
    val context = object : SweepStatesContext {
      override val s = it
      override fun assertEquals(expected: State, original: State, encoding: Array<UInt8>) {
        assertEquals(expected, cpu.execute(encoding, original))
      }
    }

    context.block()
  }
}

fun forOpcode(op: Opcode, block: OpcodeContext.() -> Unit) {
  val memory = FakeMemory()
  val cpu = Cpu(memory)

  PROTO_STATES.forEach {
    val context = object : OpcodeContext {
      override val s = it
      override fun assertEquals(expected: State, original: State, addressMode: AddressMode) {
        Assertions.assertEquals(expected, cpu.execute(Instruction(op, addressMode), original))
      }
    }

    context.block()
  }
}

fun enc(vararg bytes: Int) = bytes.map { it.u8() }.toTypedArray()
