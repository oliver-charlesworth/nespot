package choliver.nespot.apu

import choliver.nespot.apu.Rational.Companion.rational
import kotlin.math.absoluteValue

data class Rational private constructor(val a: Int, val b: Int) {
  operator fun plus(rhs: Rational) = rational(a * rhs.b + b * rhs.a, b * rhs.b)
  operator fun plus(rhs: Int) = rational(a + b * rhs, b)
  operator fun minus(rhs: Rational) = rational(a * rhs.b - b * rhs.a, b * rhs.b)
  operator fun minus(rhs: Int) = rational(a - b * rhs, b)
  operator fun times(rhs: Rational) = rational(a * rhs.a, b * rhs.b)
  operator fun times(rhs: Int) = rational(a * rhs, b)
  operator fun div(rhs: Rational) = rational(a * rhs.b, b * rhs.a)
  operator fun div(rhs: Int) = rational(a, b * rhs)
  operator fun compareTo(rhs: Rational) = (a * rhs.b) - (b * rhs.a)
  operator fun compareTo(rhs: Int) = a - (b * rhs)
  operator fun unaryMinus() = rational(-a, b)
  /** Rounds towards zero. */
  fun toInt() = a / b

  companion object {
    fun rational(a: Int, b: Int = 1): Rational {
      // Ensure denominator is always positive
      val d = gcd(a.absoluteValue, b.absoluteValue)
      return Rational(
        (if (b < 0) -a else a) / d,
        b.absoluteValue / d
      )
    }

    private fun gcd(a: Int, b: Int): Int {
      var a = a
      var b = b
      while (b != 0) {
        val t = b
        b = a % b
        a = t
      }
      return a
    }
  }
}

fun Int.toRational() = rational(this, 1)
