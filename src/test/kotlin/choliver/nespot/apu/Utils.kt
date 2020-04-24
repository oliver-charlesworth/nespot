package choliver.nespot.apu

internal fun <T> Takeable<T>.take(num: Int) = List(num) { take() }

internal fun <T> List<T>.repeat(num: Int) = (0 until num).flatMap { this }
