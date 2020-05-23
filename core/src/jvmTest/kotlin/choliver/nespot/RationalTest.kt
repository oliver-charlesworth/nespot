package choliver.nespot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RationalTest {
  @Test
  fun simplify() {
    val r = Rational.of(10, 8)

    assertEquals(5, r.a)
    assertEquals(4, r.b)
  }

  @Test
  fun `negative values`() {
    val r = Rational.of(-10, 8)
    val s = Rational.of(10, -8)

    assertEquals(-5, r.a)
    assertEquals(4, r.b)
    assertEquals(-5, s.a)
    assertEquals(4, s.b)
  }

  @Test
  fun plus() {
    assertEquals(Rational.of(19, 12), Rational.of(5, 6) + Rational.of(3, 4))
    assertEquals(Rational.of(23, 6), Rational.of(5, 6) + 3)
  }

  @Test
  fun minus() {
    assertEquals(Rational.of(1, 12), Rational.of(5, 6) - Rational.of(3, 4))
    assertEquals(Rational.of(-13, 6), Rational.of(5, 6) - 3)
  }

  @Test
  fun multiply() {
    assertEquals(Rational.of(5, 8), Rational.of(5, 6) * Rational.of(3, 4))
    assertEquals(Rational.of(5, 2), Rational.of(5, 6) * 3)
  }

  @Test
  fun divide() {
    assertEquals(Rational.of(10, 9), Rational.of(5, 6) / Rational.of(3, 4))
    assertEquals(Rational.of(5, 18), Rational.of(5, 6) / 3)
  }

  @Test
  fun negate() {
    assertEquals(Rational.of(-3, 4), -Rational.of(3, 4))
  }

  @Test
  fun toInt() {
    assertEquals(2, Rational.of(12, 5).toInt())
    assertEquals(-2, Rational.of(-12, 5).toInt())
  }
}
