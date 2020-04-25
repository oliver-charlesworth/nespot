package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import choliver.nespot.sixfiveohtwo.utils._0

internal fun Synth.take(num: Int, tick: Ticks = Ticks(_0, _0)) = List(num) { take(tick) }

internal fun <T> List<T>.repeat(num: Int) = (0 until num).flatMap { this }
