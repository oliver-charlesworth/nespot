import choliver.sixfiveohtwo.Alu
import choliver.sixfiveohtwo.Flags
import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

@Suppress("ObjectPropertyName")
const val _0 = false
@Suppress("ObjectPropertyName")
const val _1 = true

private val PROTO_STATES = listOf(
  State(),
  State(flags = Flags(C = true)),
  State(flags = Flags(Z = true)),
  State(flags = Flags(I = true)),
  State(flags = Flags(D = true)),
  State(flags = Flags(V = true)),
  State(flags = Flags(N = true)),
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
