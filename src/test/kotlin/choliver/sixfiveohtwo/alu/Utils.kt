package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals

private val PROTO_STATES = listOf(
  State(),
  State(C = true),
  State(Z = true),
  State(I = true),
  State(D = true),
  State(V = true),
  State(N = true),
  State(A = 0x69u),
  State(X = 0x69u),
  State(Y = 0x69u),
  State(S = 0x69u)
)

internal interface ContextNoOperand {
  val s: State
  fun assertEquals(expected: State, original: State)
}

internal interface ContextOneOperand {
  val s: State
  fun assertEquals(expected: State, original: State, operand: UByte)
}

internal fun forOpcode(op: Alu.(State) -> State, block: ContextNoOperand.() -> Unit) {
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

internal fun forOpcode(op: Alu.(State, UByte) -> State, block: ContextOneOperand.() -> Unit) {
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
