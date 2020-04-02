import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.AddressMode.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*

private data class Case(
  val enc: (operand: Int) -> Array<UInt8>,
  val state: State.() -> State = { this },
  val mem: (Memory, UInt8) -> Unit = { _: Memory, _: UInt8 -> }
)

private val PROTO_STATES = listOf(
  State(),
  State(P = Flags(C = true)),
  State(P = Flags(Z = true)),
  State(P = Flags(I = true)),
  State(P = Flags(D = true)),
  State(P = Flags(V = true)),
  State(P = Flags(N = true)),
  State(A = 0x69u),
  State(X = 0x69u),
  State(Y = 0x69u),
  State(S = 0x69u)
)

private val CASES = mapOf(
  IMMEDIATE to Case(
    enc = { enc(it) }
  ),
  ZERO_PAGE to Case(
    enc = { enc(0x30) },
    mem = { m, operand -> m.store(0x0030u, operand) }
  ),
  ZERO_PAGE_X to Case(
    enc = { enc(0x30) },
    state = { with(X = 0x20u) },
    mem = { m, operand -> m.store(0x0050u, operand) }
  ),
  ZERO_PAGE_Y to Case(
    enc = { enc(0x30) },
    state = { with(Y = 0x20u) },
    mem = { m, operand -> m.store(0x0050u, operand) }
  ),
  ABSOLUTE to Case(
    enc = { enc(0x30, 0x12) },
    mem = { m, operand -> m.store(0x1230u, operand) }
  ),
  ABSOLUTE_X to Case(
    enc = { enc(0x30, 0x12) },
    state = { with(X = 0x20u) },
    mem = { m, operand -> m.store(0x1250u, operand) }
  ),
  ABSOLUTE_Y to Case(
    enc = { enc(0x30, 0x12) },
    state = { with(Y = 0x20u) },
    mem = { m, operand -> m.store(0x1250u, operand) }
  ),
  INDEXED_INDIRECT to Case(
    enc = { enc(0x30) },
    state = { with(X = 0x10u) },
    mem = { m, operand ->
      m.store(0x1230u, operand)
      m.store(0x0040u, 0x30u)
      m.store(0x0041u, 0x12u)
    }
  ),
  INDIRECT_INDEXED to Case(
    enc = { enc(0x30) },
    state = { with(Y = 0x10u) },
    mem = { m, operand ->
      m.store(0x1230u, operand)
      m.store(0x0030u, 0x20u)
      m.store(0x0031u, 0x12u)
    }
  )
)

fun assertForAddressModes(ops: Map<AddrMode, Int>, operand: Int, expected: State.() -> State) {
  ops.forEach { (mode, enc) ->
    val memory = FakeMemory()
    val cpu = Cpu(memory)

    val case = CASES[mode]!!

    case.mem(memory, operand.u8())

    val protoState = case.state(State())

    assertEquals(
      protoState.expected(),
      cpu.execute(enc(enc) + case.enc(operand), protoState),
      "[${mode.name}]"
    )
  }

  val (mode, enc) = ops.entries.first() // TODO - is this deterministic?
  val case = CASES[mode]!!

  PROTO_STATES.forEach {
    val memory = FakeMemory()
    val cpu = Cpu(memory)

    case.mem(memory, operand.u8())

    val protoState = case.state(it)

    assertEquals(
      protoState.expected(),
      cpu.execute(enc(enc) + case.enc(operand), protoState),
      "[${mode.name}]"
    )
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
//        Assertions.assertEquals(emptyList<Pair<UInt16, UInt8>>(), memory.stores)
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
        assertEquals(emptyList<Pair<UInt16, UInt8>>(), memory.stores)
      }
    }

    context.block()
  }
}

fun enc(vararg bytes: Int) = bytes.map { it.u8() }.toTypedArray()
