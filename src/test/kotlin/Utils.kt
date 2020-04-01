import choliver.sixfiveohtwo.*
import choliver.sixfiveohtwo.AddressMode.*
import org.junit.jupiter.api.Assertions

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
