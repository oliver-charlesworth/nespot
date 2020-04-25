package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RationalTest {
  @Test
  fun simplify() {
    val r = Rational(10, 8)

    assertEquals(5, r.a)
    assertEquals(4, r.b)
  }

  @Test
  fun `negative values`() {
    val r = Rational(-10, 8)
    val s = Rational(10, -8)

    assertEquals(-5, r.a)
    assertEquals(4, r.b)
    assertEquals(-5, s.a)
    assertEquals(4, s.b)
  }

  @Test
  fun plus() {
    assertEquals(Rational(19, 12), Rational(5, 6) + Rational(3, 4))
    assertEquals(Rational(23, 6), Rational(5, 6) + 3)
  }

  @Test
  fun minus() {
    assertEquals(Rational(1, 12), Rational(5, 6) - Rational(3, 4))
    assertEquals(Rational(-13, 6), Rational(5, 6) - 3)
  }

  @Test
  fun multiply() {
    assertEquals(Rational(5, 8), Rational(5, 6) * Rational(3, 4))
    assertEquals(Rational(5, 2), Rational(5, 6) * 3)
  }

  @Test
  fun divide() {
    assertEquals(Rational(10, 9), Rational(5, 6) / Rational(3, 4))
    assertEquals(Rational(5, 18), Rational(5, 6) / 3)
  }

  @Test
  fun negate() {
    assertEquals(Rational(-3, 4), -Rational(3, 4))
  }

  @Test
  fun toInt() {
    assertEquals(2, Rational(12, 5).toInt())
    assertEquals(-2, Rational(-12, 5).toInt())
  }
}
