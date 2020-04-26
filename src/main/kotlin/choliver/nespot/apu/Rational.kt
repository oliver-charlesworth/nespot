package choliver.nespot.apu

import kotlin.math.absoluteValue

class Rational(a: Int, b: Int) {
  val a: Int
  val b: Int

  init {
    // Ensure denominator is always positive
    val d = gcd(a.absoluteValue, b.absoluteValue)
    this.a = (if (b < 0) -a else a) / d
    this.b = b.absoluteValue / d
  }

  operator fun plus(rhs: Rational) = Rational(a * rhs.b + b * rhs.a, b * rhs.b)
  operator fun plus(rhs: Int) = Rational(a + b * rhs, b)
  operator fun minus(rhs: Rational) = Rational(a * rhs.b - b * rhs.a, b * rhs.b)
  operator fun minus(rhs: Int) = Rational(a - b * rhs, b)
  operator fun times(rhs: Rational) = Rational(a * rhs.a, b * rhs.b)
  operator fun times(rhs: Int) = Rational(a * rhs, b)
  operator fun div(rhs: Rational) = Rational(a * rhs.b, b * rhs.a)
  operator fun div(rhs: Int) = Rational(a, b * rhs)
  operator fun compareTo(rhs: Rational) = (a * rhs.b) - (b * rhs.a)
  operator fun compareTo(rhs: Int) = a - (b * rhs)
  operator fun unaryMinus() = Rational(-a, b)
  /** Rounds towards zero. */
  fun toInt() = a / b

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Rational
    return (a == other.a) && (b == other.b)
  }

  override fun hashCode() = (31 * a) + b

  override fun toString() = "($a, $b)"

  companion object {
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

internal fun Int.toRational() = Rational(this, 1)
