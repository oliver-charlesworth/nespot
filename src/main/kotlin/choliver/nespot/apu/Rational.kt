package choliver.nespot.apu

class Rational(a: Int, b: Int = 1) {
  val a: Int
  val b: Int

  init {
    val d = gcd(a, b)
    this.a = a / d
    this.b = b / d
  }

  operator fun plus(rhs: Rational) = Rational(a * rhs.b + b * rhs.a, b * rhs.b)
  operator fun minus(rhs: Rational) = Rational(a * rhs.b - b * rhs.a, b * rhs.b)
  operator fun div(rhs: Rational) = Rational(a * rhs.b, b * rhs.a)
  operator fun div(rhs: Int) = Rational(a, b * rhs)
  operator fun compareTo(rhs: Rational) = (a * rhs.b) - (b * rhs.a)
  operator fun compareTo(rhs: Int) = a - (b * rhs)

  companion object {
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

fun Int.toRational() = Rational(this, 1)
