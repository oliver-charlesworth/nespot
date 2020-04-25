package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks

internal fun Synth.take(num: Int, tick: Ticks = Ticks(0, 0)) = List(num) { take(tick) }

internal fun <T> List<T>.repeat(num: Int) = (0 until num).flatMap { this }
