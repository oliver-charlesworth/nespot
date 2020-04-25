package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RationalTest {
  @Test
  fun simplify() {
    val r = rational(10, 8)

    assertEquals(5, r.a)
    assertEquals(4, r.b)
  }

  @Test
  fun `negative values`() {
    val r = rational(-10, 8)
    val s = rational(10, -8)

    assertEquals(-5, r.a)
    assertEquals(4, r.b)
    assertEquals(-5, s.a)
    assertEquals(4, s.b)
  }

  @Test
  fun plus() {
    assertEquals(rational(19, 12), rational(5, 6) + rational(3, 4))
    assertEquals(rational(23, 6), rational(5, 6) + 3)
  }

  @Test
  fun minus() {
    assertEquals(rational(1, 12), rational(5, 6) - rational(3, 4))
    assertEquals(rational(-13, 6), rational(5, 6) - 3)
  }

  @Test
  fun multiply() {
    assertEquals(rational(5, 8), rational(5, 6) * rational(3, 4))
    assertEquals(rational(5, 2), rational(5, 6) * 3)
  }

  @Test
  fun divide() {
    assertEquals(rational(10, 9), rational(5, 6) / rational(3, 4))
    assertEquals(rational(5, 18), rational(5, 6) / 3)
  }

  @Test
  fun negate() {
    assertEquals(rational(-3, 4), -rational(3, 4))
  }

  @Test
  fun toInt() {
    assertEquals(2, rational(12, 5).toInt())
    assertEquals(-2, rational(-12, 5).toInt())
  }
}
