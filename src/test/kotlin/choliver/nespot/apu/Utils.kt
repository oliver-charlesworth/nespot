package choliver.nespot.apu

internal fun Synth.take(num: Int) = List(num) { output.also { onTimer(1) } }

internal fun <T> List<T>.repeat(num: Int) = (0 until num).flatMap { this }

internal fun <T> List<T>.repeatEach(num: Int) = flatMap { v -> List(num) { v } }

internal fun Synth.nextNonZeroOutput(): Int {
  while(output == 0) { onTimer(1) }
  return output
}
