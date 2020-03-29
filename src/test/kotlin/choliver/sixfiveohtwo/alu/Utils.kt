package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals

internal enum class Flag {
  C, Z, I, D, V, N
}

internal interface WithInvariant {
  val alu: Alu

  fun assertEquals(expected: State, original: State, fn: (State) -> State)
}

internal fun withInvariants(vararg invariants: Flag, block: WithInvariant.() -> Unit) {
  val scope = object : WithInvariant {
    override val alu = Alu()

    override fun assertEquals(expected: State, original: State, fn: (State) -> State) {
      assertEquals(invariants.asList(), expected, original, fn)
    }
  }

  scope.block()
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
