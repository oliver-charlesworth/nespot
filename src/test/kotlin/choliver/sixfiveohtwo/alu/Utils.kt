package choliver.sixfiveohtwo.alu

import choliver.sixfiveohtwo.State
import org.junit.jupiter.api.Assertions.assertEquals

internal fun assertEqualsID(expected: State, original: State, fn: (State) -> State) {
  (0 until 4).forEach {
    fun State.withFlags() = copy(
      I = (it and 0x01) != 0x00,
      D = (it and 0x02) != 0x00
    )

    assertEquals(expected.withFlags(), fn(original.withFlags()))
  }
}

internal fun assertEqualsIDCV(expected: State, original: State, fn: (State) -> State) {
  (0 until 16).forEach {
    fun State.withFlags() = copy(
      I = (it and 0x01) != 0x00,
      D = (it and 0x02) != 0x00,
      C = (it and 0x04) != 0x00,
      V = (it and 0x08) != 0x00
    )

    assertEquals(expected.withFlags(), fn(original.withFlags()))
  }
}

internal fun assertEqualsIDCVZN(expected: State, original: State, fn: (State) -> State) {
  (0 until 64).forEach {
    fun State.withFlags() = copy(
      C = (it and 0x01) != 0x00,
      I = (it and 0x02) != 0x00,
      D = (it and 0x04) != 0x00,
      V = (it and 0x08) != 0x00,
      Z = (it and 0x10) != 0x00,
      N = (it and 0x20) != 0x00
    )

    assertEquals(expected.withFlags(), fn(original.withFlags()))
  }
}
