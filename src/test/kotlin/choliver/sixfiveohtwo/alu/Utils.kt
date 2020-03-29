package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals

internal enum class Flag {
  C, Z, I, D, V, N
}

internal interface ContextNoOperand {
  fun assertEquals(expected: State, original: State)
}

internal interface ContextOneOperand {
  fun assertEquals(expected: State, original: State, operand: UByte)
}

internal fun forOpcode(op: Alu.(State) -> State, vararg invariants: Flag, block: ContextNoOperand.() -> Unit) {
  val alu = Alu()

  val context = object : ContextNoOperand {
    override fun assertEquals(expected: State, original: State) {
      assertEquals(invariants.asList(), expected, original) { alu.op(it) }
    }
  }

  context.block()
}

internal fun forOpcode(op: Alu.(State, UByte) -> State, vararg invariants: Flag, block: ContextOneOperand.() -> Unit) {
  val alu = Alu()

  val context = object : ContextOneOperand {
    override fun assertEquals(expected: State, original: State, operand: UByte) {
      assertEquals(invariants.asList(), expected, original) { alu.op(it, operand) }
    }
  }

  context.block()
}

private fun assertEquals(invariants: List<Flag>, expected: State, original: State, fn: (State) -> State) {
  if (invariants.isEmpty()) {
    assertEquals(expected, fn(original))
  } else {
    fun State.withFlag(b: Boolean) = when (invariants.last()) {
      Flag.C -> copy(C = b)
      Flag.Z -> copy(Z = b)
      Flag.I -> copy(I = b)
      Flag.D -> copy(D = b)
      Flag.V -> copy(V = b)
      Flag.N -> copy(N = b)
    }

    assertEquals(invariants.dropLast(1), expected.withFlag(false), original.withFlag(false), fn)
    assertEquals(invariants.dropLast(1), expected.withFlag(true), original.withFlag(true), fn)
  }
}
