import choliver.sixfiveohtwo.Alu
import choliver.sixfiveohtwo.Flags
import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

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

interface ContextNoOperand {
  val s: State
  fun assertEquals(expected: State, original: State)
}

interface ContextOneOperand {
  val s: State
  fun assertEquals(expected: State, original: State, operand: UByte)
}

fun forOpcode(op: Alu.(State) -> State, block: ContextNoOperand.() -> Unit) {
  val alu = Alu()

  PROTO_STATES.forEach {
    val context = object : ContextNoOperand {
      override val s = it
      override fun assertEquals(expected: State, original: State) {
        Assertions.assertEquals(expected, alu.op(original))
      }
    }

    context.block()
  }
}

fun forOpcode(op: Alu.(State, UByte) -> State, block: ContextOneOperand.() -> Unit) {
  val alu = Alu()

  PROTO_STATES.forEach {
    val context = object : ContextOneOperand {
      override val s = it
      override fun assertEquals(expected: State, original: State, operand: UByte) {
        assertEquals(expected, alu.op(original, operand))
      }
    }

    context.block()
  }
}
