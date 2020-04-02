import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddrMode.*
import choliver.sixfiveohtwo.AddressMode.*
import org.junit.jupiter.api.Assertions

private data class Case(
  val encOperand: Array<UInt8>,
  val state: State = State(),
  val setup: (Memory) -> Unit
)

private val CASES = mapOf(
  IMMEDIATE to Case(enc(0x69)) {},
  ZERO_PAGE to Case(enc(0x30)) {
    it.store(0x0030u, 0x69u)
  },
  ZERO_PAGE_X to Case(enc(0x30), State(X = 0x20u)) {
    it.store(0x0050u, 0x69u)
  },
  ZERO_PAGE_Y to Case(enc(0x30), State(Y = 0x20u)) {
    it.store(0x0050u, 0x69u)
  },
  ABSOLUTE to Case(enc(0x30, 0x12)) {
    it.store(0x1230u, 0x69u)
  },
  ABSOLUTE_X to Case(enc(0x30, 0x12), State(X = 0x20u)) {
    it.store(0x1250u, 0x69u)
  },
  ABSOLUTE_Y to Case(enc(0x30, 0x12), State(Y = 0x20u)) {
    it.store(0x1250u, 0x69u)
  },
  INDEXED_INDIRECT to Case(enc(0x30), State(X = 0x10u)) {
    it.store(0x1230u, 0x69u)
    it.store(0x0040u, 0x30u)
    it.store(0x0041u, 0x12u)
  },
  INDIRECT_INDEXED to Case(enc(0x30), State(Y = 0x10u)) {
    it.store(0x1230u, 0x69u)
    it.store(0x0030u, 0x20u)
    it.store(0x0031u, 0x12u)
  }
)

fun assertForAddressModes(vararg ops: Pair<AddrMode, Int>, expected: State.() -> State) {
  ops.forEach { (mode, enc) ->
    val memory = FakeMemory()
    val cpu = Cpu(memory)

    val case = CASES[mode]!!

    case.setup(memory)

    Assertions.assertEquals(
      case.state.expected(),
      cpu.execute(enc(enc) + case.encOperand, case.state)
    )
  }
}


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
        Assertions.assertEquals(expected, cpu.execute(encoding, original))
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
        Assertions.assertEquals(emptyList<Pair<UInt16, UInt8>>(), memory.stores)
      }
    }

    context.block()
  }
}

fun enc(vararg bytes: Int) = bytes.map { it.u8() }.toTypedArray()
