package choliver.nespot.common

import kotlin.math.absoluteValue

data class Rational private constructor(val a: Int, val b: Int) {
  operator fun plus(rhs: Rational) = of(a * rhs.b + b * rhs.a, b * rhs.b)
  operator fun plus(rhs: Int) = of(a + b * rhs, b)
  operator fun minus(rhs: Rational) = of(a * rhs.b - b * rhs.a, b * rhs.b)
  operator fun minus(rhs: Int) = of(a - b * rhs, b)
  operator fun times(rhs: Rational) = of(a * rhs.a, b * rhs.b)
  operator fun times(rhs: Int) = of(a * rhs, b)
  operator fun div(rhs: Rational) = of(a * rhs.b, b * rhs.a)
  operator fun div(rhs: Int) = of(a, b * rhs)
  operator fun compareTo(rhs: Rational) = (a * rhs.b) - (b * rhs.a)
  operator fun compareTo(rhs: Int) = a - (b * rhs)
  operator fun unaryMinus() = of(-a, b)
  /** Rounds towards zero. */
  fun toInt() = (a / b)
  fun toDouble() = (a.toDouble() / b.toDouble())

  override fun toString() = "($a, $b)"

  companion object {
    fun of(a: Int, b: Int = 1): Rational {
      // Ensure denominator is always positive
      val d = gcd(a.absoluteValue, b.absoluteValue)
      return Rational(
        (if (b < 0) -a else a) / d,
        b.absoluteValue / d
      )
    }

    private fun gcd(a: Int, b: Int): Int {
      var x = a
      var y = b
      while (y != 0) {
        val t = y
        y = x % y
        x = t
      }
      return x
    }
  }
}
