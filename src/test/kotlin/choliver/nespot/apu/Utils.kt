package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks

internal fun Generator.take(num: Int, tick: Ticks = Ticks(0, 0)) = List(num) { take(tick) }

internal fun <T> Takeable<T>.take(num: Int) = List(num) { take() }

internal fun <T> List<T>.repeat(num: Int) = (0 until num).flatMap { this }
